package domain.dto.simulacao.buscar.response;

import domain.dto.simulacao.create.response.ResultadoSimulacaoDTO;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "DTO com detalhes completos de uma simulação específica")
public class SimulacaoDetalhesDTO {
    
    @Schema(description = "ID único da simulação", example = "1")
    private Long id;
    
    @Schema(description = "Valor desejado para o empréstimo", example = "5000.00")
    private BigDecimal valorDesejado;
    
    @Schema(description = "Prazo em meses", example = "12")
    private Integer prazo;
    
    @Schema(description = "Código do produto associado", example = "1")
    private Integer codigoProduto;
    
    @Schema(description = "Nome do produto associado", example = "Produto 1")
    private String descricaoProduto;
    
    @Schema(description = "Taxa de juros aplicada", example = "0.0179")
    private BigDecimal taxaJuros;
    
    @Schema(description = "Valor médio da prestação", example = "541.67")
    private BigDecimal valorMedioPrestacao;
    
    @Schema(description = "Valor total do crédito", example = "6500.00")
    private BigDecimal valorTotalCredito;
    
    @Schema(description = "Data e hora da simulação", example = "2025-08-24T10:00:00")
    private String dataSimulacao;

    @Schema(description = "Resultados da simulação com parcelas SAC e PRICE", required = true)
    private List<ResultadoSimulacaoDTO> resultadosSimulacao;
}
