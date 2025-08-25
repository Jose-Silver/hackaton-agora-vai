package emprestimos.v1.domain.service.strategy;

import emprestimos.v1.domain.enums.FinanceiroConstant;
import emprestimos.v1.domain.dto.simulacao.create.response.ParcelaDTO;
import emprestimos.v1.domain.qualifier.Sac;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação da estratégia de cálculo para o Sistema de Amortização Constante (SAC).
 */
@ApplicationScoped
@Sac
public class SacCalculadoraStrategy implements CalculadoraParcelasStrategy {

    @Override
    public List<ParcelaDTO> calcularParcelas(BigDecimal valorFinanciado, BigDecimal taxaMensal, int prazoMeses) {
        List<ParcelaDTO> parcelas = new ArrayList<>();
        BigDecimal amortizacaoConstante = valorFinanciado.divide(BigDecimal.valueOf(prazoMeses),
                FinanceiroConstant.DECIMAL_SCALE.getValor(), RoundingMode.HALF_UP);
        BigDecimal saldoDevedor = valorFinanciado;

        for (int parcela = 1; parcela <= prazoMeses; parcela++) {
            BigDecimal juros = saldoDevedor.multiply(taxaMensal)
                    .setScale(FinanceiroConstant.DECIMAL_SCALE.getValor(), RoundingMode.HALF_UP);
            BigDecimal valorPrestacao = amortizacaoConstante.add(juros);
            saldoDevedor = saldoDevedor.subtract(amortizacaoConstante);

            parcelas.add(criarParcela(parcela, valorPrestacao, juros, amortizacaoConstante));
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
