package mapper;

import domain.dto.simulacao.create.response.ParcelaDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Mapper utilitário para cálculos financeiros
 */
public final class FinanceiroMapper {

    private FinanceiroMapper() {

    }

    /**
     * Calcula o saldo devedor após o pagamento de uma parcela específica.
     */
    public static BigDecimal calcularSaldoDevedor(List<ParcelaDTO> parcelas, int numeroParcela) {
        var saldoInicial = parcelas.stream()
            .map(ParcelaDTO::getValorAmortizacao)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        var amortizacaoAcumulada = parcelas.stream()
            .filter(parcela -> parcela.getNumero() <= numeroParcela)
            .map(ParcelaDTO::getValorAmortizacao)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return saldoInicial.subtract(amortizacaoAcumulada);
    }

    /**
     * Calcula o percentual de um valor sobre outro.
     */
    public static BigDecimal calcularPercentual(BigDecimal valor, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return valor.divide(total, 4, RoundingMode.HALF_UP)
                   .multiply(BigDecimal.valueOf(100));
    }
}
