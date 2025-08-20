package domain.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SimulacaoPorProdutoDiaDTO {
    private Integer codigoProduto;
    private String descricaoProduto;
    private LocalDate dataSimulacao;
    private BigDecimal valorDesejado;
    private Long prazo;
    private BigDecimal valorTotalParcelas;
}

