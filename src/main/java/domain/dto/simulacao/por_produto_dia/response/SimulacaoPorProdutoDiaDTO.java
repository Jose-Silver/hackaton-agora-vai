package domain.dto.simulacao.por_produto_dia.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Schema(description = "Dados de simulação agrupada por produto e data")
public class SimulacaoPorProdutoDiaDTO {

    @Schema(description = "Código identificador do produto", example = "1", required = true)
    private Integer codigoProduto;

    @Schema(description = "Nome/descrição do produto financeiro", example = "Crédito Pessoal", required = true)
    private String descricaoProduto;

    @Schema(description = "Data da simulação", example = "2025-08-23", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataSimulacao;

    @Schema(description = "Valor desejado para o empréstimo em reais", example = "15000.00", required = true)
    private BigDecimal valorDesejado;

    @Schema(description = "Prazo do empréstimo em meses", example = "24", required = true)
    private Long prazo;

    @Schema(description = "Valor total das parcelas incluindo juros", example = "18500.75", required = true)
    private BigDecimal valorTotalParcelas;

    @Schema(description = "Taxa de juros aplicada ao produto", example = "0.15")
    private BigDecimal taxaJuros;

    @Schema(description = "Quantidade de simulações realizadas para este produto na data", example = "5")
    private Integer quantidadeSimulacoes;
}
