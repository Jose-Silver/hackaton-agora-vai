package domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class SimulacaoPorProdutoDiaResponseDTO {
    private String dataReferencia;
    private List<SimulacaoProdutoDiaResumoDTO> simulacoes;
}

