package service;

import domain.dto.*;
import domain.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import repository.ProdutoRepository;
import repository.SimulacaoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SimulacaoService {
    @Inject
    ProdutoRepository produtoRepository;
    @Inject
    SimulacaoRepository simulacaoRepository;

    public SimulacaoResponseDTO simularEmprestimo(SimulacaoCreateDTO dto) {
        List<Produto> produtos = produtoRepository.listAll();
        BigDecimal valorDesejado = BigDecimal.valueOf(dto.getValorDesejado());
        int prazo = dto.getPrazo().intValue();

        // Filtra produtos elegíveis
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
        // Critério: menor taxa de juros
        Produto melhorProduto = elegiveis.stream()
                .min(Comparator.comparing(Produto::getPcTaxaJuros))
                .orElseThrow();

        // Gerar resultados de simulação (SAC e PRICE)
        List<ResultadoSimulacaoDTO> resultados = new ArrayList<>();
        ResultadoSimulacaoDTO sac = calcularResultado(dto, melhorProduto, "SAC");
        ResultadoSimulacaoDTO price = calcularResultado(dto, melhorProduto, "PRICE");
        resultados.add(sac);
        resultados.add(price);

        // Persistir simulação
        Simulacao simulacao = new Simulacao();
        simulacao.setValorDesejado(valorDesejado);
        simulacao.setPrazo(dto.getPrazo());
        simulacao.setTaxaMediaJuros(melhorProduto.getPcTaxaJuros().setScale(4, RoundingMode.HALF_UP));
        simulacao.setValorTotalDesejado(valorDesejado);
        // Valor total das parcelas (PRICE)
        BigDecimal valorTotalParcelas = price.getParcelas().stream().map(ParcelaDTO::getValorPrestacao).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        simulacao.setValorTotalCredito(valorTotalParcelas);
        simulacao.setValorMedioPrestacao(
            price.getParcelas().stream().map(ParcelaDTO::getValorPrestacao).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(price.getParcelas().size()), 2, RoundingMode.HALF_UP)
        );
        simulacao.setProduto(null);
        simulacao.setDataSimulacao(java.time.LocalDateTime.now());
        simulacaoRepository.persist(simulacao);

        // Montar resposta
        SimulacaoResponseDTO response = new SimulacaoResponseDTO();
        response.setIdSimulacao(simulacao.getId());
        response.setCodigoProduto(melhorProduto.getCoProduto());
        response.setDescricaoProduto(melhorProduto.getNoProduto());
        response.setTaxaJuros(melhorProduto.getPcTaxaJuros().setScale(4));
        response.setResultadoSimulacao(resultados);
        return response;
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

    public List<SimulacaoPorProdutoDiaDTO> listarSimulacoesPorProdutoDia() {
        List<Simulacao> simulacoes = simulacaoRepository.listAll();
        List<SimulacaoPorProdutoDiaDTO> dtos = new ArrayList<>();
        for (Simulacao sim : simulacoes) {
            // Buscar o melhor produto novamente para preencher os dados do produto
            List<Produto> produtos = produtoRepository.listAll();
            Produto produto = produtos.stream()
                .filter(p -> p.getVrMinimo().compareTo(sim.getValorDesejado()) <= 0 &&
                             p.getVrMaximo().compareTo(sim.getValorDesejado()) >= 0 &&
                             p.getNuMinimoMeses() <= sim.getPrazo() &&
                             p.getNuMaximoMeses() >= sim.getPrazo())
                .min(Comparator.comparing(Produto::getPcTaxaJuros))
                .orElse(null);
            SimulacaoPorProdutoDiaDTO dto = new SimulacaoPorProdutoDiaDTO();
            if (produto != null) {
                dto.setCodigoProduto(produto.getCoProduto());
                dto.setDescricaoProduto(produto.getNoProduto());
            }
            dto.setDataSimulacao(sim.getDataSimulacao().toLocalDate());
            dto.setValorDesejado(sim.getValorDesejado());
            dto.setPrazo(sim.getPrazo());
            dto.setValorTotalParcelas(sim.getValorTotalCredito());
            dtos.add(dto);
        }
        return dtos;
    }

    public SimulacaoPorProdutoDiaResponseDTO buscarSimulacoesPorProdutoEData(String data, Integer produtoId) {
        List<Simulacao> simulacoes = simulacaoRepository.listAll();
        Produto produto = produtoRepository.findById(Long.valueOf(produtoId));
        if (produto == null) {
            throw new IllegalArgumentException("Produto não encontrado para o id informado.");
        }
        java.time.LocalDate dataFiltro = java.time.LocalDate.parse(data);
        List<SimulacaoProdutoDiaResumoDTO> simulacoesFiltradas = new ArrayList<>();
        for (Simulacao sim : simulacoes) {
            // Verifica se a data bate
            if (!sim.getDataSimulacao().toLocalDate().equals(dataFiltro)) continue;
            // Verifica se o produto selecionado para a simulação é o produtoId
            List<Produto> produtos = produtoRepository.listAll();
            Produto produtoSimulacao = produtos.stream()
                .filter(p -> p.getVrMinimo().compareTo(sim.getValorDesejado()) <= 0 &&
                             p.getVrMaximo().compareTo(sim.getValorDesejado()) >= 0 &&
                             p.getNuMinimoMeses() <= sim.getPrazo() &&
                             p.getNuMaximoMeses() >= sim.getPrazo())
                .min(Comparator.comparing(Produto::getPcTaxaJuros))
                .orElse(null);
            if (produtoSimulacao == null || !produtoSimulacao.getCoProduto().equals(produtoId)) continue;
            SimulacaoProdutoDiaResumoDTO dto = new SimulacaoProdutoDiaResumoDTO();
            dto.setCodigoProduto(produtoSimulacao.getCoProduto());
            dto.setDescricaoProduto(produtoSimulacao.getNoProduto());
            dto.setTaxaMediaJuro(sim.getTaxaMediaJuros());
            dto.setValorMedioPrestacao(sim.getValorMedioPrestacao());
            dto.setValorTotalDesejado(sim.getValorTotalDesejado());
            dto.setValorTotalCredito(sim.getValorTotalCredito());
            simulacoesFiltradas.add(dto);
        }
        SimulacaoPorProdutoDiaResponseDTO resposta = new SimulacaoPorProdutoDiaResponseDTO();
        resposta.setDataReferencia(data);
        resposta.setSimulacoes(simulacoesFiltradas);
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
            BigDecimal i = taxaJuros;
            BigDecimal umMaisI = BigDecimal.ONE.add(i);
            BigDecimal fator = umMaisI.pow(prazo);
            BigDecimal valorPrestacao = valorDesejado.multiply(i).multiply(fator)
                    .divide(fator.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
            for (int n = 1; n <= prazo; n++) {
                BigDecimal valorJuros = saldoDevedor.multiply(i).setScale(2, RoundingMode.HALF_UP);
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
