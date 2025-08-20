package domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class PaginaSimulacaoDTO {
    private Integer pagina;
    private Long qtdRegistros;
    private Integer qtdRegistrosPagina;
    private List<SimulacaoResumoDTO> registros;
}

