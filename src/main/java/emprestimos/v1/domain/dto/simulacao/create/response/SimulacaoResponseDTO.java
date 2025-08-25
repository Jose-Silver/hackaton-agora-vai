package emprestimos.v1.domain.dto.simulacao.create.response;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "DTO de resposta para simulação de empréstimo.")
public class SimulacaoResponseDTO {
    @Schema(description = "ID da simulação gerada.", example = "123")
    private Long idSimulacao;
    @Schema(description = "Código do produto.", example = "456")
    private Integer codigoProduto;
    @Schema(description = "Descrição do produto.", example = "Crédito Imobiliário")
    private String descricaoProduto;
    @Schema(description = "Taxa de juros aplicada.", example = "0.08")
    private BigDecimal taxaJuros;
    @Schema(description = "Lista de resultados da simulação.")
    private List<ResultadoSimulacaoDTO> resultadoSimulacao;
    @Schema(description = "Mensagem informativa sobre o resultado da simulação.", example = "Simulação realizada com sucesso")
    private String mensagem;
    @Schema(description = "Indica se a simulação encontrou produtos elegíveis.", example = "true")
    private Boolean sucesso;
}
