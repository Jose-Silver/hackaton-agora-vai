package emprestimos.v1.domain.dto.simulacao.parcelas.response;

import emprestimos.v1.domain.dto.simulacao.create.response.ParcelaDTO;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "DTO com as parcelas de um tipo específico de amortização para uma simulação")
public class ParcelasSimulacaoDTO {

    @Schema(description = "ID único da simulação", example = "1", required = true)
    private Long idSimulacao;

    @Schema(description = "Tipo de amortização das parcelas", example = "SAC", required = true)
    private String tipoAmortizacao;

    @Schema(description = "Descrição do tipo de amortização", example = "Sistema de Amortização Constante", required = true)
    private String descricaoTipoAmortizacao;

    @Schema(description = "Código do produto associado", example = "1")
    private Integer codigoProduto;

    @Schema(description = "Nome do produto associado", example = "Produto 1")
    private String descricaoProduto;

    @Schema(description = "Valor desejado para o empréstimo", example = "5000.00", required = true)
    private BigDecimal valorDesejado;

    @Schema(description = "Prazo em meses", example = "12", required = true)
    private Integer prazo;

    @Schema(description = "Taxa de juros aplicada", example = "0.0179", required = true)
    private BigDecimal taxaJuros;

    @Schema(description = "Lista completa de parcelas do tipo de amortização especificado", required = true)
    private List<ParcelaDTO> parcelas;

    @Schema(description = "Valor total das parcelas", example = "6500.00", required = true)
    private BigDecimal valorTotalParcelas;

    @Schema(description = "Quantidade total de parcelas", example = "12", required = true)
    private Integer quantidadeParcelas;
}
