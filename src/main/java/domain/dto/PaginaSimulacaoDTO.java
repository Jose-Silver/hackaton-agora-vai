package domain.dto;

import lombok.Data;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Schema(description = "DTO de resposta paginada de simulações.")
public class PaginaSimulacaoDTO {
    private Integer pagina;
    private Long qtdRegistros;
    private Integer qtdRegistrosPagina;
    private List<SimulacaoResumoDTO> registros;
}
