package emprestimos.v1.domain.dto.simulacao.list.response;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import lombok.Data;
import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@XmlRootElement(name = "simulacaoResumo")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "Resumo simplificado de uma simulação de empréstimo")
public class SimulacaoResumoSimplificadoDTO {

    @XmlElement(name = "idSimulacao")
    @Schema(description = "Identificador único da simulação", example = "12345")
    private Long idSimulacao;

    @XmlElement(name = "valorDesejado")
    @Schema(description = "Valor desejado para o empréstimo em reais", example = "25000.00")
    private BigDecimal valorDesejado;

    @XmlElement(name = "prazo")
    @Schema(description = "Prazo do empréstimo em meses", example = "36")
    private Integer prazo;

    @XmlElement(name = "valorTotalParcelas")
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
