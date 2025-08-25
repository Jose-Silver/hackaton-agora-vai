package emprestimos.v1.domain.dto.simulacao.por_produto_dia.response;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Schema(description = "DTO com informações da simulação por produto no dia")
public class SimulacaoPorProdutoDiaDTO {
    @Schema(description = "Código do produto")
    private Integer codigoProduto;

    @Schema(description = "Descrição do produto")
    private String descricaoProduto;

    @Schema(description = "Taxa média de juros" )
    private Double taxaMediaJuro;

    @Schema(description = "Valor médio da prestação" )
    private Double valorMedioPrestacao;

    @Schema(description = "Valor total desejado" )
    private Double valorTotalDesejado;

    @Schema(description = "Valor total de crédito")
    private Double valorTotalCredito;
}
