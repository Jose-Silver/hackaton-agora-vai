package emprestimos.v1.domain.dto.simulacao.parcelas.response;

import emprestimos.v1.domain.dto.simulacao.create.response.ParcelaDTO;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@XmlRootElement(name = "parcelasSimulacao")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "DTO com as parcelas de um tipo específico de amortização para uma simulação")
public class ParcelasSimulacaoDTO {

    @XmlElement(name = "idSimulacao")
    @Schema(description = "ID único da simulação", example = "1", required = true)
    private Long idSimulacao;

    @XmlElement(name = "tipoAmortizacao")
    @Schema(description = "Tipo de amortização das parcelas", example = "SAC", required = true)
    private String tipoAmortizacao;

    @XmlElement(name = "descricaoTipoAmortizacao")
    @Schema(description = "Descrição do tipo de amortização", example = "Sistema de Amortização Constante", required = true)
    private String descricaoTipoAmortizacao;

    @XmlElement(name = "codigoProduto")
    @Schema(description = "Código do produto associado", example = "1")
    private Integer codigoProduto;

    @XmlElement(name = "descricaoProduto")
    @Schema(description = "Nome do produto associado", example = "Produto 1")
    private String descricaoProduto;

    @XmlElement(name = "valorDesejado")
    @Schema(description = "Valor desejado para o empréstimo", example = "5000.00", required = true)
    private BigDecimal valorDesejado;

    @XmlElement(name = "prazo")
    @Schema(description = "Prazo em meses", example = "12", required = true)
    private Integer prazo;

    @XmlElement(name = "taxaJuros")
    @Schema(description = "Taxa de juros aplicada", example = "0.0179", required = true)
    private BigDecimal taxaJuros;

    @XmlElementWrapper(name = "parcelas")
    @XmlElement(name = "parcela")
    @Schema(description = "Lista completa de parcelas do tipo de amortização especificado", required = true)
    private List<ParcelaDTO> parcelas;

    @XmlElement(name = "valorTotalParcelas")
    @Schema(description = "Valor total das parcelas", example = "6500.00", required = true)
    private BigDecimal valorTotalParcelas;

    @XmlElement(name = "quantidadeParcelas")
    @Schema(description = "Quantidade total de parcelas", example = "12", required = true)
    private Integer quantidadeParcelas;

    @XmlElement(name = "Links")
    @Schema(description = "Links Hypermidia")
    public Map<String, String> links = new HashMap<>();

    public void addLink(String rel, String href) {
        this.links.put(rel, href);
    }
}
