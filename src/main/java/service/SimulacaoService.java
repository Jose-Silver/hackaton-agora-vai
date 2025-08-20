package service;

import domain.dto.*;
import domain.entity.Produto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class SimulacaoService {
    @Inject
    ProdutoRepository produtoRepository;

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

        List<ResultadoSimulacaoDTO> resultados = new ArrayList<>();
        resultados.add(calcularResultado(dto, melhorProduto, "SAC"));
        resultados.add(calcularResultado(dto, melhorProduto, "PRICE"));

        SimulacaoResponseDTO response = new SimulacaoResponseDTO();
        response.setIdSimulacao(System.currentTimeMillis()); // mock id
        response.setCodigoProduto(melhorProduto.getCoProduto());
        response.setDescricaoProduto(melhorProduto.getNoProduto());
        response.setTaxaJuros(melhorProduto.getPcTaxaJuros());
        response.setResultadoSimulacao(resultados);
        return response;
    }

    private ResultadoSimulacaoDTO calcularResultado(SimulacaoCreateDTO dto, Produto produto, String tipo) {
        BigDecimal valorDesejado = BigDecimal.valueOf(dto.getValorDesejado());
        int prazo = dto.getPrazo().intValue();
        BigDecimal taxaJuros = produto.getPcTaxaJuros();
        List<ParcelaDTO> parcelas = new ArrayList<>();
        BigDecimal saldoDevedor = valorDesejado;

        if ("SAC".equals(tipo)) {
            BigDecimal amortizacao = valorDesejado.divide(BigDecimal.valueOf(prazo), 2, BigDecimal.ROUND_HALF_UP);
            for (int n = 1; n <= prazo; n++) {
                BigDecimal valorJuros = saldoDevedor.multiply(taxaJuros).setScale(2, BigDecimal.ROUND_HALF_UP);
                BigDecimal valorPrestacao = amortizacao.add(valorJuros).setScale(2, BigDecimal.ROUND_HALF_UP);
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
                    .divide(fator.subtract(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP);
            for (int n = 1; n <= prazo; n++) {
                BigDecimal valorJuros = saldoDevedor.multiply(i).setScale(2, BigDecimal.ROUND_HALF_UP);
                BigDecimal valorAmortizacao = valorPrestacao.subtract(valorJuros).setScale(2, BigDecimal.ROUND_HALF_UP);
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
