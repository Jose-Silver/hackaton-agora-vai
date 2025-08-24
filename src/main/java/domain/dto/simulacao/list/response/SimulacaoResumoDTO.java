package domain.dto.simulacao.list.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Schema(description = "Resumo de uma simulação de empréstimo")
public class SimulacaoResumoDTO {

    @Schema(description = "Identificador único da simulação", example = "12345")
    private Long idSimulacao;

    @Schema(description = "Valor desejado para o empréstimo em reais", example = "25000.00")
    private BigDecimal valorDesejado;

    @Schema(description = "Prazo do empréstimo em meses", example = "36" )
    private Integer prazo;

    @Schema(description = "Valor total a ser pago incluindo juros", example = "30500.50", required = true)
    private BigDecimal valorTotalParcelas;

    @Schema(description = "Data de criação da simulação", example = "2025-08-23T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataSimulacao;

    @Schema(description = "Código do produto selecionado", example = "2")
    private Integer codigoProduto;

    @Schema(description = "Nome do produto selecionado", example = "Crédito Consignado")
    private String nomeProduto;
}
