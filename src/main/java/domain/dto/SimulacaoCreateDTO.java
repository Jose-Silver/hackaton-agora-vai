package domain.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SimulacaoCreateDTO {
    @NotNull(message = "O campo 'valor desejado' é obrigatório.")
    @Positive(message = "O valor desejado deve ser maior que zero.")
    private Long valorDesejado;
    @NotNull(message = "O campo 'prazo' é obrigatório.")
    @Positive(message = "O prazo deve ser maior que zero.")
    private Long prazo;
}
