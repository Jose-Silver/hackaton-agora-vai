package service;

import domain.constants.FinanceiroConstants;
import domain.dto.*;
import domain.entity.remote.Produto;
import domain.entity.local.Simulacao;
import domain.exception.ParametroInvalidoException;
import domain.exception.ProdutoException;
import domain.service.CalculadoraFinanceiraService;
import domain.service.ErrorHandlingService;
import domain.service.ProdutoElegibilidadeService;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CacheKey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import repository.ProdutoRepository;
import repository.SimulacaoRepository;
import resource.SimulacaoMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class SimulacaoService {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Inject
    @PersistenceUnit("produtos")
    ProdutoRepository produtoRepository;

    @Inject
    SimulacaoRepository simulacaoRepository;

    @Inject
    CalculadoraFinanceiraService calculadoraFinanceira;

    @Inject
    ProdutoElegibilidadeService produtoElegibilidade;

    @Inject
    ErrorHandlingService errorHandling;

    // Cache for product validation to avoid redundant DB queries
    private final Map<Integer, Produto> produtoCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Cache para produtos para evitar múltiplas consultas
    private List<Produto> produtosCache;
    private long ultimaAtualizacaoProdutos = 0;
    private static final long CACHE_TIMEOUT = 300000; // 5 minutos

    /**
     * Simula um empréstimo calculando as melhores opções de financiamento disponíveis.
     * Retorna status 200 mesmo quando não encontra produtos elegíveis, com mensagem informativa.
     */
    public SimulacaoResponseDTO simularEmprestimo(SimulacaoCreateDTO solicitacaoSimulacao, String requestId) {
        List<Produto> todosProdutos = buscarTodosProdutos();
        BigDecimal valorDesejado = solicitacaoSimulacao.getValorDesejado();
        int prazoMeses = solicitacaoSimulacao.getPrazo();

        // Usa o novo método que retorna Optional ao invés de lançar exceção
        Optional<Produto> melhorProdutoOpt = produtoElegibilidade.encontrarMelhorProdutoOptional(todosProdutos, valorDesejado, prazoMeses);

        if (melhorProdutoOpt.isEmpty()) {
            // Retorna resposta com status 200 mas indicando que não foi possível encontrar produto ideal
            SimulacaoResponseDTO respostaSemProduto = new SimulacaoResponseDTO();
            respostaSemProduto.setSucesso(false);
            respostaSemProduto.setMensagem(String.format(
                "Não foi possível encontrar um produto ideal para o valor de R$ %.2f e prazo de %d meses. " +
                "Verifique se os parâmetros estão dentro dos limites disponíveis e tente novamente.",
                valorDesejado, prazoMeses
            ));
            respostaSemProduto.setResultadoSimulacao(List.of());

            errorHandling.logarInfo(requestId, String.format(
                "Nenhum produto elegível encontrado - Valor: R$ %.2f, Prazo: %d meses",
                valorDesejado, prazoMeses
            ));

            return respostaSemProduto;
        }

        Produto melhorProduto = melhorProdutoOpt.get();
        List<ResultadoSimulacaoDTO> resultadosCalculados = calcularResultadosSimulacao(solicitacaoSimulacao, melhorProduto);

        ResultadoSimulacaoDTO resultadoPrice = encontrarResultadoPorTipo(resultadosCalculados);
        Simulacao simulacaoPersistida = persistirSimulacao(solicitacaoSimulacao, melhorProduto, resultadoPrice, valorDesejado);

        SimulacaoResponseDTO resposta = construirRespostaSimulacao(simulacaoPersistida, melhorProduto, resultadosCalculados);

        // Adiciona mensagem de sucesso e flag indicando sucesso
        resposta.setSucesso(true);
        resposta.setMensagem("Simulação realizada com sucesso. Produto ideal encontrado.");

        // Envio assíncrono ao Event Hub com retry mechanism
        enviarMensagemEventHubComRetry(resposta, requestId);

        return resposta;
    }

    /**
     * Lista as simulações com suporte a paginação.
     */
    public PaginaSimulacaoDTO listarSimulacoes(int numeroPagina, int quantidadePorPagina) {
        long totalRegistros = simulacaoRepository.count();
        List<Simulacao> simulacoesPaginadas = buscarSimulacoesPaginadas(numeroPagina, quantidadePorPagina);

        List<SimulacaoResumoDTO> resumosSimulacao = simulacoesPaginadas.stream()
            .map(SimulacaoMapper::toSimulacaoResumoDTO)
            .collect(Collectors.toList());

        return construirPaginaSimulacao(numeroPagina, quantidadePorPagina, totalRegistros, resumosSimulacao);
    }

    /**
     * Busca simulações filtradas por produto e/ou data, sem paginação.
     * Resultado é cacheado para melhorar performance em consultas frequentes.
     *
     * Este método implementa uma lógica de filtragem flexível baseada nos parâmetros fornecidos:
     *
     * CENÁRIOS SUPORTADOS:
     * 1. Sem parâmetros (data=null, produtoId=null):
     *    - Retorna TODAS as simulações feitas no dia ATUAL
     *    - Agrupa e categoriza por produto
     *
     * 2. Apenas data (?data=2024-01-15):
     *    - Retorna todas as simulações da DATA ESPECIFICADA
     *    - Agrupa e categoriza por produto
     *
     * 3. Apenas produto (?produtoId=123):
     *    - Retorna simulações do PRODUTO ESPECÍFICO feitas no dia ATUAL
     *    - Mantém agrupamento por produto
     *
     * 4. Data e produto (?data=2024-01-15&produtoId=123):
     *    - Retorna simulações do PRODUTO ESPECÍFICO na DATA ESPECÍFICA
     *    - Mantém agrupamento por produto
     *
     * @param dataFiltro Data no formato yyyy-MM-dd (opcional). Se null/vazio, usa data atual
     * @param produtoId ID do produto (opcional). Se null, considera todos os produtos
     * @return Simulações agrupadas por produto com estatísticas agregadas
     * @throws ParametroInvalidoException se a data estiver em formato inválido
     * @throws ProdutoException se o produtoId especificado não existir
     */
    @CacheResult(cacheName = "simulacoes-agregadas-por-produto")
    public SimulacaoPorProdutoDiaResponseDTO buscarSimulacoesPorProdutoEData(
            @CacheKey String dataFiltro,
            @CacheKey Integer produtoId,
            String requestId) {
        FiltroSimulacaoContext context = processarFiltrosSimulacao(dataFiltro, produtoId);
        errorHandling.logarInfo(requestId, String.format("Busca de simulações por produto e data: dataFiltro=%s, produtoId=%s", dataFiltro, produtoId));
        return construirRespostaPorProdutoEData(context.simulacoesFiltradas, context.dataConsulta, requestId);
    }

    /**
     * Busca simulações filtradas por produto e/ou data sem paginação, retornando simulações separadas por produto.
     * Resultado é cacheado para melhorar performance em consultas frequentes.
     *
     * Este método é semelhante ao anterior, mas em vez de agregar as simulações, retorna cada simulação
     * separadamente, agrupadas pelo produto correspondente.
     *
     * @param dataFiltro Data no formato yyyy-MM-dd (opcional). Se null/vazio, usa data atual
     * @param produtoId ID do produto (opcional). Se null, considera todos os produtos
     * @return Simulações separadas por produto
     * @throws ParametroInvalidoException se a data estiver em formato inválido
     * @throws ProdutoException se o produtoId especificado não existir
     */
    @CacheResult(cacheName = "simulacoes-por-produto")
    public SimulacoesPorProdutoResponseDTO buscarSimulacoesSeparadasPorProdutoEData(
            @CacheKey String dataFiltro,
            @CacheKey Integer produtoId,
            String requestId) {
        FiltroSimulacaoContext context = processarFiltrosSimulacao(dataFiltro, produtoId);
        errorHandling.logarInfo(requestId, String.format("Busca de simulações separadas por produto e data: dataFiltro=%s, produtoId=%s", dataFiltro, produtoId));
        return construirRespostaSeparadaPorProdutoEData(context.simulacoesFiltradas, context.dataConsulta, requestId);
    }

    // Métodos privados para organizar a lógica

    /**
     * Processa filtros comuns para evitar duplicação de código entre métodos de busca.
     */
    private FiltroSimulacaoContext processarFiltrosSimulacao(String dataFiltro, Integer produtoId) {
        LocalDate dataConsulta = determinarDataConsulta(dataFiltro);
        List<Simulacao> simulacoesFiltradas = aplicarFiltrosSimulacao(dataConsulta, produtoId);

        return new FiltroSimulacaoContext(dataConsulta, simulacoesFiltradas);
    }

    /**
     * Constrói resposta separada por produto (usado no segundo método de busca).
     */
    private SimulacoesPorProdutoResponseDTO construirRespostaSeparadaPorProdutoEData(
            List<Simulacao> simulacoesFiltradas, LocalDate dataConsulta, String requestId) {

        List<Produto> todosProdutos = buscarTodosProdutos();
        Map<Integer, List<Simulacao>> simulacoesPorProduto = agruparSimulacoesPorProduto(simulacoesFiltradas, todosProdutos);

        List<SimulacoesDeUmProdutoDTO> produtos = simulacoesPorProduto.entrySet().stream()
            .filter(entry -> entry.getKey() != -1)
            .map(entry -> construirSimulacoesDeUmProduto(entry, todosProdutos))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        errorHandling.logarInfo(requestId, String.format("Construída resposta separada por produto para %d produtos", produtos.size()));
        SimulacoesPorProdutoResponseDTO resposta = new SimulacoesPorProdutoResponseDTO();
        resposta.setDataReferencia(dataConsulta.toString());
        resposta.setProdutos(produtos);
        return resposta;
    }

    /**
     * Constrói DTO para simulações de um produto específico.
     */
    private SimulacoesDeUmProdutoDTO construirSimulacoesDeUmProduto(
            Map.Entry<Integer, List<Simulacao>> entry, List<Produto> todosProdutos) {

        Integer codigoProduto = entry.getKey();
        List<Simulacao> simulacoesProduto = entry.getValue();

        Produto produto = todosProdutos.stream()
            .filter(p -> p.getCoProduto().equals(codigoProduto))
            .findFirst().orElse(null);

        if (produto == null) return null;

        SimulacoesDeUmProdutoDTO dto = new SimulacoesDeUmProdutoDTO();
        dto.setCodigoProduto(produto.getCoProduto());
        dto.setDescricaoProduto(produto.getNoProduto());

        List<SimulacaoDetalheDTO> detalhes = simulacoesProduto.stream()
            .map(simulacao -> construirSimulacaoDetalhe(simulacao, produto))
            .collect(Collectors.toList());

        dto.setSimulacoes(detalhes);
        return dto;
    }

    /**
     * Constrói detalhe de uma simulação específica.
     */
    private SimulacaoDetalheDTO construirSimulacaoDetalhe(Simulacao simulacao, Produto produto) {
        SimulacaoDetalheDTO detalhe = new SimulacaoDetalheDTO();
        detalhe.setId(simulacao.getId());
        detalhe.setCodigoProduto(produto.getCoProduto());
        detalhe.setDescricaoProduto(produto.getNoProduto());
        detalhe.setValorDesejado(simulacao.getValorDesejado());
        detalhe.setPrazo(simulacao.getPrazo() != null ? simulacao.getPrazo().intValue() : null);
        detalhe.setTaxaJuro(simulacao.getTaxaMediaJuros());
        detalhe.setValorPrestacao(simulacao.getValorMedioPrestacao());
        detalhe.setDataSimulacao(simulacao.getDataSimulacao() != null ?
            simulacao.getDataSimulacao().toString() : null);
        return detalhe;
    }

    /**
     * Envia mensagem ao Event Hub com mecanismo de retry.
     */
    private void enviarMensagemEventHubComRetry(SimulacaoResponseDTO resposta, String requestId) {
        int tentativa = 1;

        while (tentativa <= MAX_RETRY_ATTEMPTS) {
            try {
                errorHandling.enviarMensagemEventHub(resposta);
                errorHandling.logarInfo(requestId, "Mensagem enviada ao Event Hub com sucesso na tentativa " + tentativa);
                return; // Sucesso, sai do método
            } catch (Exception e) {
                errorHandling.logarErro(requestId, "simularEmprestimo - Event Hub tentativa " + tentativa, e);

                if (tentativa < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * tentativa); // Backoff exponencial simples
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        errorHandling.logarErro(requestId, "Thread interrompida durante retry", ie);
                        break;
                    }
                }
                tentativa++;
            }
        }

        // Se chegou aqui, todas as tentativas falharam
        errorHandling.logarErro(requestId, "simularEmprestimo - Event Hub", new Exception("Todas as tentativas de envio ao Event Hub falharam."));
    }

    /**
     * Valida a existência de um produto usando cache para otimizar consultas.
     */
    private void validarExistenciaProdutoComCache(Integer produtoId) {
        // Primeiro verifica o cache
        if (produtoCache.containsKey(produtoId)) {
            return; // Produto existe no cache
        }

        // Se não está no cache, consulta o banco
        Produto produto = produtoRepository.findById(Long.valueOf(produtoId));
        if (produto == null) {
            throw ProdutoException.produtoNaoEncontrado(produtoId);
        }

        // Adiciona ao cache para próximas consultas
        produtoCache.put(produtoId, produto);
    }

    private List<Produto> buscarTodosProdutos() {
        long agora = System.currentTimeMillis();
        if (produtosCache == null || (agora - ultimaAtualizacaoProdutos) > CACHE_TIMEOUT) {
            produtosCache = produtoRepository.listAll();
            ultimaAtualizacaoProdutos = agora;
        }
        return produtosCache;
    }

    private List<ResultadoSimulacaoDTO> calcularResultadosSimulacao(SimulacaoCreateDTO simulacao, Produto produto) {
        ResultadoSimulacaoDTO resultadoSAC = calculadoraFinanceira.calcularResultado(simulacao, produto, FinanceiroConstants.TIPO_SAC);
        ResultadoSimulacaoDTO resultadoPrice = calculadoraFinanceira.calcularResultado(simulacao, produto, FinanceiroConstants.TIPO_PRICE);
        return List.of(resultadoSAC, resultadoPrice);
    }

    private ResultadoSimulacaoDTO encontrarResultadoPorTipo(List<ResultadoSimulacaoDTO> resultados) {
        return resultados.stream()
            .filter(resultado -> FinanceiroConstants.TIPO_PRICE.equals(resultado.getTipo()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Resultado não encontrado para o tipo: " + FinanceiroConstants.TIPO_PRICE));
    }

    @Transactional
    protected Simulacao persistirSimulacao(SimulacaoCreateDTO solicitacao, Produto produto,
                                         ResultadoSimulacaoDTO resultadoPrice, BigDecimal valorDesejado) {
        Simulacao novaSimulacao = criarNovaSimulacao(solicitacao, produto, resultadoPrice, valorDesejado);
        simulacaoRepository.persist(novaSimulacao);
        return novaSimulacao;
    }

    private Simulacao criarNovaSimulacao(SimulacaoCreateDTO solicitacao, Produto produto,
                                       ResultadoSimulacaoDTO resultadoPrice, BigDecimal valorDesejado) {
        Simulacao simulacao = new Simulacao();
        simulacao.setValorDesejado(valorDesejado);
        simulacao.setPrazo(solicitacao.getPrazo().longValue());
        simulacao.setTaxaMediaJuros(produto.getPcTaxaJuros().setScale(FinanceiroConstants.TAXA_SCALE, RoundingMode.HALF_UP));
        simulacao.setValorTotalDesejado(valorDesejado);

        BigDecimal valorTotalParcelas = calculadoraFinanceira.calcularValorTotalParcelas(resultadoPrice.getParcelas());
        simulacao.setValorTotalCredito(valorTotalParcelas);

        BigDecimal valorMedioPrestacao = calculadoraFinanceira.calcularValorMedioPrestacao(resultadoPrice.getParcelas());
        simulacao.setValorMedioPrestacao(valorMedioPrestacao);

        simulacao.setProduto(null);
        simulacao.setDataSimulacao(LocalDateTime.now());

        return simulacao;
    }

    private SimulacaoResponseDTO construirRespostaSimulacao(Simulacao simulacao, Produto produto,
                                                          List<ResultadoSimulacaoDTO> resultados) {
        SimulacaoResponseDTO resposta = new SimulacaoResponseDTO();
        resposta.setIdSimulacao(simulacao.getId());
        resposta.setCodigoProduto(produto.getCoProduto());
        resposta.setDescricaoProduto(produto.getNoProduto());
        resposta.setTaxaJuros(produto.getPcTaxaJuros().setScale(FinanceiroConstants.TAXA_SCALE, RoundingMode.HALF_UP));
        resposta.setResultadoSimulacao(resultados);
        return resposta;
    }

    private List<Simulacao> buscarSimulacoesPaginadas(int numeroPagina, int quantidadePorPagina) {
        return simulacaoRepository.findAll()
            .page(numeroPagina - 1, quantidadePorPagina)
            .list();
    }

    private PaginaSimulacaoDTO construirPaginaSimulacao(int numeroPagina, int quantidadePorPagina,
                                                       long totalRegistros, List<SimulacaoResumoDTO> resumos) {
        PaginaSimulacaoDTO paginaDTO = new PaginaSimulacaoDTO();
        paginaDTO.setPagina(numeroPagina);
        paginaDTO.setQtdRegistros(totalRegistros);
        paginaDTO.setQtdRegistrosPagina(quantidadePorPagina);
        paginaDTO.setRegistros(resumos);
        return paginaDTO;
    }

    private LocalDate determinarDataConsulta(String dataFiltro) {
        if (dataFiltro == null || dataFiltro.isBlank()) {
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(dataFiltro);
        } catch (Exception e) {
            throw new domain.exception.ParametroInvalidoException(
                domain.enums.MensagemErro.FORMATO_DATA_INVALIDO,
                "Data inválida: " + dataFiltro + ". Use o formato YYYY-MM-DD."
            );
        }
    }

    /**
     * Aplica filtros de data e produto nas simulações baseado nos cenários:
     * - Sem parâmetros ou só data: filtra por data
     * - Com produto: filtra por data e produto específico
     */
    private List<Simulacao> aplicarFiltrosSimulacao(LocalDate dataConsulta, Integer produtoId) {
        List<Simulacao> todasSimulacoes = simulacaoRepository.listAll();

        // Filtra por data primeiro
        List<Simulacao> simulacoesPorData = todasSimulacoes.stream()
            .filter(simulacao -> simulacao.getDataSimulacao().toLocalDate().equals(dataConsulta))
            .collect(Collectors.toList());

        // Se não há produto específico, retorna todas as simulações da data
        if (produtoId == null) {
            return simulacoesPorData;
        }

        // Valida existência do produto com cache e filtra por ele
        validarExistenciaProdutoComCache(produtoId);
        return filtrarSimulacoesPorProduto(simulacoesPorData, produtoId);
    }


    private List<Simulacao> filtrarSimulacoesPorProduto(List<Simulacao> simulacoes, Integer produtoId) {
        List<Produto> todosProdutos = buscarTodosProdutos();

        return simulacoes.stream()
            .filter(simulacao -> {
                Optional<Produto> produtoAssociado = produtoElegibilidade.encontrarProdutoPorSimulacao(
                    todosProdutos, simulacao.getValorDesejado(), simulacao.getPrazo().intValue()
                );
                return produtoAssociado
                    .map(produto -> produto.getCoProduto().equals(produtoId))
                    .orElse(false);
            })
            .toList();
    }

    /**
     * Constrói a resposta com simulações agrupadas por produto.
     */
    private SimulacaoPorProdutoDiaResponseDTO construirRespostaPorProdutoEData(List<Simulacao> simulacoesFiltradas, LocalDate dataConsulta, String requestId) {
        errorHandling.logarInfo(requestId, String.format("Construindo resposta para %d simulações filtradas", simulacoesFiltradas.size()));
        List<Produto> todosProdutos = buscarTodosProdutos();
        Map<Integer, List<Simulacao>> simulacoesPorProduto = agruparSimulacoesPorProduto(simulacoesFiltradas, todosProdutos);
        List<SimulacaoProdutoDiaResumoDTO> resumosPorProduto = construirResumosPorProduto(simulacoesPorProduto, todosProdutos);

        SimulacaoPorProdutoDiaResponseDTO resposta = new SimulacaoPorProdutoDiaResponseDTO();
        resposta.setDataReferencia(dataConsulta.toString());
        resposta.setSimulacoes(resumosPorProduto);

        return resposta;
    }

    /**
     * Agrupa simulações por produto baseado na elegibilidade de cada simulação.
     */
    private Map<Integer, List<Simulacao>> agruparSimulacoesPorProduto(List<Simulacao> simulacoes, List<Produto> todosProdutos) {
        return simulacoes.stream()
            .collect(Collectors.groupingBy(simulacao -> {
                Optional<Produto> produtoAssociado = produtoElegibilidade.encontrarProdutoPorSimulacao(
                    todosProdutos, simulacao.getValorDesejado(), simulacao.getPrazo().intValue()
                );
                return produtoAssociado
                    .map(Produto::getCoProduto)
                    .orElse(-1); // Produto não identificado
            }))
            .entrySet().stream()
            .filter(entry -> entry.getKey() != -1) // Remove simulações sem produto identificado
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Constrói resumos estatísticos para cada grupo de produto.
     */
    private List<SimulacaoProdutoDiaResumoDTO> construirResumosPorProduto(Map<Integer, List<Simulacao>> simulacoesPorProduto, List<Produto> todosProdutos) {
        return simulacoesPorProduto.entrySet().stream()
            .map(entry -> {
                Integer codigoProduto = entry.getKey();
                List<Simulacao> simulacoesProduto = entry.getValue();

                Produto produto = todosProdutos.stream()
                    .filter(p -> p.getCoProduto().equals(codigoProduto))
                    .findFirst()
                    .orElse(null);

                if (produto == null) {
                    return null; // Skip produtos não encontrados
                }

                return construirResumoIndividual(produto, simulacoesProduto);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Constrói resumo individual para um produto específico com suas simulações.
     */
    private SimulacaoProdutoDiaResumoDTO construirResumoIndividual(Produto produto, List<Simulacao> simulacoes) {
        SimulacaoProdutoDiaResumoDTO resumo = new SimulacaoProdutoDiaResumoDTO();
        resumo.setCodigoProduto(produto.getCoProduto());
        resumo.setDescricaoProduto(produto.getNoProduto());
        resumo.setTaxaMediaJuro(produto.getPcTaxaJuros().setScale(FinanceiroConstants.TAXA_SCALE, RoundingMode.HALF_UP));

        // Calcula médias das simulações
        BigDecimal valorMedioPrestacao = calcularMediaPrestacoes(simulacoes);
        BigDecimal valorTotalDesejado = calcularTotalValoresDesejados(simulacoes);
        BigDecimal valorTotalCredito = calcularTotalValoresCredito(simulacoes);

        resumo.setValorMedioPrestacao(valorMedioPrestacao);
        resumo.setValorTotalDesejado(valorTotalDesejado);
        resumo.setValorTotalCredito(valorTotalCredito);

        return resumo;
    }

    /**
     * Calcula a média das prestações das simulações.
     */
    private BigDecimal calcularMediaPrestacoes(List<Simulacao> simulacoes) {
        return simulacoes.stream()
            .map(Simulacao::getValorMedioPrestacao)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(simulacoes.size()), FinanceiroConstants.TAXA_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o total dos valores desejados das simulações.
     */
    private BigDecimal calcularTotalValoresDesejados(List<Simulacao> simulacoes) {
        return simulacoes.stream()
            .map(Simulacao::getValorTotalDesejado)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula o total dos valores de crédito das simulações.
     */
    private BigDecimal calcularTotalValoresCredito(List<Simulacao> simulacoes) {
        return simulacoes.stream()
            .map(Simulacao::getValorTotalCredito)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Classe interna para encapsular contexto de filtros
     */
    private static class FiltroSimulacaoContext {
        final LocalDate dataConsulta;
        final List<Simulacao> simulacoesFiltradas;

        FiltroSimulacaoContext(LocalDate dataConsulta, List<Simulacao> simulacoesFiltradas) {
            this.dataConsulta = dataConsulta;
            this.simulacoesFiltradas = simulacoesFiltradas;
        }
    }
}
