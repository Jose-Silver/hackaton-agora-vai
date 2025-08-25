package emprestimos.v1.domain.dto.simulacao.create.response;

import emprestimos.v1.domain.dto.simulacao.list.response.SimulacaoResumoSimplificadoDTO;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Data
@XmlRootElement(name = "paginaSimulacao")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "DTO de resposta paginada de simulações simplificada")
public class PaginaSimulacaoSimplificadaDTO {

    @XmlElement(name = "pagina")
    @Schema(description = "Número da página atual", example = "1", required = true)
    private Integer pagina;

    @XmlElement(name = "qtdRegistros")
    @Schema(description = "Quantidade total de registros encontrados", example = "150", required = true)
    private Long qtdRegistros;

    @XmlElement(name = "qtdRegistrosPagina")
    @Schema(description = "Quantidade de registros por página", example = "10", required = true)
    private Integer qtdRegistrosPagina;

    @XmlElementWrapper(name = "registros")
    @XmlElement(name = "simulacao")
    @Schema(description = "Lista de simulações simplificadas da página atual", required = true)
    private List<SimulacaoResumoSimplificadoDTO> registros;
}
