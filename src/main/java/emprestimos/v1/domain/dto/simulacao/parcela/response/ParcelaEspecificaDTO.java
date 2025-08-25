package emprestimos.v1.domain.dto.simulacao.parcela.response;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Data
@XmlRootElement(name = "parcelaEspecifica")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "DTO com informações detalhadas de uma parcela específica")
public class ParcelaEspecificaDTO {

    @XmlElement(name = "idSimulacao")
    @Schema(description = "ID único da simulação", required = true)
    private Long idSimulacao;

    @XmlElement(name = "tipoAmortizacao")
    @Schema(description = "Tipo de amortização da parcela", required = true)
    private String tipoAmortizacao;

    @XmlElement(name = "descricaoTipoAmortizacao")
    @Schema(description = "Descrição do tipo de amortização", required = true)
    private String descricaoTipoAmortizacao;

    @XmlElement(name = "codigoProduto")
    @Schema(description = "Código do produto associado")
    private Integer codigoProduto;

    @XmlElement(name = "descricaoProduto")
    @Schema(description = "Nome do produto associado")
    private String descricaoProduto;

    @XmlElement(name = "valorDesejado")
    @Schema(description = "Valor desejado para o empréstimo", required = true)
    private BigDecimal valorDesejado;

    @XmlElement(name = "prazo")
    @Schema(description = "Prazo em meses", required = true)
    private Integer prazo;

    @XmlElement(name = "taxaJuros")
    @Schema(description = "Taxa de juros aplicada", required = true)
    private BigDecimal taxaJuros;

    @XmlElement(name = "numeroParcela")
    @Schema(description = "Número da parcela específica", required = true)
    private Long numeroParcela;

    @XmlElement(name = "valorAmortizacao")
    @Schema(description = "Valor da amortização desta parcela específica", required = true)
    private BigDecimal valorAmortizacao;

    @XmlElement(name = "valorJuros")
    @Schema(description = "Valor dos juros desta parcela específica", required = true)
    private BigDecimal valorJuros;

    @XmlElement(name = "valorPrestacao")
    @Schema(description = "Valor total da prestação desta parcela específica", required = true)
    private BigDecimal valorPrestacao;

    @XmlElement(name = "valorTotalFinanciamento")
    @Schema(description = "Valor total das parcelas de todo o financiamento", required = true)
    private BigDecimal valorTotalFinanciamento;

    @XmlElement(name = "quantidadeTotalParcelas")
    @Schema(description = "Quantidade total de parcelas do financiamento", required = true)
    private Integer quantidadeTotalParcelas;

    @XmlElement(name = "saldoDevedor")
    @Schema(description = "Saldo devedor restante após o pagamento desta parcela")
    private BigDecimal saldoDevedor;

    @XmlElement(name = "percentualAmortizacao")
    @Schema(description = "Percentual de amortização sobre o valor total")
    private BigDecimal percentualAmortizacao;

    @XmlElement(name = "percentualJuros")
    @Schema(description = "Percentual de juros sobre o valor da prestação")
    private BigDecimal percentualJuros;
}