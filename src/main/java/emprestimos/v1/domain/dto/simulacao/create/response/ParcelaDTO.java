package emprestimos.v1.domain.dto.simulacao.create.response;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.math.BigDecimal;

@Data
@Schema(description = "Dados de uma parcela do empréstimo")
public class ParcelaDTO {

    @Schema(description = "Número da parcela", example = "1", required = true)
    private Long numero;

    @Schema(description = "Valor da amortização da parcela em reais", example = "833.33", required = true)
    private BigDecimal valorAmortizacao;

    @Schema(description = "Valor dos juros da parcela em reais", example = "166.67", required = true)
    private BigDecimal valorJuros;

    @Schema(description = "Valor total da prestação em reais", example = "1000.00", required = true)
    private BigDecimal valorPrestacao;
}
