package emprestimos.v1.domain.dto.simulacao.create.response;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Data
@XmlRootElement(name = "parcela")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "Dados de uma parcela do empréstimo")
public class ParcelaDTO {

    @XmlElement(name = "numero")
    @Schema(description = "Número da parcela", example = "1", required = true)
    private Long numero;

    @XmlElement(name = "valorAmortizacao")
    @Schema(description = "Valor da amortização da parcela em reais", example = "833.33", required = true)
    private BigDecimal valorAmortizacao;

    @XmlElement(name = "valorJuros")
    @Schema(description = "Valor dos juros da parcela em reais", example = "166.67", required = true)
    private BigDecimal valorJuros;

    @XmlElement(name = "valorPrestacao")
    @Schema(description = "Valor total da prestação em reais", example = "1000.00", required = true)
    private BigDecimal valorPrestacao;
}
