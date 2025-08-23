package domain.dto;

import lombok.Data;
import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Schema(description = "Resumo de uma simulação de empréstimo.")
public class SimulacaoResumoDTO {
    private Long idSimulacao;
    private BigDecimal valorDesejado;
    private Integer prazo;
    private BigDecimal valorTotalParcelas;
}
