package domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class SimulacoesDeUmProdutoDTO {
    private Integer codigoProduto;
    private String descricaoProduto;
    private List<SimulacaoDetalheDTO> simulacoes;
}
