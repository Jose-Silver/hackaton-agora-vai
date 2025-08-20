package domain.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SimulacaoResponseDTO {
    private Long idSimulacao;
    private Integer codigoProduto;
    private String descricaoProduto;
    private BigDecimal taxaJuros;
    private List<ResultadoSimulacaoDTO> resultadoSimulacao;
}

