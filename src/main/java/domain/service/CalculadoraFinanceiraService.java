package domain.service;

import domain.constants.FinanceiroConstants;
import domain.dto.ParcelaDTO;
import domain.dto.ResultadoSimulacaoDTO;
import domain.dto.SimulacaoCreateDTO;
import domain.entity.remote.Produto;
import domain.enums.TipoAmortizacao;
import domain.exception.SimulacaoException;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável pelos cálculos financeiros das simulações.
 */
@ApplicationScoped
public class CalculadoraFinanceiraService {

    /**
     * Calcula o resultado da simulação para um tipo específico de amortização.
     */
    public ResultadoSimulacaoDTO calcularResultado(SimulacaoCreateDTO simulacao, Produto produto, String tipoAmortizacao) {
        BigDecimal valorFinanciado = simulacao.getValorDesejado();
        int prazoMeses = simulacao.getPrazo();
        BigDecimal taxaMensal = calcularTaxaMensal(produto.getPcTaxaJuros());

        TipoAmortizacao tipo;
        try {
            tipo = TipoAmortizacao.porCodigo(tipoAmortizacao);
        } catch (IllegalArgumentException e) {
            throw SimulacaoException.tipoAmortizacaoInvalido(tipoAmortizacao);
        }

        List<ParcelaDTO> parcelas = switch (tipo) {
            case SAC -> calcularParcelasSAC(valorFinanciado, taxaMensal, prazoMeses);
            case PRICE -> calcularParcelasPrice(valorFinanciado, taxaMensal, prazoMeses);
        };

        return criarResultadoSimulacao(tipo.getCodigo(), parcelas);
    }

    /**
     * Calcula a taxa mensal a partir da taxa anual.
     */
    private BigDecimal calcularTaxaMensal(BigDecimal taxaAnual) {
        return taxaAnual.divide(BigDecimal.valueOf(FinanceiroConstants.MESES_POR_ANO),
                FinanceiroConstants.TAXA_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calcula parcelas usando o sistema SAC (Sistema de Amortização Constante).
     */
    private List<ParcelaDTO> calcularParcelasSAC(BigDecimal valorFinanciado, BigDecimal taxaMensal, int prazoMeses) {
        List<ParcelaDTO> parcelas = new ArrayList<>();
        BigDecimal amortizacaoConstante = valorFinanciado.divide(BigDecimal.valueOf(prazoMeses),
                FinanceiroConstants.DECIMAL_SCALE, RoundingMode.HALF_UP);
        BigDecimal saldoDevedor = valorFinanciado;

        for (int parcela = 1; parcela <= prazoMeses; parcela++) {
            BigDecimal juros = saldoDevedor.multiply(taxaMensal)
                    .setScale(FinanceiroConstants.DECIMAL_SCALE, RoundingMode.HALF_UP);
            BigDecimal valorPrestacao = amortizacaoConstante.add(juros);
            saldoDevedor = saldoDevedor.subtract(amortizacaoConstante);

            parcelas.add(criarParcela(parcela, valorPrestacao, juros, amortizacaoConstante));
        }

        return parcelas;
    }

    /**
     * Calcula parcelas usando o sistema PRICE (Sistema Francês).
     */
    private List<ParcelaDTO> calcularParcelasPrice(BigDecimal valorFinanciado, BigDecimal taxaMensal, int prazoMeses) {
        List<ParcelaDTO> parcelas = new ArrayList<>();
        BigDecimal prestacaoFixa = calcularPrestacaoFixaPrice(valorFinanciado, taxaMensal, prazoMeses);
        BigDecimal saldoDevedor = valorFinanciado;

        for (int parcela = 1; parcela <= prazoMeses; parcela++) {
            BigDecimal juros = saldoDevedor.multiply(taxaMensal)
                    .setScale(FinanceiroConstants.DECIMAL_SCALE, RoundingMode.HALF_UP);
            BigDecimal amortizacao = prestacaoFixa.subtract(juros);
            saldoDevedor = saldoDevedor.subtract(amortizacao);

            parcelas.add(criarParcela(parcela, prestacaoFixa, juros, amortizacao));
        }

        return parcelas;
    }

    /**
     * Calcula a prestação fixa para o sistema PRICE.
     */
    private BigDecimal calcularPrestacaoFixaPrice(BigDecimal valorFinanciado, BigDecimal taxaMensal, int prazoMeses) {
        if (taxaMensal.compareTo(BigDecimal.ZERO) == 0) {
            return valorFinanciado.divide(BigDecimal.valueOf(prazoMeses),
                    FinanceiroConstants.DECIMAL_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);
        BigDecimal fatorDesconto = umMaisTaxa.pow(prazoMeses);
        BigDecimal numerador = valorFinanciado.multiply(taxaMensal).multiply(fatorDesconto);
        BigDecimal denominador = fatorDesconto.subtract(BigDecimal.ONE);

        return numerador.divide(denominador, FinanceiroConstants.DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Cria um objeto ParcelaDTO com os valores calculados.
     */
    private ParcelaDTO criarParcela(int numeroParcela, BigDecimal valorPrestacao, BigDecimal juros, BigDecimal amortizacao) {
        ParcelaDTO parcela = new ParcelaDTO();
        parcela.setNumero((long) numeroParcela);
        parcela.setValorPrestacao(valorPrestacao);
        parcela.setValorJuros(juros);
        parcela.setValorAmortizacao(amortizacao);
        return parcela;
    }

    /**
     * Cria o resultado da simulação com as parcelas calculadas.
     */
    private ResultadoSimulacaoDTO criarResultadoSimulacao(String tipoAmortizacao, List<ParcelaDTO> parcelas) {
        ResultadoSimulacaoDTO resultado = new ResultadoSimulacaoDTO();
        resultado.setTipo(tipoAmortizacao);
        resultado.setParcelas(parcelas);
        return resultado;
    }

    /**
     * Calcula o valor total das parcelas.
     */
    public BigDecimal calcularValorTotalParcelas(List<ParcelaDTO> parcelas) {
        return parcelas.stream()
                .map(ParcelaDTO::getValorPrestacao)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(FinanceiroConstants.DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o valor médio das prestações.
     */
    public BigDecimal calcularValorMedioPrestacao(List<ParcelaDTO> parcelas) {
        if (parcelas.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal valorTotal = calcularValorTotalParcelas(parcelas);
        return valorTotal.divide(BigDecimal.valueOf(parcelas.size()),
                FinanceiroConstants.DECIMAL_SCALE, RoundingMode.HALF_UP);
    }
}
