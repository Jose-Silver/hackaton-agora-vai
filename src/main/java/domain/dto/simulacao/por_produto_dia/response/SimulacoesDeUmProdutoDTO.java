package domain.dto.simulacao.por_produto_dia.response;

import domain.dto.simulacao.create.response.SimulacaoDetalheDTO;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "Simulações agrupadas por um produto específico")
public class SimulacoesDeUmProdutoDTO {

    @Schema(description = "Código identificador do produto", example = "1", required = true)
    private Integer codigoProduto;

    @Schema(description = "Nome/descrição do produto financeiro", example = "Crédito Pessoal", required = true)
    private String descricaoProduto;

    @Schema(description = "Taxa de juros do produto", example = "0.15")
    private BigDecimal taxaJuros;

    @Schema(description = "Lista de simulações realizadas para este produto", required = true)
    private List<SimulacaoDetalheDTO> simulacoes;

    @Schema(description = "Quantidade total de simulações do produto", example = "5")
    private Integer totalSimulacoes;
}
