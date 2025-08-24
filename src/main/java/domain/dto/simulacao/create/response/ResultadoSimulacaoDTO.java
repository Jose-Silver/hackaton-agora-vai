package domain.dto.simulacao.create.response;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.List;

@Data
@Schema(description = "Resultado da simulação de empréstimo com cálculo das parcelas")
public class ResultadoSimulacaoDTO {

    @Schema(description = "Tipo de amortização utilizada", example = "SAC", required = true)
    private String tipo;

    @Schema(description = "Lista de parcelas do empréstimo", required = true)
    private List<ParcelaDTO> parcelas;
}
