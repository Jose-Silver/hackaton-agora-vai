package emprestimos.v1.resource;

import emprestimos.v1.domain.dto.simulacao.list.response.SimulacaoResumoSimplificadoDTO;
import emprestimos.v1.domain.entity.local.Simulacao;

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
