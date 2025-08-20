package resource;

import domain.dto.SimulacaoCreateDTO;
import domain.dto.SimulacaoResponseDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import service.SimulacaoService;

@Path("/simulacoes")
@Consumes("application/json")
@Produces("application/json")
public class SimulacaoResource {

    @Inject
    SimulacaoService simulacaoService;

    @POST
    public Response criarSimulacao(SimulacaoCreateDTO simulacaoCreateDTO) {
        try {
            SimulacaoResponseDTO response = simulacaoService.simularEmprestimo(simulacaoCreateDTO);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(422).entity(e.getMessage()).build();
        }
    }
}
