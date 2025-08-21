package domain.dto;

import jakarta.ws.rs.QueryParam;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulacaoQueryParams {
    @QueryParam("pagina")
    @Min(value = 1, message = "O número da página deve ser maior ou igual a 1.")
    private Integer pagina = 1;

    @QueryParam("qtdRegistrosPagina")
    @Min(value = 1, message = "A quantidade de registros por página deve ser maior ou igual a 1.")
    private Integer qtdRegistrosPagina = 20;

    @QueryParam("data")
    @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}$", message = "Data inválida. Use o formato yyyy-MM-dd.")
    private String data;

    @QueryParam("produtoId")
    @Positive(message = "produtoId deve ser um número positivo.")
    private Integer produtoId;
}

