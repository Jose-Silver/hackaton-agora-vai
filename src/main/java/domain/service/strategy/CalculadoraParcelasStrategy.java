package domain.service.strategy;

import domain.dto.simulacao.create.response.ParcelaDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface para estratégias de cálculo de parcelas de empréstimo.
 */
public interface CalculadoraParcelasStrategy {

    /**
     * Calcula as parcelas do empréstimo baseado na estratégia implementada.
     *
     * @param valorFinanciado Valor total financiado
     * @param taxaMensal Taxa de juros mensal
     * @param prazoMeses Prazo do empréstimo em meses
     * @return Lista de parcelas calculadas
     */
    List<ParcelaDTO> calcularParcelas(BigDecimal valorFinanciado, BigDecimal taxaMensal, int prazoMeses);
}
