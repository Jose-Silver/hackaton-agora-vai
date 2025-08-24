package domain.dto.simulacao.create.response;

import domain.dto.simulacao.list.response.SimulacaoResumoSimplificadoDTO;
import lombok.Data;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Schema(description = "DTO de resposta paginada de simulações simplificada")
public class PaginaSimulacaoSimplificadaDTO {

    @Schema(description = "Número da página atual", example = "1", required = true)
    private Integer pagina;

    @Schema(description = "Quantidade total de registros encontrados", example = "150", required = true)
    private Long qtdRegistros;

    @Schema(description = "Quantidade de registros por página", example = "10", required = true)
    private Integer qtdRegistrosPagina;

    @Schema(description = "Lista de simulações simplificadas da página atual", required = true)
    private List<SimulacaoResumoSimplificadoDTO> registros;
}
