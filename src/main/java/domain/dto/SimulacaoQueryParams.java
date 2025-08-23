package domain.dto;

import domain.constants.ValidationMessages;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO para parâmetros de consulta de simulações com paginação.
 */
@Getter
@Setter
@Schema(description = "Parâmetros para consulta paginada de simulações")
public class SimulacaoQueryParams {

    /**
     * Número da página para consultas paginadas.
     * Valor padrão: 1
     */
    @QueryParam("pagina")
    @Min(value = 1, message = ValidationMessages.PAGINA_MINIMA)
    @Schema(
        description = "Número da página para consulta paginada (mínimo 1)",
        examples = {"1"},
        defaultValue = "1"
    )
    private Integer pagina = 1;

    /**
     * Quantidade de registros por página para consultas paginadas.
     * Valor padrão: 10
     */
    @QueryParam("qtdRegistrosPagina")
    @Min(value = 1, message = ValidationMessages.QTD_REGISTROS_MINIMA)
    @Max(value = 100, message = ValidationMessages.QTD_REGISTROS_MAXIMA)
    @Schema(
        description = "Quantidade de registros por página (1-100)",
        examples = {"10"},
        defaultValue = "10"
    )
    private Integer qtdRegistrosPagina = 10;
}
