package domain.service.strategy;

import domain.constants.FinanceiroConstants;
import domain.dto.ParcelaDTO;
import domain.qualifier.Price;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação da estratégia de cálculo para o Sistema Francês (PRICE).
 */
@ApplicationScoped
@Price
public class PriceCalculadoraStrategy implements CalculadoraParcelasStrategy {

    @Override
    public List<ParcelaDTO> calcularParcelas(BigDecimal valorFinanciado, BigDecimal taxaMensal, int prazoMeses) {
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
}
