package domain.dto.simulacao.por_produto_dia.response;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Data
@Schema(description = "Resposta com simulações agrupadas por produto e data")
public class SimulacaoPorProdutoDiaResponseDTO {

    @Schema(description = "Data de referência da consulta no formato yyyy-MM-dd", example = "2025-08-23", required = true)
    private String dataReferencia;

    @Schema(description = "Lista de produtos com suas simulações", required = true)
    private List<SimulacoesDeUmProdutoDTO> simulacoes;
}
