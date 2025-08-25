package emprestimos.v1.domain.dto.simulacao.create.response;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Data
@Schema(description = "Detalhes completos de uma simulação de empréstimo")
public class SimulacaoDetalheDTO {

    @Schema(description = "Identificador único da simulação", example = "12345", required = true)
    private Long id;

    @Schema(description = "Código do produto selecionado", example = "1", required = true)
    private Integer codigoProduto;

    @Schema(description = "Nome/descrição do produto financeiro", example = "Crédito Pessoal", required = true)
    private String descricaoProduto;

    @Schema(description = "Valor desejado para o empréstimo em reais", example = "25000.00", required = true)
    private BigDecimal valorDesejado;

    @Schema(description = "Prazo do empréstimo em meses", example = "36", required = true)
    private Integer prazo;

    @Schema(description = "Taxa de juros aplicada ao produto", example = "0.15", required = true)
    private BigDecimal taxaJuro;

    @Schema(description = "Valor da prestação mensal", example = "850.50", required = true)
    private BigDecimal valorPrestacao;

    @Schema(description = "Data e hora da simulação", example = "2025-08-23T10:30:00", required = true)
    private String dataSimulacao;

    @Schema(description = "Valor total a ser pago", example = "30618.00")
    private BigDecimal valorTotalPagar;
}
