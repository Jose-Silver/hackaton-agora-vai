package resource;

import domain.dto.*;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import service.SimulacaoService;

@Path("/simulacoes")
@Consumes("application/json")
@Produces("application/json")
public class SimulacaoResource {

    @Inject
    SimulacaoService simulacaoService;

    @POST
    @Transactional
    public Response criarSimulacao(SimulacaoCreateDTO simulacaoCreateDTO) {
        try {
            SimulacaoResponseDTO response = simulacaoService.simularEmprestimo(simulacaoCreateDTO);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(422).entity(e.getMessage()).build();
        }
    }

    @GET
    public Response listarSimulacoes(@QueryParam("pagina") @DefaultValue("1") int pagina,
                                     @QueryParam("qtdRegistrosPagina") @DefaultValue("20") int qtdRegistrosPagina) {
        PaginaSimulacaoDTO paginaDTO = simulacaoService.listarSimulacoes(pagina, qtdRegistrosPagina);
        return Response.ok(paginaDTO).build();
    }

    @GET
    @Path("/por-produto-dia")
    public Response listarSimulacoesPorProdutoDia() {
        return Response.ok(simulacaoService.listarSimulacoesPorProdutoDia()).build();
    }

    @GET
    @Path("/por-produto-dia-filtrado")
    public Response buscarSimulacoesPorProdutoEData(@QueryParam("data") String data,
                                                    @QueryParam("produtoId") Integer produtoId) {
        try {
            SimulacaoPorProdutoDiaResponseDTO resposta = simulacaoService.buscarSimulacoesPorProdutoEData(data, produtoId);
            return Response.ok(resposta).build();
        } catch (IllegalArgumentException e) {
            return Response.status(404).entity(e.getMessage()).build();
        }
    }
}
