package domain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SimulacaoResumoDTO {
    private Long idSimulacao;
    private BigDecimal valorDesejado;
    private Integer prazo;
    private BigDecimal valorTotalParcelas;
}

