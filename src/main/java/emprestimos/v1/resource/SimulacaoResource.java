package emprestimos.v1.resource;

import emprestimos.v1.domain.dto.simulacao.por_produto_dia.request.SimulacaoPorProdutoDiaQueryParams;
import emprestimos.v1.domain.dto.simulacao.buscar.response.SimulacaoDetalhesDTO;
import emprestimos.v1.domain.dto.simulacao.create.request.SimulacaoCreateDTO;
import emprestimos.v1.domain.dto.simulacao.create.response.SimulacaoResponseDTO;
import emprestimos.v1.domain.dto.simulacao.create.response.PaginaSimulacaoSimplificadaDTO;
import emprestimos.v1.domain.dto.simulacao.list.request.SimulacaoQueryParams;
import emprestimos.v1.domain.dto.common.ErrorResponseDTO;
import emprestimos.v1.domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaDTO;
import emprestimos.v1.domain.dto.simulacao.parcelas.response.ParcelasSimulacaoDTO;
import emprestimos.v1.domain.dto.simulacao.parcela.response.ParcelaEspecificaDTO;
import emprestimos.v1.util.FieldFilterUtil;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import emprestimos.v1.service.SimulacaoService;
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
import org.jboss.logging.Logger;

import java.util.UUID;

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

    @Inject
    FieldFilterUtil fieldFilterUtil;

    @POST
    @Operation(
        summary = "Criar nova simulação de empréstimo",
        description = "Cria uma simulação de empréstimo com os dados fornecidos, calculando as melhores opções de financiamento disponíveis"
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Simulação criada com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SimulacaoResponseDTO.class))),
        @APIResponse(responseCode = "400", description = "Dados de entrada inválidos ou nenhum produto elegível encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @APIResponse(responseCode = "500", description = "Erro interno do servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Response criarSimulacao(@Valid SimulacaoCreateDTO solicitacaoSimulacao,
                                  @QueryParam("campos") String campos,
                                  @Context HttpHeaders headers) {
        var requestId = getOrGenerateRequestId(headers);

        logger.infof("[requestId=%s] Iniciando criação de simulação - Valor: %s, Prazo: %d meses",
                    requestId, solicitacaoSimulacao.getValorDesejado(), solicitacaoSimulacao.getPrazo());

        var respostaSimulacao = simulacaoService.simularEmprestimo(solicitacaoSimulacao, requestId);

        logger.infof("[requestId=%s] Simulação criada com sucesso - SimulacaoId: %d",
                    requestId, respostaSimulacao.getIdSimulacao());

        var responseFiltered = fieldFilterUtil.filterFields(respostaSimulacao, campos);
        return Response.ok(responseFiltered).build();
    }

    @GET
    @Operation(
        summary = "Listar simulações",
        description = "Lista simulações com suporte a paginação. Retorna apenas os campos essenciais."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista de simulações recuperada com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginaSimulacaoSimplificadaDTO.class))),
        @APIResponse(responseCode = "400", description = "Parâmetros de paginação inválidos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Response listarSimulacoes(@Valid @BeanParam SimulacaoQueryParams parametrosConsulta) {
        logger.infof("Listando simulações - Página: %d, Registros por página: %d",
                    parametrosConsulta.getPagina(), parametrosConsulta.getQtdRegistrosPagina());

        var paginaSimulacao = simulacaoService.listarSimulacoes(
            parametrosConsulta.getPagina(),
            parametrosConsulta.getQtdRegistrosPagina()
        );

        var responseFiltered = fieldFilterUtil.filterFields(paginaSimulacao, parametrosConsulta.getCampos());
        return Response.ok(responseFiltered).build();
    }

    /**
     * Busca simulações separadas por produto e/ou data sem paginação.
     */
    @GET
    @Path("/por-produto-dia")
    @Operation(
        summary = "Buscar simulações por produto e data",
        description = "Busca simulações filtradas por produto e/ou data. Suporta filtros opcionais de data e produto."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Simulações recuperadas com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SimulacaoPorProdutoDiaDTO[].class))),
        @APIResponse(responseCode = "400", description = "Parâmetros de filtro inválidos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @APIResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Response buscarSimulacoesPorProdutoEData(@Valid @BeanParam SimulacaoPorProdutoDiaQueryParams parametrosConsulta, @Context HttpHeaders headers) {
        var requestId = getOrGenerateRequestId(headers);

        logger.infof("[requestId=%s] Buscando simulações por produto e data - Data: %s, ProdutoId: %s",
                    requestId, parametrosConsulta.getData(), parametrosConsulta.getProdutoId());

        var simulacoes = simulacaoService.buscarSimulacoesPorProdutoEData(
            parametrosConsulta.getData(),
            parametrosConsulta.getProdutoId(),
            requestId
        );

        logger.infof("[requestId=%s] Consulta concluída com sucesso - %d simulações retornadas",
                    requestId, simulacoes.getSimulacoes().size());

        var responseFiltered = fieldFilterUtil.filterFields(simulacoes, parametrosConsulta.getCampos());
        return Response.ok(responseFiltered).build();
    }

    /**
     * Busca uma simulação específica pelo seu ID.
     */
    @GET
    @Path("/{id}")
    @Operation(
        summary = "Buscar simulação por ID",
        description = "Busca uma simulação específica pelo seu ID único com detalhes completos."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Simulação encontrada com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SimulacaoDetalhesDTO.class))),
        @APIResponse(responseCode = "404", description = "Simulação não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @APIResponse(responseCode = "400", description = "ID inválido fornecido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Response buscarSimulacaoPorId(@PathParam("id") Long id,
                                       @QueryParam("campos") String campos,
                                       @Context HttpHeaders headers) {
        var requestId = getOrGenerateRequestId(headers);

        logger.infof("[requestId=%s] Buscando simulação por ID: %d", requestId, id);

        var simulacao = simulacaoService.buscarSimulacaoPorId(id, requestId);

        logger.infof("[requestId=%s] Simulação encontrada com sucesso - ID: %d", requestId, id);

        var responseFiltered = fieldFilterUtil.filterFields(simulacao, campos);
        return Response.ok(responseFiltered).build();
    }

    /**
     * Busca todas as parcelas de um tipo específico de amortização para uma simulação.
     */
    @GET
    @Path("/{id}/{tipoAmortizacao}")
    @Operation(
        summary = "Buscar parcelas por tipo de amortização",
        description = "Busca todas as parcelas de um tipo específico de amortização (SAC ou PRICE) para uma simulação."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Parcelas encontradas com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParcelasSimulacaoDTO.class))),
        @APIResponse(responseCode = "400", description = "Tipo de amortização inválido ou ID inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @APIResponse(responseCode = "404", description = "Simulação não encontrada ou produto não elegível",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Response buscarParcelasPorTipoAmortizacao(@PathParam("id") Long id,
                                                   @PathParam("tipoAmortizacao") String tipoAmortizacao,
                                                   @QueryParam("campos") String campos,
                                                   @Context HttpHeaders headers) {
        var requestId = getOrGenerateRequestId(headers);

        logger.infof("[requestId=%s] Buscando parcelas por tipo de amortização - SimulacaoId: %d, Tipo: %s",
                    requestId, id, tipoAmortizacao);

        var parcelas = simulacaoService.buscarParcelasPorTipoAmortizacao(id, tipoAmortizacao, requestId);

        logger.infof("[requestId=%s] Parcelas encontradas com sucesso - SimulacaoId: %d, Tipo: %s, Quantidade: %d",
                    requestId, id, tipoAmortizacao, parcelas.getQuantidadeParcelas());

        var responseFiltered = fieldFilterUtil.filterFields(parcelas, campos);
        return Response.ok(responseFiltered).build();
    }

    /**
     * Busca informações detalhadas de uma parcela específica.
     */
    @GET
    @Path("/{id}/{tipoAmortizacao}/{parcelaId}")
    @Operation(
        summary = "Buscar parcela específica",
        description = "Busca informações detalhadas de uma parcela específica incluindo saldo devedor e percentuais."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Parcela específica encontrada com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParcelaEspecificaDTO.class))),
        @APIResponse(responseCode = "400", description = "Parâmetros inválidos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @APIResponse(responseCode = "404", description = "Simulação, produto ou parcela não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Response buscarParcelaEspecifica(@PathParam("id") Long id,
                                          @PathParam("tipoAmortizacao") String tipoAmortizacao,
                                          @PathParam("parcelaId") Long parcelaId,
                                          @QueryParam("campos") String campos,
                                          @Context HttpHeaders headers) {
        var requestId = getOrGenerateRequestId(headers);

        logger.infof("[requestId=%s] Buscando parcela específica - SimulacaoId: %d, Tipo: %s, ParcelaId: %d",
                    requestId, id, tipoAmortizacao, parcelaId);

        var parcela = simulacaoService.buscarParcelaEspecifica(id, tipoAmortizacao, parcelaId, requestId);

        logger.infof("[requestId=%s] Parcela específica encontrada - SimulacaoId: %d, Tipo: %s, Parcela: %d, Valor: R$ %s",
                    requestId, id, tipoAmortizacao, parcelaId, parcela.getValorPrestacao());

        var responseFiltered = fieldFilterUtil.filterFields(parcela, campos);
        return Response.ok(responseFiltered).build();
    }

    private String getOrGenerateRequestId(HttpHeaders headers) {
        var headerId = headers.getHeaderString("X-Request-ID");
        return (headerId != null && !headerId.isBlank()) ? headerId : UUID.randomUUID().toString();
    }
}
