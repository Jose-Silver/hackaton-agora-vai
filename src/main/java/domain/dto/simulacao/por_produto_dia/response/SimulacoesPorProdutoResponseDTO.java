package domain.dto.simulacao.por_produto_dia.response;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Data
@Schema(description = "DTO de resposta para simulações agrupadas por produto em uma data específica.")
public class SimulacoesPorProdutoResponseDTO {
    @Schema(description = "Data de referência das simulações.", example = "2025-08-21")
    private String dataReferencia;
    @Schema(description = "Lista de produtos com suas simulações.")
    private List<SimulacoesDeUmProdutoDTO> produtos;
}
