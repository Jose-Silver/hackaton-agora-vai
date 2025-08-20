package domain.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SimulacaoCreateDTO {
    @NotNull(message = "O campo 'valor desejado' é obrigatório.")
    private Long valorDesejado;
    @NotNull(message = "O campo 'prazo' é obrigatório.")
    private Long prazo;
}
