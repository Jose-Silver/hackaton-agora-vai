package domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SimulacaoDetalheDTO {
    private Long id;
    private Integer codigoProduto;
    private String descricaoProduto;
    private BigDecimal valorDesejado;
    private Integer prazo;
    private BigDecimal taxaJuro;
    private BigDecimal valorPrestacao;
    private String dataSimulacao;
}
