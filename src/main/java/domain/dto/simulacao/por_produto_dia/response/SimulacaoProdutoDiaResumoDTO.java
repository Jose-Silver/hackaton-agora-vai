package domain.dto.simulacao.por_produto_dia.response;

import lombok.Data;
import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Schema(description = "Resumo estatístico das simulações de um produto em uma data")
public class SimulacaoProdutoDiaResumoDTO {

    @Schema(description = "Código identificador do produto", example = "1", required = true)
    private Integer codigoProduto;

    @Schema(description = "Nome/descrição do produto financeiro", example = "Crédito Pessoal", required = true)
    private String descricaoProduto;

    @Schema(description = "Taxa média de juros do produto", example = "0.15", required = true)
    private BigDecimal taxaMediaJuro;

    @Schema(description = "Valor médio das prestações", example = "850.75", required = true)
    private BigDecimal valorMedioPrestacao;

    @Schema(description = "Valor total desejado (soma de todas as simulações)", example = "125000.00", required = true)
    private BigDecimal valorTotalDesejado;

    @Schema(description = "Valor total de crédito aprovado", example = "125000.00", required = true)
    private BigDecimal valorTotalCredito;

    @Schema(description = "Quantidade de simulações realizadas", example = "8")
    private Integer quantidadeSimulacoes;
}
