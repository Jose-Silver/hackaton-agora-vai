package domain.service;

import domain.dto.simulacao.create.response.ParcelaDTO;
import domain.dto.simulacao.create.response.ResultadoSimulacaoDTO;
import domain.dto.simulacao.create.request.SimulacaoCreateDTO;
import domain.entity.remote.Produto;
import domain.enums.TipoAmortizacao;
import domain.enums.FinanceiroConstant;
import domain.exception.SimulacaoException;
import domain.service.strategy.CalculadoraParcelasStrategy;
import domain.qualifier.Price;
import domain.qualifier.Sac;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Serviço responsável pelos cálculos financeiros das simulações.
 */
@ApplicationScoped
public class CalculadoraFinanceiraService {

    @Inject
    @Sac
    private CalculadoraParcelasStrategy sacStrategy;

    @Inject
    @Price
    private CalculadoraParcelasStrategy priceStrategy;

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
            case SAC -> sacStrategy.calcularParcelas(valorFinanciado, taxaMensal, prazoMeses);
            case PRICE -> priceStrategy.calcularParcelas(valorFinanciado, taxaMensal, prazoMeses);
        };

        return criarResultadoSimulacao(tipo.getCodigo(), parcelas);
    }

    /**
     * Calcula a taxa mensal a partir da taxa anual.
     */
    private BigDecimal calcularTaxaMensal(BigDecimal taxaAnual) {
        return taxaAnual.divide(BigDecimal.valueOf(FinanceiroConstant.MESES_POR_ANO.getValor()),
                FinanceiroConstant.TAXA_SCALE.getValor(), RoundingMode.HALF_UP);
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
                .setScale(FinanceiroConstant.DECIMAL_SCALE.getValor(), RoundingMode.HALF_UP);
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
                FinanceiroConstant.DECIMAL_SCALE.getValor(), RoundingMode.HALF_UP);
    }
}
