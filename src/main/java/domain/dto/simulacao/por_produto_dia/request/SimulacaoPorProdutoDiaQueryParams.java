package domain.dto;

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
    @QueryParam("dataSimulacao")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Data inválida: deve estar no formato YYYY-MM-DD")
    @Schema(
        description = "Data para filtrar simulações (formato: yyyy-MM-dd). Se não informada, utiliza a data atual",
        examples = {"2025-08-21"},
        pattern = "\\d{4}-\\d{2}-\\d{2}"
    )
    private String data;

    /**
     * ID do produto para filtrar simulações. Opcional.
     */
    @QueryParam("produtoId")
    @Positive(message = "produtoId deve ser um número positivo")
    @Schema(
        description = "ID do produto para filtrar simulações. Se não informado, retorna todos os produtos",
        examples = {"123"},
        minimum = "1"
    )
    private Integer produtoId;
}
