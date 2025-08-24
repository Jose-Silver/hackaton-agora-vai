package domain.service.strategy;

    import domain.enums.FinanceiroConstant;
import domain.dto.simulacao.create.response.ParcelaDTO;
import domain.qualifier.Price;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação da estratégia de cálculo para o Sistema Price (prestações constantes).
 */
@ApplicationScoped
@Price
public class PriceCalculadoraStrategy implements CalculadoraParcelasStrategy {

    @Override
    public List<ParcelaDTO> calcularParcelas(BigDecimal valorFinanciado, BigDecimal taxaMensal, int prazoMeses) {
        List<ParcelaDTO> parcelas = new ArrayList<>();

        // Cálculo do coeficiente de financiamento (Price)
        BigDecimal um = BigDecimal.ONE;
        BigDecimal umMaisTaxa = um.add(taxaMensal);
        BigDecimal coeficiente = umMaisTaxa.pow(prazoMeses);
        BigDecimal prestacaoConstante = valorFinanciado
                .multiply(taxaMensal)
                .multiply(coeficiente)
                .divide(coeficiente.subtract(um), FinanceiroConstant.DECIMAL_SCALE.getValor(), RoundingMode.HALF_UP);

        BigDecimal saldoDevedor = valorFinanciado;

        for (int parcela = 1; parcela <= prazoMeses; parcela++) {
            BigDecimal juros = saldoDevedor.multiply(taxaMensal)
                    .setScale(FinanceiroConstant.DECIMAL_SCALE.getValor(), RoundingMode.HALF_UP);
            BigDecimal amortizacao = prestacaoConstante.subtract(juros);
            saldoDevedor = saldoDevedor.subtract(amortizacao);

            parcelas.add(criarParcela(parcela, prestacaoConstante, juros, amortizacao));
        }

        return parcelas;
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
