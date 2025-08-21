package resource;

import domain.dto.*;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import service.SimulacaoService;
import service.EventHubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import domain.dto.SimulacaoQueryParams;

@Path("/simulacoes")
@Consumes("application/json")
@Produces("application/json")
public class SimulacaoResource {

    @Inject
    SimulacaoService simulacaoService;

    @Inject
    EventHubService eventHubService;

    @Inject
    ObjectMapper objectMapper;

    @POST
    public Response criarSimulacao(@Valid SimulacaoCreateDTO simulacaoCreateDTO) {
        SimulacaoResponseDTO response = simulacaoService.simularEmprestimo(simulacaoCreateDTO);
        // Envia resposta ao Event Hub (falha não impede resposta ao cliente)
        try {
            String json = objectMapper.writeValueAsString(response);
            eventHubService.sendMessage(json);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Response.ok(response).build();
    }

    @GET
    public Response listarSimulacoes(@Valid @BeanParam SimulacaoQueryParams params) {
        PaginaSimulacaoDTO paginaDTO = simulacaoService.listarSimulacoes(params.getPagina(), params.getQtdRegistrosPagina());
        return Response.ok(paginaDTO).build();
    }

    @GET
    @Path("/por-produto-dia")
    public Response buscarSimulacoesPorProdutoEData(@Valid @BeanParam SimulacaoQueryParams params) {
        SimulacaoPorProdutoDiaResponseDTO resposta = simulacaoService.buscarSimulacoesPorProdutoEData(
            params.getData(), params.getProdutoId());
        return Response.ok(resposta).build();
    }
}
