package resource;

import domain.dto.*;
import domain.entity.local.Simulacao;
import domain.entity.remote.Produto;

public class SimulacaoMapper {
    public static SimulacaoResumoDTO toSimulacaoResumoDTO(Simulacao simulacao) {
        SimulacaoResumoDTO dto = new SimulacaoResumoDTO();
        dto.setIdSimulacao(simulacao.getId());
        dto.setValorDesejado(simulacao.getValorDesejado());
        dto.setPrazo(simulacao.getPrazo().intValue());
        dto.setValorTotalParcelas(simulacao.getValorTotalCredito());
        return dto;
    }

    public static SimulacaoProdutoDiaResumoDTO toSimulacaoProdutoDiaResumoDTO(Simulacao simulacao, Produto produto) {
        SimulacaoProdutoDiaResumoDTO dto = new SimulacaoProdutoDiaResumoDTO();
        dto.setCodigoProduto(produto.getCoProduto());
        dto.setDescricaoProduto(produto.getNoProduto());
        dto.setTaxaMediaJuro(simulacao.getTaxaMediaJuros());
        dto.setValorMedioPrestacao(simulacao.getValorMedioPrestacao());
        dto.setValorTotalDesejado(simulacao.getValorTotalDesejado());
        dto.setValorTotalCredito(simulacao.getValorTotalCredito());
        return dto;
    }
}

