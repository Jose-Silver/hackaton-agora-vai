package resource;

import domain.dto.simulacao.list.response.SimulacaoResumoSimplificadoDTO;
import domain.entity.local.Simulacao;
import domain.entity.remote.Produto;

public class SimulacaoMapper {


    public static SimulacaoResumoSimplificadoDTO toSimulacaoResumoSimplificadoDTO(Simulacao simulacao) {
        SimulacaoResumoSimplificadoDTO dto = new SimulacaoResumoSimplificadoDTO();
        dto.setIdSimulacao(simulacao.getId());
        dto.setValorDesejado(simulacao.getValorDesejado());
        dto.setPrazo(simulacao.getPrazo().intValue());
        dto.setValorTotalParcelas(simulacao.getValorTotalCredito());
        return dto;
    }


}
