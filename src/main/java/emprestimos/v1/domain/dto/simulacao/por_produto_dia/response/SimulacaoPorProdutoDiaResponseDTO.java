package emprestimos.v1.domain.dto.simulacao.por_produto_dia.response;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Data
@XmlRootElement(name = "simulacaoPorProdutoDiaResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "Resposta com simulações")
public class SimulacaoPorProdutoDiaResponseDTO {

    @XmlElement(name = "dataReferencia")
    @Schema(description = "Data de referência da consulta no formato yyyy-MM-dd", example = "2025-08-23", required = true)
    private String dataReferencia;

    @XmlElementWrapper(name = "simulacoes")
    @XmlElement(name = "simulacao")
    @Schema(description = "Lista de simulacoes", required = true)
    private List<SimulacaoPorProdutoDiaDTO> simulacoes;
}
