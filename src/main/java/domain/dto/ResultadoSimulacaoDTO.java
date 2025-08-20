package domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResultadoSimulacaoDTO {
    private String tipo;
    private List<ParcelaDTO> parcelas;
}

