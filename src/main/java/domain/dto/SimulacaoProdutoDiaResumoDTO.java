package domain.dto;

import lombok.Data;
import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Schema(description = "Resumo estatístico das simulações de um produto em uma data.")
public class SimulacaoProdutoDiaResumoDTO {
    private Integer codigoProduto;
    private String descricaoProduto;
    private BigDecimal taxaMediaJuro;
    private BigDecimal valorMedioPrestacao;
    private BigDecimal valorTotalDesejado;
    private BigDecimal valorTotalCredito;
}
