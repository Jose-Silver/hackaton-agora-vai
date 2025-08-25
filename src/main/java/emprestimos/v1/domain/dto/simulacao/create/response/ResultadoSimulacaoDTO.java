package emprestimos.v1.domain.dto.simulacao.create.response;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Data
@XmlRootElement(name = "resultado")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "Resultado da simulação de empréstimo com cálculo das parcelas")
public class ResultadoSimulacaoDTO {

    @XmlElement(name = "tipo")
    @Schema(description = "Tipo de amortização utilizada", example = "SAC", required = true)
    private String tipo;

    @XmlElementWrapper(name = "parcelas")
    @XmlElement(name = "parcela")
    @Schema(description = "Lista de parcelas do empréstimo", required = true)
    private List<ParcelaDTO> parcelas;
}
