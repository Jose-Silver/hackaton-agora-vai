package domain.dto;

import domain.constants.ValidationMessages;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO para parâmetros de consulta do endpoint /por-produto-dia (sem paginação).
 */
@Getter
@Setter
@Schema(description = "Parâmetros para filtrar simulações por produto e data")
public class SimulacaoPorProdutoDiaQueryParams {
    /**
     * Data para filtrar simulações no formato yyyy-MM-dd.
     * Opcional.
     */
    @QueryParam("data")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = ValidationMessages.DATA_INVALIDA)
    @Schema(
        description = "Data para filtrar simulações (formato: yyyy-MM-dd). Se não informada, utiliza a data atual",
        examples = {"2025-08-21"}
    )
    private String data;

    /**
     * ID do produto para filtrar simulações. Opcional.
     */
    @QueryParam("produtoId")
    @Positive(message = ValidationMessages.PRODUTO_ID_POSITIVO)
    @Schema(
        description = "ID do produto para filtrar simulações. Se não informado, retorna todos os produtos",
        examples = {"123"}
    )
    private Integer produtoId;
}
