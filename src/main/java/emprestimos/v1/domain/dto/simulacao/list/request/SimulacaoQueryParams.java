package emprestimos.v1.domain.dto.simulacao.list.request;

import emprestimos.v1.domain.dto.common.BaseQueryParams;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.QueryParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO para parâmetros de consulta de simulações com paginação.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Parâmetros para consulta paginada de simulações")
public class SimulacaoQueryParams extends BaseQueryParams {

    /**
     * Número da página para consultas paginadas.
     * Valor padrão: 1
     */
    @QueryParam("pagina")
    @Min(value = 1, message = "Página deve ser maior ou igual a 1")
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
    @Min(value = 1, message = "Quantidade de registros deve ser maior que zero")
    @Max(value = 100, message = "Quantidade máxima de registros é 100")
    @Schema(
        description = "Quantidade de registros por página (1-100)",
        examples = {"10"},
        defaultValue = "10"
    )
    private Integer qtdRegistrosPagina = 10;
}
