package domain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SimulacaoProdutoDiaResumoDTO {
    private Integer codigoProduto;
    private String descricaoProduto;
    private BigDecimal taxaMediaJuro;
    private BigDecimal valorMedioPrestacao;
    private BigDecimal valorTotalDesejado;
    private BigDecimal valorTotalCredito;
}

