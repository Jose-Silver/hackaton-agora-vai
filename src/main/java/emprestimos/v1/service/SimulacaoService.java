package emprestimos.v1.service;

import emprestimos.v1.domain.dto.simulacao.buscar.response.SimulacaoDetalhesDTO;
import emprestimos.v1.domain.dto.simulacao.create.request.SimulacaoCreateDTO;
import emprestimos.v1.domain.dto.simulacao.create.response.PaginaSimulacaoSimplificadaDTO;
import emprestimos.v1.domain.dto.simulacao.create.response.ParcelaDTO;
import emprestimos.v1.domain.dto.simulacao.create.response.ResultadoSimulacaoDTO;
import emprestimos.v1.domain.dto.simulacao.create.response.SimulacaoResponseDTO;
import emprestimos.v1.domain.dto.simulacao.list.response.SimulacaoResumoSimplificadoDTO;
import emprestimos.v1.domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaDTO;
import emprestimos.v1.domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaResponseDTO;
import emprestimos.v1.domain.dto.simulacao.parcelas.response.ParcelasSimulacaoDTO;
import emprestimos.v1.domain.dto.simulacao.parcela.response.ParcelaEspecificaDTO;
import emprestimos.v1.domain.entity.local.Simulacao;
import emprestimos.v1.domain.entity.remote.Produto;
import emprestimos.v1.domain.enums.FinanceiroConstant;
import emprestimos.v1.domain.enums.MensagemErro;
import emprestimos.v1.domain.enums.SystemConstant;
import emprestimos.v1.domain.enums.TipoAmortizacao;
import emprestimos.v1.domain.exception.ParametroInvalidoException;
import emprestimos.v1.domain.exception.ProdutoException;
import emprestimos.v1.domain.exception.SimulacaoException;
import emprestimos.v1.domain.service.CalculadoraFinanceiraService;
import emprestimos.v1.domain.service.ErrorHandlingService;
import emprestimos.v1.domain.service.ProdutoElegibilidadeService;
import io.quarkus.cache.CacheKey;
import io.quarkus.hibernate.orm.PersistenceUnit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import emprestimos.v1.repository.ProdutoRepository;
import emprestimos.v1.repository.SimulacaoRepository;
import emprestimos.v1.resource.SimulacaoMapper;
import emprestimos.v1.mapper.ProdutoAggregationMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class SimulacaoService {

    private static final int MAX_RETRY_ATTEMPTS = SystemConstant.MAX_RETRY_ATTEMPTS.getIntValue();
    private static final long RETRY_DELAY_MS = SystemConstant.RETRY_DELAY_MS.getLongValue();
    private static final long CACHE_TIMEOUT = SystemConstant.CACHE_TIMEOUT_MS.getLongValue();

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

    @Inject
    emprestimos.v1.mapper.SimulacaoMapper simulacaoMapper;

    @Inject
    ProdutoAggregationMapper produtoAggregationMapper;

    private final Map<Integer, Produto> produtoCache = new ConcurrentHashMap<>();
    private List<Produto> produtosCache;
    private long ultimaAtualizacaoProdutos = 0;

    /**
     * Simula um empréstimo calculando as melhores opções de financiamento disponíveis.
     * Lança exceção quando não encontra produtos elegíveis.
     */
    public SimulacaoResponseDTO simularEmprestimo(SimulacaoCreateDTO solicitacaoSimulacao, String requestId
    ) {
        var todosProdutos = buscarTodosProdutos();
        var valorDesejado = solicitacaoSimulacao.getValorDesejado();
        var prazoMeses = solicitacaoSimulacao.getPrazo();

        var melhorProdutoOpt = produtoElegibilidade.encontrarMelhorProdutoOptional(todosProdutos, valorDesejado, prazoMeses);

        if (melhorProdutoOpt.isEmpty()) {
            throw ProdutoException.produtosNaoElegiveis(valorDesejado.doubleValue(), prazoMeses);
        }

        var melhorProduto = melhorProdutoOpt.get();
        var resultadosCalculados = calcularResultadosSimulacao(solicitacaoSimulacao, melhorProduto);
        var resultadoPrice = encontrarResultadoPorTipo(resultadosCalculados);
        var simulacaoPersistida = persistirSimulacao(solicitacaoSimulacao, melhorProduto, resultadoPrice, valorDesejado);

        var resposta = construirRespostaSimulacao(simulacaoPersistida, melhorProduto, resultadosCalculados);
        resposta.setSucesso(true);
        resposta.setMensagem("Simulação realizada com sucesso. Produto ideal encontrado.");

        enviarMensagemEventHubComRetry(resposta, requestId);
        return resposta;
    }

    /**
     * Lista as simulações com suporte a paginação, retornando apenas os campos essenciais.
     */
    public PaginaSimulacaoSimplificadaDTO listarSimulacoes(int numeroPagina, int quantidadePorPagina) {
        var totalRegistros = simulacaoRepository.count();
        var simulacoesPaginadas = buscarSimulacoesPaginadas(numeroPagina, quantidadePorPagina);
        var resumosSimulacao = simulacoesPaginadas.stream()
            .map(SimulacaoMapper::toSimulacaoResumoSimplificadoDTO)
            .toList();

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
     *    - Retorna simulações do PRODUTO ESPECÍFICO na DATA ESPECÍFICA
     *    - Cada simulação como item individual
     *
     * @param dataSimulacao Data no formato yyyy-MM-dd (opcional). Se null/vazio, usa data atual
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

        var context = processarFiltrosSimulacao(dataFiltro, produtoId);
        var todosProdutos = buscarTodosProdutos();
        var listaSimulacoes = context.simulacoesFiltradas.stream()
            .map(simulacao -> construirSimulacaoIndividualDTO(simulacao, todosProdutos))
            .filter(Objects::nonNull)
            .toList();

        var resposta = new SimulacaoPorProdutoDiaResponseDTO();
        resposta.setDataReferencia(context.dataConsulta.toString());
        resposta.setSimulacoes(listaSimulacoes);

        errorHandling.logarInfo(requestId, String.format("Retornando %d simulações", listaSimulacoes.size()));
        return resposta;
    }


    /**
     * Constrói DTO para uma simulação individual usando mapper.
     */
    private SimulacaoPorProdutoDiaDTO construirSimulacaoIndividualDTO(Simulacao simulacao, List<Produto> todosProdutos) {
        var produtoOpt = produtoElegibilidade.encontrarProdutoPorSimulacao(
            todosProdutos, simulacao.getValorDesejado(), simulacao.getPrazo().intValue()
        );

        if (produtoOpt.isEmpty()) {
            return null; // Simulação sem produto elegível identificado
        }

        return simulacaoMapper.toSimulacaoPorProdutoDiaDTO(simulacao, produtoOpt.get());
    }

    /**
     * Constrói DTO agregado para um produto específico usando mapper.
     */
    private SimulacaoPorProdutoDiaDTO construirSimulacaoPorProdutoDiaDTO(Map.Entry<Integer, List<Simulacao>> entry, List<Produto> todosProdutos) {
        var codigoProduto = entry.getKey();
        var simulacoesProduto = entry.getValue();

        if (simulacoesProduto.isEmpty()) {
            return null;
        }

        var produto = todosProdutos.stream()
            .filter(p -> p.getCoProduto().equals(codigoProduto))
            .findFirst()
            .orElse(null);

        if (produto == null) {
            return null;
        }

        return produtoAggregationMapper.toAggregatedSimulacaoPorProdutoDiaDTO(produto, simulacoesProduto);
    }

    private SimulacaoResponseDTO construirRespostaSimulacao(Simulacao simulacao, Produto produto, List<ResultadoSimulacaoDTO> resultados) {
        return simulacaoMapper.toSimulacaoResponseDTO(simulacao, produto, resultados);
    }

    private PaginaSimulacaoSimplificadaDTO construirPaginaSimulacaoSimplificada(int numeroPagina, int quantidadePorPagina,
                                                                              long totalRegistros, List<SimulacaoResumoSimplificadoDTO> resumos) {
        return simulacaoMapper.toPaginaSimulacaoSimplificadaDTO(numeroPagina, quantidadePorPagina, totalRegistros, resumos);
    }

    /**
     * Busca uma simulação específica pelo seu ID.
     *
     * @param id ID da simulação a ser buscada
     * @param requestId ID da requisição para logging
     * @return Detalhes completos da simulação incluindo parcelas SAC e PRICE
     * @throws SimulacaoException se a simulação não for encontrada
     */
    public SimulacaoDetalhesDTO buscarSimulacaoPorId(Long id, String requestId) {
        errorHandling.logarInfo(requestId, String.format("Buscando simulação por ID: %d", id));

        var simulacao = buscarSimulacaoOuLancarExcecao(id, requestId);
        var todosProdutos = buscarTodosProdutos();
        var produtoOpt = produtoElegibilidade.encontrarProdutoPorSimulacao(
            todosProdutos, simulacao.getValorDesejado(), simulacao.getPrazo().intValue()
        );

        var dto = construirSimulacaoDetalhesDTO(simulacao);

        if (produtoOpt.isPresent()) {
            preencherInformacoesProdutoEParcelas(dto, simulacao, produtoOpt.get(), requestId);
        } else {
            dto.setResultadosSimulacao(List.of());
            errorHandling.logarInfo(requestId, String.format("Produto não encontrado para simulação ID: %d", id));
        }

        errorHandling.logarInfo(requestId, String.format("Simulação encontrada com sucesso - ID: %d", id));
        return dto;
    }

    /**
     * Busca todas as parcelas de um tipo específico de amortização para uma simulação.
     *
     * @param id ID da simulação
     * @param tipoAmortizacao Tipo de amortização (SAC ou PRICE)
     * @param requestId ID da requisição para logging
     * @return DTO com todas as parcelas do tipo especificado
     * @throws SimulacaoException se a simulação não for encontrada
     * @throws IllegalArgumentException se o tipo de amortização for inválido
     */
    public ParcelasSimulacaoDTO buscarParcelasPorTipoAmortizacao(Long id, String tipoAmortizacao, String requestId) {
        errorHandling.logarInfo(requestId, String.format("Buscando parcelas por tipo de amortização - SimulacaoId: %d, Tipo: %s", id, tipoAmortizacao));

        var tipo = validarTipoAmortizacao(tipoAmortizacao, requestId);
        var simulacao = buscarSimulacaoOuLancarExcecao(id, requestId);
        var produto = buscarProdutoElegivelOuLancarExcecao(simulacao, requestId);

        var resultado = calcularParcelasParaTipo(simulacao, produto, tipo);
        var dto = construirParcelasSimulacaoDTO(simulacao, produto, tipo, resultado);

        errorHandling.logarInfo(requestId, String.format(
            "Parcelas calculadas com sucesso - SimulacaoId: %d, Tipo: %s, Quantidade: %d",
            id, tipo.getCodigo(), resultado.getParcelas().size()
        ));

        return dto;
    }

    /**
     * Busca uma parcela específica de um tipo de amortização para uma simulação.
     *
     * @param id ID da simulação
     * @param tipoAmortizacao Tipo de amortização (SAC ou PRICE)
     * @param parcelaId Número da parcela específica
     * @param requestId ID da requisição para logging
     * @return DTO com informações detalhadas da parcela específica
     * @throws SimulacaoException se a simulação não for encontrada
     * @throws IllegalArgumentException se o tipo de amortização for inválido
     * @throws ParametroInvalidoException se a parcela não existir
     */
    public ParcelaEspecificaDTO buscarParcelaEspecifica(Long id, String tipoAmortizacao, Long parcelaId, String requestId) {
        errorHandling.logarInfo(requestId, String.format("Buscando parcela específica - SimulacaoId: %d, Tipo: %s, ParcelaId: %d", id, tipoAmortizacao, parcelaId));

        var tipo = validarTipoAmortizacao(tipoAmortizacao, requestId);
        validarNumeroParcelaPositivo(parcelaId, requestId);

        var simulacao = buscarSimulacaoOuLancarExcecao(id, requestId);
        var produto = buscarProdutoElegivelOuLancarExcecao(simulacao, requestId);

        var resultado = calcularParcelasParaTipo(simulacao, produto, tipo);
        var parcelaEspecifica = buscarParcelaEspecificaOuLancarExcecao(resultado, parcelaId, requestId);

        var dto = construirParcelaEspecificaDTO(simulacao, produto, tipo, parcelaEspecifica, resultado);

        errorHandling.logarInfo(requestId, String.format(
            "Parcela específica encontrada com sucesso - SimulacaoId: %d, Tipo: %s, Parcela: %d",
            id, tipo.getCodigo(), parcelaId
        ));

        return dto;
    }

    // Métodos auxiliares para validação e busca
    private TipoAmortizacao validarTipoAmortizacao(String tipoAmortizacao, String requestId) {
        try {
            return TipoAmortizacao.porCodigo(tipoAmortizacao);
        } catch (IllegalArgumentException e) {
            errorHandling.logarInfo(requestId, String.format("Tipo de amortização inválido: %s", tipoAmortizacao));
            throw new ParametroInvalidoException(
                MensagemErro.TIPO_AMORTIZACAO_INVALIDO,
                String.format("Tipo de amortização inválido: %s. Valores aceitos: SAC, PRICE", tipoAmortizacao)
            );
        }
    }

    private void validarNumeroParcelaPositivo(Long parcelaId, String requestId) {
        if (parcelaId <= 0) {
            errorHandling.logarInfo(requestId, String.format("Número de parcela inválido: %d", parcelaId));
            throw new ParametroInvalidoException(
                MensagemErro.PARAMETROS_INVALIDOS,
                String.format("Número da parcela deve ser maior que zero. Valor informado: %d", parcelaId)
            );
        }
    }

    private Simulacao buscarSimulacaoOuLancarExcecao(Long id, String requestId) {
        var simulacao = simulacaoRepository.findById(id);
        if (simulacao == null) {
            errorHandling.logarInfo(requestId, String.format("Simulação não encontrada para ID: %d", id));
            throw new SimulacaoException(
                "Simulação não encontrada",
                String.format("Não foi encontrada simulação com ID: %d", id)
            );
        }
        return simulacao;
    }

    private Produto buscarProdutoElegivelOuLancarExcecao(Simulacao simulacao, String requestId) {
        var todosProdutos = buscarTodosProdutos();
        var produtoOpt = produtoElegibilidade.encontrarProdutoPorSimulacao(
            todosProdutos, simulacao.getValorDesejado(), simulacao.getPrazo().intValue()
        );

        if (produtoOpt.isEmpty()) {
            errorHandling.logarInfo(requestId, String.format("Produto não encontrado para simulação ID: %d", simulacao.getId()));
            throw new SimulacaoException(
                "Produto não encontrado",
                String.format("Não foi possível encontrar produto elegível para a simulação ID: %d", simulacao.getId())
            );
        }

        return produtoOpt.get();
    }

    private ResultadoSimulacaoDTO calcularParcelasParaTipo(Simulacao simulacao, Produto produto, TipoAmortizacao tipo) {
        var solicitacaoSimulacao = new SimulacaoCreateDTO();
        solicitacaoSimulacao.setValorDesejado(simulacao.getValorDesejado());
        solicitacaoSimulacao.setPrazo(simulacao.getPrazo().intValue());

        return calculadoraFinanceira.calcularResultado(solicitacaoSimulacao, produto, tipo.getCodigo());
    }

    private ParcelaDTO buscarParcelaEspecificaOuLancarExcecao(ResultadoSimulacaoDTO resultado, Long parcelaId, String requestId) {
        var parcelaOpt = resultado.getParcelas().stream()
            .filter(parcela -> parcela.getNumero().equals(parcelaId))
            .findFirst();

        if (parcelaOpt.isEmpty()) {
            errorHandling.logarInfo(requestId, String.format("Parcela %d não encontrada", parcelaId));
            throw new ParametroInvalidoException(
                MensagemErro.PARAMETROS_INVALIDOS,
                String.format("Parcela %d não encontrada. Total de parcelas disponíveis: %d", parcelaId, resultado.getParcelas().size())
            );
        }

        return parcelaOpt.get();
    }

    // Métodos auxiliares para construção de DTOs usando mappers
    private SimulacaoDetalhesDTO construirSimulacaoDetalhesDTO(Simulacao simulacao) {
        return simulacaoMapper.toSimulacaoDetalhesDTO(simulacao);
    }

    private void preencherInformacoesProdutoEParcelas(SimulacaoDetalhesDTO dto, Simulacao simulacao, Produto produto, String requestId) {
        var solicitacaoSimulacao = new SimulacaoCreateDTO();
        solicitacaoSimulacao.setValorDesejado(simulacao.getValorDesejado());
        solicitacaoSimulacao.setPrazo(simulacao.getPrazo().intValue());

        var resultadosCalculados = calcularResultadosSimulacao(solicitacaoSimulacao, produto);
        simulacaoMapper.enriqueceSimulacaoDetalhesDTO(dto, produto, resultadosCalculados);

        var contadorSAC = resultadosCalculados.stream()
            .filter(r -> "SAC".equals(r.getTipo()))
            .mapToInt(r -> r.getParcelas().size())
            .findFirst().orElse(0);

        var contadorPRICE = resultadosCalculados.stream()
            .filter(r -> "PRICE".equals(r.getTipo()))
            .mapToInt(r -> r.getParcelas().size())
            .findFirst().orElse(0);

        errorHandling.logarInfo(requestId, String.format("Parcelas calculadas para simulação ID: %d (SAC: %d parcelas, PRICE: %d parcelas)",
            simulacao.getId(), contadorSAC, contadorPRICE));
    }

    private ParcelasSimulacaoDTO construirParcelasSimulacaoDTO(Simulacao simulacao, Produto produto, TipoAmortizacao tipo, ResultadoSimulacaoDTO resultado) {
        return simulacaoMapper.toParcelasSimulacaoDTO(simulacao, produto, tipo, resultado);
    }

    private ParcelaEspecificaDTO construirParcelaEspecificaDTO(Simulacao simulacao, Produto produto, TipoAmortizacao tipo,
                                                              ParcelaDTO parcelaEspecifica, ResultadoSimulacaoDTO resultado) {
        return simulacaoMapper.toParcelaEspecificaDTO(simulacao, produto, tipo, parcelaEspecifica, resultado);
    }

    /**
     * Processa filtros comuns para evitar duplicação de código entre métodos de busca.
     */
    private FiltroSimulacaoContext processarFiltrosSimulacao(String dataFiltro, Integer produtoId) {
        var dataConsulta = determinarDataConsulta(dataFiltro);
        var simulacoesFiltradas = aplicarFiltrosSimulacao(dataConsulta, produtoId);
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
                return;
            } catch (Exception e) {
                errorHandling.logarErro(requestId, "simularEmprestimo - Event Hub tentativa " + tentativa, e);

                if (tentativa < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * tentativa);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        errorHandling.logarErro(requestId, "Thread interrompida durante retry", ie);
                        break;
                    }
                }
                tentativa++;
            }
        }

        errorHandling.logarErro(requestId, "simularEmprestimo - Event Hub",
            new Exception("Todas as tentativas de envio ao Event Hub falharam."));
    }

    /**
     * Valida a existência de um produto usando cache para otimizar consultas.
     */
    private void validarExistenciaProdutoComCache(Integer produtoId) {
        if (produtoCache.containsKey(produtoId)) {
            return;
        }

        var produto = produtoRepository.findById(Long.valueOf(produtoId));
        if (produto == null) {
            throw ProdutoException.produtoNaoEncontrado(produtoId);
        }

        produtoCache.put(produtoId, produto);
    }

    private List<Produto> buscarTodosProdutos() {
        var agora = System.currentTimeMillis();
        if (produtosCache == null || (agora - ultimaAtualizacaoProdutos) > CACHE_TIMEOUT) {
            produtosCache = produtoRepository.listAll();
            ultimaAtualizacaoProdutos = agora;
        }
        return produtosCache;
    }

    private List<ResultadoSimulacaoDTO> calcularResultadosSimulacao(SimulacaoCreateDTO simulacao, Produto produto) {
        var resultadoSAC = calculadoraFinanceira.calcularResultado(simulacao, produto, TipoAmortizacao.SAC.getCodigo());
        var resultadoPrice = calculadoraFinanceira.calcularResultado(simulacao, produto, TipoAmortizacao.PRICE.getCodigo());
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
        var novaSimulacao = criarNovaSimulacao(solicitacao, produto, resultadoPrice, valorDesejado);
        simulacaoRepository.persist(novaSimulacao);
        return novaSimulacao;
    }

    private Simulacao criarNovaSimulacao(SimulacaoCreateDTO solicitacao, Produto produto,
                                       ResultadoSimulacaoDTO resultadoPrice, BigDecimal valorDesejado) {
        var simulacao = new Simulacao();
        simulacao.setValorDesejado(valorDesejado);
        simulacao.setPrazo(solicitacao.getPrazo().longValue());
        simulacao.setTaxaMediaJuros(produto.getPcTaxaJuros().setScale(FinanceiroConstant.TAXA_SCALE.getValor(), RoundingMode.HALF_UP));
        simulacao.setValorTotalDesejado(valorDesejado);

        var valorTotalParcelas = calculadoraFinanceira.calcularValorTotalParcelas(resultadoPrice.getParcelas());
        simulacao.setValorTotalCredito(valorTotalParcelas);

        var valorMedioPrestacao = calculadoraFinanceira.calcularValorMedioPrestacao(resultadoPrice.getParcelas());
        simulacao.setValorMedioPrestacao(valorMedioPrestacao);

        simulacao.setProduto(null);
        simulacao.setDataSimulacao(LocalDateTime.now());

        return simulacao;
    }

    private List<Simulacao> buscarSimulacoesPaginadas(int numeroPagina, int quantidadePorPagina) {
        return simulacaoRepository.findAll()
            .page(numeroPagina - 1, quantidadePorPagina)
            .list();
    }

    private LocalDate determinarDataConsulta(String dataFiltro) {
        if (dataFiltro == null || dataFiltro.isBlank()) {
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(dataFiltro);
        } catch (Exception e) {
            throw new ParametroInvalidoException(
                MensagemErro.FORMATO_DATA_INVALIDO,
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
        var todasSimulacoes = simulacaoRepository.listAll();

        var simulacoesPorData = todasSimulacoes.stream()
            .filter(simulacao -> simulacao.getDataSimulacao().toLocalDate().equals(dataConsulta))
            .toList();

        if (produtoId == null) {
            return simulacoesPorData;
        }

        validarExistenciaProdutoComCache(produtoId);
        return filtrarSimulacoesPorProduto(simulacoesPorData, produtoId);
    }

    private List<Simulacao> filtrarSimulacoesPorProduto(List<Simulacao> simulacoes, Integer produtoId) {
        var todosProdutos = buscarTodosProdutos();

        return simulacoes.stream()
            .filter(simulacao -> {
                var produtoAssociado = produtoElegibilidade.encontrarProdutoPorSimulacao(
                    todosProdutos, simulacao.getValorDesejado(), simulacao.getPrazo().intValue()
                );
                return produtoAssociado
                    .map(produto -> produto.getCoProduto().equals(produtoId))
                    .orElse(false);
            })
            .toList();
    }

    /**
     * Agrupa simulações por produto baseado na elegibilidade de cada simulação.
     */
    private Map<Integer, List<Simulacao>> agruparSimulacoesPorProduto(List<Simulacao> simulacoes, List<Produto> todosProdutos) {
        return simulacoes.stream()
            .collect(Collectors.groupingBy(simulacao -> {
                var produtoAssociado = produtoElegibilidade.encontrarProdutoPorSimulacao(
                    todosProdutos, simulacao.getValorDesejado(), simulacao.getPrazo().intValue()
                );
                return produtoAssociado
                    .map(Produto::getCoProduto)
                    .orElse(-1); // Produto não identificado
            }))
            .entrySet().stream()
            .filter(entry -> entry.getKey() != -1)
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
