package resource;

import domain.dto.SimulacaoPorProdutoDiaQueryParams;
import domain.dto.simulacao.create.request.SimulacaoCreateDTO;
import domain.dto.simulacao.create.response.SimulacaoResponseDTO;
import domain.dto.simulacao.create.response.PaginaSimulacaoSimplificadaDTO;
import domain.dto.simulacao.list.request.SimulacaoQueryParams;
import domain.dto.common.ErrorResponseDTO;
import domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaResponseDTO;
import domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaDTO;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.h2.store.Data;
import service.SimulacaoService;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Resource REST para operações relacionadas a simulações de empréstimo.
 * Responsável por expor os endpoints da API e delegar o processamento para os serviços.
 *
 * O tratamento de exceções é realizado automaticamente pelos ExceptionMappers.
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Simulação de Empréstimos API",
        version = "1.0.0",
        description = "API para simulação de empréstimos bancários com cálculos SAC e PRICE",
        contact = @Contact(
            name = "José da Rosa Silveira Junior",
            email = "Jose-rosa.junior@caixa.gov.br"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    )
)
@Path("/v1/simulacoes")
@Consumes("application/json")
@Produces("application/json")
@Tag(name = "Simulações", description = "Operações relacionadas a simulações de empréstimo")
public class SimulacaoResource {

    private static final Logger logger = Logger.getLogger(SimulacaoResource.class);

    @Inject
    SimulacaoService simulacaoService;

    /**
     * Cria uma nova simulação de empréstimo.
     */
    @POST
    @Operation(
        summary = "Criar nova simulação de empréstimo",
        description = "Cria uma simulação de empréstimo com os dados fornecidos, calculando as melhores opções de financiamento disponíveis"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Simulação criada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SimulacaoResponseDTO.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Dados de entrada inválidos ou nenhum produto elegível encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Erro interno do servidor",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        )
    })
    @RequestBody(
        description = "Dados para criação da simulação",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = SimulacaoCreateDTO.class)
        )
    )
    public Response criarSimulacao(
            @Valid SimulacaoCreateDTO solicitacaoSimulacao,
            @Context HttpHeaders headers) {

        String requestId = getOrGenerateRequestId(headers);

        logger.infof("[requestId=%s] Iniciando criação de simulação - Valor: %s, Prazo: %d meses",
                    requestId, solicitacaoSimulacao.getValorDesejado(), solicitacaoSimulacao.getPrazo());

        SimulacaoResponseDTO respostaSimulacao = simulacaoService.simularEmprestimo(solicitacaoSimulacao, requestId);

        logger.infof("[requestId=%s] Simulação criada com sucesso - SimulacaoId: %d",
                    requestId, respostaSimulacao.getIdSimulacao());
        return Response.ok(respostaSimulacao).build();
    }

    /**
     * Lista simulações com paginação.
     */
    @GET
    @Operation(
        summary = "Listar simulações",
        description = "Lista simulações com suporte a paginação. Retorna apenas os campos essenciais: idSimulacao, valorDesejado, prazo e valorTotalParcelas."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Lista de simulações recuperada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaginaSimulacaoSimplificadaDTO.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Parâmetros de paginação inválidos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        )
    })
    public Response listarSimulacoes(@Valid @BeanParam SimulacaoQueryParams parametrosConsulta) {

        logger.infof("Listando simulações - Página: %d, Registros por página: %d",
                    parametrosConsulta.getPagina(), parametrosConsulta.getQtdRegistrosPagina());

        PaginaSimulacaoSimplificadaDTO paginaSimulacao = simulacaoService.listarSimulacoes(
            parametrosConsulta.getPagina(),
            parametrosConsulta.getQtdRegistrosPagina()
        );
        return Response.ok(paginaSimulacao).build();
    }

    /**
     * Busca simulações separadas por produto e/ou data sem paginação.
     */
    @GET
    @Path("/por-produto-dia")
    @Operation(
        summary = "Buscar simulações por produto e data",
        description = """
        Busca simulações filtradas por produto e/ou data sem paginação.
        
        Exemplos de uso:
        - GET /simulacoes/por-produto-dia → Simulações de hoje
        - GET /simulacoes/por-produto-dia?data=2024-01-15 → Simulações de 15/01/2024
        - GET /simulacoes/por-produto-dia?produtoId=123 → Simulações do produto 123 de hoje
        - GET /simulacoes/por-produto-dia?data=2024-01-15&produtoId=123 → Simulações específicas
        """
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Simulações recuperadas com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SimulacaoPorProdutoDiaDTO[].class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Parâmetros de filtro inválidos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Produto não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        )
    })
    public Response buscarSimulacoesPorProdutoEData(
            @Valid @BeanParam SimulacaoPorProdutoDiaQueryParams parametrosConsulta,
            @Context HttpHeaders headers) {

        String requestId = getOrGenerateRequestId(headers);

        logger.infof("[requestId=%s] Buscando simulações por produto e data - Data: %s, ProdutoId: %s",
                    requestId, parametrosConsulta.getData(), parametrosConsulta.getProdutoId());


        SimulacaoPorProdutoDiaResponseDTO simulacoes = simulacaoService.buscarSimulacoesPorProdutoEData(
            parametrosConsulta.getData(),
            parametrosConsulta.getProdutoId(),
            requestId
        );

        logger.infof("[requestId=%s] Consulta concluída com sucesso - %d simulações retornadas",
                    requestId, simulacoes.getSimulacoes().size());
        return Response.ok(simulacoes).build();
    }

    private String getOrGenerateRequestId(HttpHeaders headers) {
        String headerId = headers.getHeaderString("X-Request-ID");
        return (headerId != null && !headerId.isBlank()) ? headerId : UUID.randomUUID().toString();
    }
}
