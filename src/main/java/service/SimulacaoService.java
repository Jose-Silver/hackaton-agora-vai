package service;
import domain.dto.simulacao.create.request.SimulacaoCreateDTO;
import domain.dto.simulacao.create.response.PaginaSimulacaoDTO;
import domain.dto.simulacao.create.response.PaginaSimulacaoSimplificadaDTO;
import domain.dto.simulacao.create.response.ResultadoSimulacaoDTO;
import domain.dto.simulacao.create.response.SimulacaoDetalheDTO;
import domain.dto.simulacao.create.response.SimulacaoResponseDTO;
import domain.dto.simulacao.list.response.SimulacaoResumoSimplificadoDTO;
import domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaDTO;
import domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaResponseDTO;
import domain.entity.local.Simulacao;
import domain.entity.remote.Produto;
import domain.enums.FinanceiroConstant;
import domain.enums.SystemConstant;
import domain.enums.TipoAmortizacao;
import domain.exception.ParametroInvalidoException;
import domain.exception.ProdutoException;
import domain.service.CalculadoraFinanceiraService;
import domain.service.ErrorHandlingService;
import domain.service.ProdutoElegibilidadeService;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.PersistenceUnit;
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
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class SimulacaoService {

    private static final int MAX_RETRY_ATTEMPTS = SystemConstant.MAX_RETRY_ATTEMPTS.getIntValue();
    private static final long RETRY_DELAY_MS = SystemConstant.RETRY_DELAY_MS.getLongValue();

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
    private static final long CACHE_TIMEOUT = SystemConstant.CACHE_TIMEOUT_MS.getLongValue(); // 5 minutos

    /**
     * Simula um empréstimo calculando as melhores opções de financiamento disponíveis.
     * Lança exceção quando não encontra produtos elegíveis.
     */
    public SimulacaoResponseDTO simularEmprestimo(SimulacaoCreateDTO solicitacaoSimulacao, String requestId) {
        List<Produto> todosProdutos = buscarTodosProdutos();
        BigDecimal valorDesejado = solicitacaoSimulacao.getValorDesejado();
        int prazoMeses = solicitacaoSimulacao.getPrazo();

        // Usa o novo método que retorna Optional ao invés de lançar exceção
        Optional<Produto> melhorProdutoOpt = produtoElegibilidade.encontrarMelhorProdutoOptional(todosProdutos, valorDesejado, prazoMeses);

        if (melhorProdutoOpt.isEmpty()) {
            // Lança exceção que retorna HTTP 400 com mensagem apropriada
            errorHandling.logarInfo(requestId, String.format(
                "Nenhum produto elegível encontrado - Valor: R$ %.2f, Prazo: %d meses",
                valorDesejado, prazoMeses
            ));

            throw ProdutoException.produtosNaoElegiveis(valorDesejado.doubleValue(), prazoMeses);
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
     * Lista as simulações com suporte a paginação, retornando apenas os campos essenciais.
     */
    public PaginaSimulacaoSimplificadaDTO listarSimulacoes(int numeroPagina, int quantidadePorPagina) {
        long totalRegistros = simulacaoRepository.count();
        List<Simulacao> simulacoesPaginadas = buscarSimulacoesPaginadas(numeroPagina, quantidadePorPagina);

        List<SimulacaoResumoSimplificadoDTO> resumosSimulacao = simulacoesPaginadas.stream()
            .map(SimulacaoMapper::toSimulacaoResumoSimplificadoDTO)
            .collect(Collectors.toList());

        return construirPaginaSimulacaoSimplificada(numeroPagina, quantidadePorPagina, totalRegistros, resumosSimulacao);
    }

    /**
     * Busca simulações filtradas por produto e/ou data, retornando uma lista de simulações individuais.
     * Cada simulação é retornada como um item separado na lista.
     *
     * Este método implementa uma lógica de filtragem flexível baseada nos parâmetros fornecidos:
     *
     * CENÁRIOS SUPORTADOS:
     * 1. Sem parâmetros (data=null, produtoId=null):
     *    - Retorna TODAS as simulações feitas no dia ATUAL
     *    - Cada simulação como item individual
     *
     * 2. Apenas data (?data=2024-01-15):
     *    - Retorna todas as simulações da DATA ESPECIFICADA
     *    - Cada simulação como item individual
     *
     * 3. Apenas produto (?produtoId=123):
     *    - Retorna simulações do PRODUTO ESPECÍFICO feitas no dia ATUAL
     *    - Cada simulação como item individual
     *
     * 4. Data e produto (?data=2024-01-15&produtoId=123):
//     *    - Retorna simulações do PRODUTO ESPECÍFICO na DATA ESPECÍFICA
//     *    - Cada simulação como item individual
//     *
//     * @param dataSimulacao Data no formato yyyy-MM-dd (opcional). Se null/vazio, usa data atual
     * @param produtoId ID do produto (opcional). Se null, considera todos os produtos
     * @param requestId ID da requisição para logging
     * @return Lista de simulações individuais filtradas
     * @throws ParametroInvalidoException se a data estiver em formato inválido
     * @throws ProdutoException se o produtoId especificado não existir
     */
    public SimulacaoPorProdutoDiaResponseDTO buscarSimulacoesPorProdutoEData(
            @CacheKey String dataFiltro,
            @CacheKey Integer produtoId,
            String requestId) {

        errorHandling.logarInfo(requestId, String.format("Busca de simulações por produto e data: dataFiltro=%s, produtoId=%s", dataFiltro, produtoId));

        FiltroSimulacaoContext context = processarFiltrosSimulacao(dataFiltro, produtoId);
        List<Produto> todosProdutos = buscarTodosProdutos();


        List<SimulacaoPorProdutoDiaDTO> listaSimulacoes = context.simulacoesFiltradas.stream()
            .map(simulacao -> construirSimulacaoIndividualDTO(simulacao, todosProdutos))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        SimulacaoPorProdutoDiaResponseDTO resposta = new SimulacaoPorProdutoDiaResponseDTO();
        resposta.setDataReferencia(context.dataConsulta.toString());
        resposta.setSimulacoes(listaSimulacoes);

        errorHandling.logarInfo(requestId, String.format("Retornando %d simulações", listaSimulacoes.size()));
        return resposta;
    }


    /**
     * Constrói DTO para uma simulação individual.
     */
    private SimulacaoPorProdutoDiaDTO construirSimulacaoIndividualDTO(
            Simulacao simulacao,
            List<Produto> todosProdutos) {

        // Encontra o produto associado à simulação baseado na elegibilidade
        Optional<Produto> produtoOpt = produtoElegibilidade.encontrarProdutoPorSimulacao(
            todosProdutos,
            simulacao.getValorDesejado(),
            simulacao.getPrazo().intValue()
        );

        if (produtoOpt.isEmpty()) {
            return null; // Simulação sem produto elegível identificado
        }

        Produto produto = produtoOpt.get();

        SimulacaoPorProdutoDiaDTO dto = new SimulacaoPorProdutoDiaDTO();
        dto.setCodigoProduto(produto.getCoProduto());
        dto.setDescricaoProduto(produto.getNoProduto());

        // Dados individuais da simulação (não agregados)
        dto.setTaxaMediaJuro(simulacao.getTaxaMediaJuros() != null ?
            simulacao.getTaxaMediaJuros().doubleValue() : null);
        dto.setValorMedioPrestacao(simulacao.getValorMedioPrestacao() != null ?
            simulacao.getValorMedioPrestacao().doubleValue() : null);
        dto.setValorTotalDesejado(simulacao.getValorTotalDesejado() != null ?
            simulacao.getValorTotalDesejado().doubleValue() : null);
        dto.setValorTotalCredito(simulacao.getValorTotalCredito() != null ?
            simulacao.getValorTotalCredito().doubleValue() : null);

        return dto;
    }

    /**
     * Constrói DTO agregado para um produto específico.
     */
    private SimulacaoPorProdutoDiaDTO construirSimulacaoPorProdutoDiaDTO(
            Map.Entry<Integer, List<Simulacao>> entry,
            List<Produto> todosProdutos) {

        Integer codigoProduto = entry.getKey();
        List<Simulacao> simulacoesProduto = entry.getValue();

        if (simulacoesProduto.isEmpty()) {
            return null;
        }

        // Encontra o produto correspondente
        Produto produto = todosProdutos.stream()
            .filter(p -> p.getCoProduto().equals(codigoProduto))
            .findFirst().orElse(null);

        if (produto == null) {
            return null;
        }

        SimulacaoPorProdutoDiaDTO dto = new SimulacaoPorProdutoDiaDTO();
        dto.setCodigoProduto(produto.getCoProduto());
        dto.setDescricaoProduto(produto.getNoProduto());

        // Calcula agregações
        List<BigDecimal> taxasJuros = simulacoesProduto.stream()
            .map(Simulacao::getTaxaMediaJuros)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<BigDecimal> valoresPrestacao = simulacoesProduto.stream()
            .map(Simulacao::getValorMedioPrestacao)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<BigDecimal> valoresDesejados = simulacoesProduto.stream()
            .map(Simulacao::getValorDesejado)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<BigDecimal> valoresCredito = simulacoesProduto.stream()
            .map(Simulacao::getValorTotalCredito)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // Taxa média de juros
        if (!taxasJuros.isEmpty()) {
            BigDecimal somaJuros = taxasJuros.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTaxaMediaJuro(somaJuros.divide(BigDecimal.valueOf(taxasJuros.size()), 4, RoundingMode.HALF_UP).doubleValue());
        }

        // Valor médio da prestação
        if (!valoresPrestacao.isEmpty()) {
            BigDecimal somaPrestacoes = valoresPrestacao.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setValorMedioPrestacao(somaPrestacoes.divide(BigDecimal.valueOf(valoresPrestacao.size()), 2, RoundingMode.HALF_UP).doubleValue());
        }

        // Valor total desejado (soma de todos os valores desejados)
        if (!valoresDesejados.isEmpty()) {
            BigDecimal totalDesejado = valoresDesejados.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setValorTotalDesejado(totalDesejado.doubleValue());
        }

        // Valor total de crédito (soma de todos os valores de crédito)
        if (!valoresCredito.isEmpty()) {
            BigDecimal totalCredito = valoresCredito.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setValorTotalCredito(totalCredito.doubleValue());
        }

        return dto;
    }



    /**
     * Processa filtros comuns para evitar duplicação de código entre métodos de busca.
     */
    private FiltroSimulacaoContext processarFiltrosSimulacao(String dataFiltro, Integer produtoId) {
        LocalDate dataConsulta = determinarDataConsulta(dataFiltro);
        List<Simulacao> simulacoesFiltradas = aplicarFiltrosSimulacao(dataConsulta, produtoId);

        return new FiltroSimulacaoContext(dataConsulta, simulacoesFiltradas);
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
        ResultadoSimulacaoDTO resultadoSAC = calculadoraFinanceira.calcularResultado(simulacao, produto, TipoAmortizacao.SAC.getCodigo());
        ResultadoSimulacaoDTO resultadoPrice = calculadoraFinanceira.calcularResultado(simulacao, produto, TipoAmortizacao.PRICE.getCodigo());
        return List.of(resultadoSAC, resultadoPrice);
    }

    private ResultadoSimulacaoDTO encontrarResultadoPorTipo(List<ResultadoSimulacaoDTO> resultados) {
        return resultados.stream()
            .filter(resultado -> TipoAmortizacao.PRICE.getCodigo().equals(resultado.getTipo()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Resultado não encontrado para o tipo: " + TipoAmortizacao.PRICE.getCodigo()));
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
        simulacao.setTaxaMediaJuros(produto.getPcTaxaJuros().setScale(FinanceiroConstant.TAXA_SCALE.getValor(), RoundingMode.HALF_UP));
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
        resposta.setTaxaJuros(produto.getPcTaxaJuros().setScale(FinanceiroConstant.TAXA_SCALE.getValor(), RoundingMode.HALF_UP));
        resposta.setResultadoSimulacao(resultados);
        return resposta;
    }

    private List<Simulacao> buscarSimulacoesPaginadas(int numeroPagina, int quantidadePorPagina) {
        return simulacaoRepository.findAll()
            .page(numeroPagina - 1, quantidadePorPagina)
            .list();
    }



    private PaginaSimulacaoSimplificadaDTO construirPaginaSimulacaoSimplificada(int numeroPagina, int quantidadePorPagina,
                                                       long totalRegistros, List<SimulacaoResumoSimplificadoDTO> resumos) {
        PaginaSimulacaoSimplificadaDTO paginaDTO = new PaginaSimulacaoSimplificadaDTO();
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
     * Constrói a resposta com simulações agrupadas por produto para o método buscarSimulacoesPorProdutoEData.
     */
    private SimulacaoPorProdutoDiaResponseDTO construirRespostaPorProdutoEData(List<Simulacao> simulacoesFiltradas, LocalDate dataConsulta, String requestId) {
        errorHandling.logarInfo(requestId, String.format("Construindo resposta para %d simulações filtradas", simulacoesFiltradas.size()));
        List<Produto> todosProdutos = buscarTodosProdutos();
        Map<Integer, List<Simulacao>> simulacoesPorProduto = agruparSimulacoesPorProduto(simulacoesFiltradas, todosProdutos);

        // Converte para DTOs com dados agregados
        List<SimulacaoPorProdutoDiaDTO> produtos = simulacoesPorProduto.entrySet().stream()
            .filter(entry -> entry.getKey() != -1)
            .map(entry -> construirSimulacaoPorProdutoDiaDTO(entry, todosProdutos))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        SimulacaoPorProdutoDiaResponseDTO resposta = new SimulacaoPorProdutoDiaResponseDTO();
        resposta.setDataReferencia(dataConsulta.toString());
        resposta.setSimulacoes(produtos);
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
