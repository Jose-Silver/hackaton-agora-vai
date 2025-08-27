package emprestimos.v1.domain.dto.simulacao.por_produto_dia.response;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;

@Data
@XmlRootElement(name = "simulacaoPorProdutoDia")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "DTO com informações da simulação por produto no dia")
public class SimulacaoPorProdutoDiaDTO {
    @XmlElement(name = "codigoProduto")
    @Schema(description = "Código do produto")
    private Integer codigoProduto;

    @XmlElement(name = "descricaoProduto")
    @Schema(description = "Descrição do produto")
    private String descricaoProduto;

    @XmlElement(name = "taxaMediaJuro")
    @Schema(description = "Taxa média de juros" )
    private Double taxaMediaJuro;

    @XmlElement(name = "valorMedioPrestacao")
    @Schema(description = "Valor médio da prestação" )
    private Double valorMedioPrestacao;

    @XmlElement(name = "valorTotalDesejado")
    @Schema(description = "Valor total desejado" )
    private Double valorTotalDesejado;

    @XmlElement(name = "valorTotalCredito")
    @Schema(description = "Valor total de crédito")
    private Double valorTotalCredito;

    @XmlElementWrapper(name = "links")
    @XmlElement(name = "link")
    @Schema(description = "Links HATEOAS para navegação")
    private Map<String, String> links = new HashMap<>();

    public void addLink(String rel, String href) {
        links.put(rel, href);
    }
}
