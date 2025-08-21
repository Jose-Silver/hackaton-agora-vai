package service;

import domain.dto.*;
import domain.entity.remote.Produto;
import domain.entity.local.Simulacao;
import io.quarkus.agroal.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import repository.ProdutoRepository;
import repository.SimulacaoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SimulacaoService {
    @Inject
    @DataSource("mssql")
    ProdutoRepository produtoRepository;
    @Inject
    @DataSource("h2")
    SimulacaoRepository simulacaoRepository;

    public SimulacaoResponseDTO simularEmprestimo(SimulacaoCreateDTO dto) {
        // Fetch MSSQL data outside transaction
        List<Produto> produtos = produtoRepository.listAll();
        BigDecimal valorDesejado = BigDecimal.valueOf(dto.getValorDesejado());
        int prazo = dto.getPrazo().intValue();
        List<Produto> elegiveis = new ArrayList<>();
        for (Produto p : produtos) {
            if (p.getVrMinimo().compareTo(valorDesejado) <= 0 &&
                p.getVrMaximo().compareTo(valorDesejado) >= 0 &&
                p.getNuMinimoMeses() <= prazo &&
                p.getNuMaximoMeses() >= prazo) {
                elegiveis.add(p);
            }
        }
        if (elegiveis.isEmpty()) {
            throw new IllegalArgumentException("Nenhum produto elegível para o valor e prazo informados.");
        }
        Produto melhorProduto = elegiveis.stream()
                .min(Comparator.comparing(Produto::getPcTaxaJuros))
                .orElseThrow();
        List<ResultadoSimulacaoDTO> resultados = new ArrayList<>();
        ResultadoSimulacaoDTO sac = calcularResultado(dto, melhorProduto, "SAC");
        ResultadoSimulacaoDTO price = calcularResultado(dto, melhorProduto, "PRICE");
        resultados.add(sac);
        resultados.add(price);

        // Persist only in H2 inside transaction
        Simulacao simulacao = persistSimulacao(dto, melhorProduto, price, valorDesejado);

        SimulacaoResponseDTO response = new SimulacaoResponseDTO();
        response.setIdSimulacao(simulacao.getId());
        response.setCodigoProduto(melhorProduto.getCoProduto());
        response.setDescricaoProduto(melhorProduto.getNoProduto());
        response.setTaxaJuros(melhorProduto.getPcTaxaJuros().setScale(4, RoundingMode.HALF_UP));
        response.setResultadoSimulacao(resultados);
        return response;
    }

    @Transactional
    protected Simulacao persistSimulacao(SimulacaoCreateDTO dto, Produto melhorProduto, ResultadoSimulacaoDTO price, BigDecimal valorDesejado) {
        Simulacao simulacao = new Simulacao();
        simulacao.setValorDesejado(valorDesejado);
        simulacao.setPrazo(dto.getPrazo());
        simulacao.setTaxaMediaJuros(melhorProduto.getPcTaxaJuros().setScale(4, RoundingMode.HALF_UP));
        simulacao.setValorTotalDesejado(valorDesejado);
        BigDecimal valorTotalParcelas = price.getParcelas().stream().map(ParcelaDTO::getValorPrestacao).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        simulacao.setValorTotalCredito(valorTotalParcelas);
        simulacao.setValorMedioPrestacao(
            price.getParcelas().stream().map(ParcelaDTO::getValorPrestacao).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(price.getParcelas().size()), 2, RoundingMode.HALF_UP)
        );
        simulacao.setProduto(null);
        simulacao.setDataSimulacao(java.time.LocalDateTime.now());
        simulacaoRepository.persist(simulacao);
        return simulacao;
    }

    public PaginaSimulacaoDTO listarSimulacoes(int pagina, int qtdPorPagina) {
        long total = simulacaoRepository.count();
        List<Simulacao> lista = simulacaoRepository.findAll()
                .page(pagina - 1, qtdPorPagina)
                .list();
        List<SimulacaoResumoDTO> registros = lista.stream().map(sim -> {
            SimulacaoResumoDTO dto = new SimulacaoResumoDTO();
            dto.setIdSimulacao(sim.getId());
            dto.setValorDesejado(sim.getValorDesejado());
            dto.setPrazo(sim.getPrazo().intValue());
            dto.setValorTotalParcelas(sim.getValorTotalCredito());
            return dto;
        }).collect(Collectors.toList());
        PaginaSimulacaoDTO paginaDTO = new PaginaSimulacaoDTO();
        paginaDTO.setPagina(pagina);
        paginaDTO.setQtdRegistros(total);
        paginaDTO.setQtdRegistrosPagina(qtdPorPagina);
        paginaDTO.setRegistros(registros);
        return paginaDTO;
    }


    public SimulacaoPorProdutoDiaResponseDTO buscarSimulacoesPorProdutoEData(String data, Integer produtoId) {
        // Determine the date to filter
        java.time.LocalDate dataFiltro = (data != null && !data.isBlank())
                ? java.time.LocalDate.parse(data)
                : java.time.LocalDate.now();

        // Fetch all simulations
        List<Simulacao> simulacoes = simulacaoRepository.listAll();

        // Filter by date
        List<Simulacao> simulacoesPorData = simulacoes.stream()
                .filter(sim -> sim.getDataSimulacao().toLocalDate().equals(dataFiltro))
                .toList();

        // Filter by product if provided
        List<Simulacao> simulacoesFiltradas;
        if (produtoId != null) {
            // Find the product in MSSQL
            Produto produto = produtoRepository.findById(Long.valueOf(produtoId));
            if (produto == null) {
                throw new IllegalArgumentException("Produto não encontrado para o id informado.");
            }
            // Filter simulations by productId (using the same logic as before)
            simulacoesFiltradas = simulacoesPorData.stream()
                .filter(sim -> {
                    List<Produto> produtos = produtoRepository.listAll();
                    Produto produtoSimulacao = produtos.stream()
                        .filter(p -> p.getVrMinimo().compareTo(sim.getValorDesejado()) <= 0 &&
                                     p.getVrMaximo().compareTo(sim.getValorDesejado()) >= 0 &&
                                     p.getNuMinimoMeses() <= sim.getPrazo() &&
                                     p.getNuMaximoMeses() >= sim.getPrazo())
                        .min(Comparator.comparing(Produto::getPcTaxaJuros))
                        .orElse(null);
                    return produtoSimulacao != null && produtoSimulacao.getCoProduto().equals(produtoId);
                })
                .toList();
        } else {
            simulacoesFiltradas = simulacoesPorData;
        }

        // Group by product
        List<Produto> produtos = produtoRepository.listAll();
        List<SimulacaoProdutoDiaResumoDTO> simulacoesSegmentadas = new ArrayList<>();
        if (produtoId != null) {
            // Only one product
            Produto produto = produtoRepository.findById(Long.valueOf(produtoId));
            if (produto != null) {
                for (Simulacao sim : simulacoesFiltradas) {
                    Produto produtoSimulacao = produtos.stream()
                        .filter(p -> p.getVrMinimo().compareTo(sim.getValorDesejado()) <= 0 &&
                                     p.getVrMaximo().compareTo(sim.getValorDesejado()) >= 0 &&
                                     p.getNuMinimoMeses() <= sim.getPrazo() &&
                                     p.getNuMaximoMeses() >= sim.getPrazo())
                        .min(Comparator.comparing(Produto::getPcTaxaJuros))
                        .orElse(null);
                    if (produtoSimulacao != null && produtoSimulacao.getCoProduto().equals(produtoId)) {
                        SimulacaoProdutoDiaResumoDTO dto = new SimulacaoProdutoDiaResumoDTO();
                        dto.setCodigoProduto(produtoSimulacao.getCoProduto());
                        dto.setDescricaoProduto(produtoSimulacao.getNoProduto());
                        dto.setTaxaMediaJuro(sim.getTaxaMediaJuros());
                        dto.setValorMedioPrestacao(sim.getValorMedioPrestacao());
                        dto.setValorTotalDesejado(sim.getValorTotalDesejado());
                        dto.setValorTotalCredito(sim.getValorTotalCredito());
                        simulacoesSegmentadas.add(dto);
                    }
                }
            }
        } else {
            // Group by product
            for (Produto produto : produtos) {
                List<Simulacao> simsDoProduto = simulacoesFiltradas.stream()
                    .filter(sim -> {
                        Produto produtoSimulacao = produtos.stream()
                            .filter(p -> p.getVrMinimo().compareTo(sim.getValorDesejado()) <= 0 &&
                                         p.getVrMaximo().compareTo(sim.getValorDesejado()) >= 0 &&
                                         p.getNuMinimoMeses() <= sim.getPrazo() &&
                                         p.getNuMaximoMeses() >= sim.getPrazo())
                            .min(Comparator.comparing(Produto::getPcTaxaJuros))
                            .orElse(null);
                        return produtoSimulacao != null && produtoSimulacao.getCoProduto().equals(produto.getCoProduto());
                    })
                    .toList();
                if (!simsDoProduto.isEmpty()) {
                    for (Simulacao sim : simsDoProduto) {
                        SimulacaoProdutoDiaResumoDTO dto = new SimulacaoProdutoDiaResumoDTO();
                        dto.setCodigoProduto(produto.getCoProduto());
                        dto.setDescricaoProduto(produto.getNoProduto());
                        dto.setTaxaMediaJuro(sim.getTaxaMediaJuros());
                        dto.setValorMedioPrestacao(sim.getValorMedioPrestacao());
                        dto.setValorTotalDesejado(sim.getValorTotalDesejado());
                        dto.setValorTotalCredito(sim.getValorTotalCredito());
                        simulacoesSegmentadas.add(dto);
                    }
                }
            }
        }

        SimulacaoPorProdutoDiaResponseDTO resposta = new SimulacaoPorProdutoDiaResponseDTO();
        resposta.setDataReferencia(dataFiltro.toString());
        resposta.setSimulacoes(simulacoesSegmentadas);
        return resposta;
    }

    private ResultadoSimulacaoDTO calcularResultado(SimulacaoCreateDTO dto, Produto produto, String tipo) {
        BigDecimal valorDesejado = BigDecimal.valueOf(dto.getValorDesejado());
        int prazo = dto.getPrazo().intValue();
        BigDecimal taxaJuros = produto.getPcTaxaJuros();
        List<ParcelaDTO> parcelas = new ArrayList<>();
        BigDecimal saldoDevedor = valorDesejado;

        if ("SAC".equals(tipo)) {
            BigDecimal amortizacao = valorDesejado.divide(BigDecimal.valueOf(prazo), 2, RoundingMode.HALF_UP);
            for (int n = 1; n <= prazo; n++) {
                BigDecimal valorJuros = saldoDevedor.multiply(taxaJuros).setScale(2, RoundingMode.HALF_UP);
                BigDecimal valorPrestacao = amortizacao.add(valorJuros).setScale(2, RoundingMode.HALF_UP);
                ParcelaDTO parcela = new ParcelaDTO();
                parcela.setNumero((long) n);
                parcela.setValorAmortizacao(amortizacao);
                parcela.setValorJuros(valorJuros);
                parcela.setValorPrestacao(valorPrestacao);
                parcelas.add(parcela);
                saldoDevedor = saldoDevedor.subtract(amortizacao);
            }
        } else if ("PRICE".equals(tipo)) {
            BigDecimal umMaisI = BigDecimal.ONE.add(taxaJuros);
            BigDecimal fator = umMaisI.pow(prazo);
            BigDecimal valorPrestacao = valorDesejado.multiply(taxaJuros).multiply(fator)
                    .divide(fator.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
            for (int n = 1; n <= prazo; n++) {
                BigDecimal valorJuros = saldoDevedor.multiply(taxaJuros).setScale(2, RoundingMode.HALF_UP);
                BigDecimal valorAmortizacao = valorPrestacao.subtract(valorJuros).setScale(2, RoundingMode.HALF_UP);
                ParcelaDTO parcela = new ParcelaDTO();
                parcela.setNumero((long) n);
                parcela.setValorAmortizacao(valorAmortizacao);
                parcela.setValorJuros(valorJuros);
                parcela.setValorPrestacao(valorPrestacao);
                parcelas.add(parcela);
                saldoDevedor = saldoDevedor.subtract(valorAmortizacao);
            }
        }
        ResultadoSimulacaoDTO resultado = new ResultadoSimulacaoDTO();
        resultado.setTipo(tipo);
        resultado.setParcelas(parcelas);
        return resultado;
    }
}
