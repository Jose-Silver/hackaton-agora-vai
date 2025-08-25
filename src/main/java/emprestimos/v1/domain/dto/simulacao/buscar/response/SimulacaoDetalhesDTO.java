package emprestimos.v1.domain.dto.simulacao.buscar.response;

import emprestimos.v1.domain.dto.simulacao.create.response.ResultadoSimulacaoDTO;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Data
@XmlRootElement(name = "simulacaoDetalhes")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "DTO com detalhes completos de uma simulação específica")
public class SimulacaoDetalhesDTO {
    
    @XmlElement(name = "id")
    @Schema(description = "ID único da simulação", example = "1")
    private Long id;
    
    @XmlElement(name = "valorDesejado")
    @Schema(description = "Valor desejado para o empréstimo", example = "5000.00")
    private BigDecimal valorDesejado;
    
    @XmlElement(name = "prazo")
    @Schema(description = "Prazo em meses", example = "12")
    private Integer prazo;
    
    @XmlElement(name = "codigoProduto")
    @Schema(description = "Código do produto associado", example = "1")
    private Integer codigoProduto;
    
    @XmlElement(name = "descricaoProduto")
    @Schema(description = "Nome do produto associado", example = "Produto 1")
    private String descricaoProduto;
    
    @XmlElement(name = "taxaJuros")
    @Schema(description = "Taxa de juros aplicada", example = "0.0179")
    private BigDecimal taxaJuros;
    
    @XmlElement(name = "valorMedioPrestacao")
    @Schema(description = "Valor médio da prestação", example = "541.67")
    private BigDecimal valorMedioPrestacao;
    
    @XmlElement(name = "valorTotalCredito")
    @Schema(description = "Valor total do crédito", example = "6500.00")
    private BigDecimal valorTotalCredito;
    
    @XmlElement(name = "dataSimulacao")
    @Schema(description = "Data e hora da simulação", example = "2025-08-24T10:00:00")
    private String dataSimulacao;

    @XmlElementWrapper(name = "resultadosSimulacao")
    @XmlElement(name = "resultado")
    @Schema(description = "Resultados da simulação com parcelas SAC e PRICE", required = true)
    private List<ResultadoSimulacaoDTO> resultadosSimulacao;
}
