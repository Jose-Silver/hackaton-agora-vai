package domain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ParcelaDTO {
    private Long numero;
    private BigDecimal valorAmortizacao;
    private BigDecimal valorJuros;
    private BigDecimal valorPrestacao;
}

