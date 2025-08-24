package domain.dto.simulacao.create.request;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados de entrada para criar uma nova simulação de empréstimo")
public class SimulacaoCreateDTO {

    @NotNull(message = "O campo 'valor desejado' é obrigatório.")
    @Positive(message = "O valor desejado deve ser maior que zero.")
    @Schema(
        description = "Valor desejado para o empréstimo em reais",
        examples = {"10000.00"},
        required = true
    )
    private BigDecimal valorDesejado;

    @NotNull(message = "O campo 'prazo' é obrigatório.")
    @Positive(message = "O prazo deve ser maior que zero.")
    @Schema(
        description = "Prazo do empréstimo em meses",
        examples = {"36"},
        required = true
    )
    private Integer prazo;
}
