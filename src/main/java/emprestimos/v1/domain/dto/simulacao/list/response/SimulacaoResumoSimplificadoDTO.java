package emprestimos.v1.domain.dto.simulacao.list.response;

import lombok.Data;
import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Schema(description = "Resumo simplificado de uma simulação de empréstimo")
public class SimulacaoResumoSimplificadoDTO {

    @Schema(description = "Identificador único da simulação", example = "12345")
    private Long idSimulacao;

    @Schema(description = "Valor desejado para o empréstimo em reais", example = "25000.00")
    private BigDecimal valorDesejado;

    @Schema(description = "Prazo do empréstimo em meses", example = "36")
    private Integer prazo;

    @Schema(description = "Valor total a ser pago incluindo juros", example = "30500.50", required = true)
    private BigDecimal valorTotalParcelas;

    // Constructor for easy conversion from full DTO
    public SimulacaoResumoSimplificadoDTO(Long idSimulacao, BigDecimal valorDesejado, 
                                         Integer prazo, BigDecimal valorTotalParcelas) {
        this.idSimulacao = idSimulacao;
        this.valorDesejado = valorDesejado;
        this.prazo = prazo;
        this.valorTotalParcelas = valorTotalParcelas;
    }

    public SimulacaoResumoSimplificadoDTO() {
        // Default constructor for framework usage
    }
}
