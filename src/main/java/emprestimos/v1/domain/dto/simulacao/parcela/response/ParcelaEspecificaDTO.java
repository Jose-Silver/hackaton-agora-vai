package emprestimos.v1.domain.dto.simulacao.parcela.response;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Data
@Schema(description = "DTO com informações detalhadas de uma parcela específica")
public class ParcelaEspecificaDTO {
    
    @Schema(description = "ID único da simulação", required = true)
    private Long idSimulacao;
    
    @Schema(description = "Tipo de amortização da parcela", required = true)
    private String tipoAmortizacao;
    
    @Schema(description = "Descrição do tipo de amortização", required = true)
    private String descricaoTipoAmortizacao;
    
    @Schema(description = "Código do produto associado")
    private Integer codigoProduto;
    
    @Schema(description = "Nome do produto associado")
    private String descricaoProduto;
    
    @Schema(description = "Valor desejado para o empréstimo", required = true)
    private BigDecimal valorDesejado;
    
    @Schema(description = "Prazo em meses", required = true)
    private Integer prazo;
    
    @Schema(description = "Taxa de juros aplicada", required = true)
    private BigDecimal taxaJuros;
    
    @Schema(description = "Número da parcela específica", required = true)
    private Long numeroParcela;
    
    @Schema(description = "Valor da amortização desta parcela específica", required = true)
    private BigDecimal valorAmortizacao;
    
    @Schema(description = "Valor dos juros desta parcela específica", required = true)
    private BigDecimal valorJuros;
    
    @Schema(description = "Valor total da prestação desta parcela específica", required = true)
    private BigDecimal valorPrestacao;
    
    @Schema(description = "Valor total das parcelas de todo o financiamento", required = true)
    private BigDecimal valorTotalFinanciamento;
    
    @Schema(description = "Quantidade total de parcelas do financiamento", required = true)
    private Integer quantidadeTotalParcelas;
    
    @Schema(description = "Saldo devedor restante após o pagamento desta parcela")
    private BigDecimal saldoDevedor;
    
    @Schema(description = "Percentual de amortização sobre o valor total")
    private BigDecimal percentualAmortizacao;
    
    @Schema(description = "Percentual de juros sobre o valor da prestação")
    private BigDecimal percentualJuros;
}
