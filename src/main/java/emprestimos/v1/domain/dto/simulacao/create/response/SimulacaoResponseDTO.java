package emprestimos.v1.domain.dto.simulacao.create.response;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@XmlRootElement(name = "simulacao")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "DTO de resposta para simulação de empréstimo.")
public class SimulacaoResponseDTO {
    @XmlElement(name = "idSimulacao")
    @Schema(description = "ID da simulação gerada.", example = "123")
    private Long idSimulacao;
    @XmlElement(name = "codigoProduto")
    @Schema(description = "Código do produto.", example = "456")
    private Integer codigoProduto;
    @XmlElement(name = "descricaoProduto")
    @Schema(description = "Descrição do produto.", example = "Crédito Imobiliário")
    private String descricaoProduto;
    @XmlElement(name = "taxaJuros")
    @Schema(description = "Taxa de juros aplicada.", example = "0.08")
    private BigDecimal taxaJuros;
    @XmlElementWrapper(name = "resultadoSimulacao")
    @XmlElement(name = "resultado")
    @Schema(description = "Lista de resultados da simulação.")
    private List<ResultadoSimulacaoDTO> resultadoSimulacao;
    @XmlElement(name = "Links")
    @Schema(description = "Links Hypermidia")
    public Map<String, String> links = new HashMap<>();

    public void addLink(String rel, String href) {
        this.links.put(rel, href);
    }


}
