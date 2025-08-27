package emprestimos.v1.resource;

import emprestimos.v1.config.RateLimited;
import emprestimos.v1.config.Auditado;
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
import jakarta.ws.rs.core.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Tag(name = "Simulações", description = "Operações relacionadas a simulações de empréstimo")
public class SimulacaoResource {

    private static final Logger logger = LoggerFactory.getLogger(SimulacaoResource.class);

    @Inject
    SimulacaoService simulacaoService;

    @Inject
    FieldFilterUtil fieldFilterUtil;

    @POST
    @RateLimited(maxRequests = 10, timeWindowSeconds = 60)
    @Auditado(acao = "CRIAR_SIMULACAO", recurso = "SIMULACAO", capturarDadosNovos = true)
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
                                   @Context HttpHeaders headers,
                                   @Context UriInfo uriInfo
                                   ) {
        var requestId = getOrGenerateRequestId(headers);

        logger.info("[requestId={}] Iniciando criação de simulação - Valor: {}, Prazo: {} meses",
                    requestId, solicitacaoSimulacao.getValorDesejado(), solicitacaoSimulacao.getPrazo());

        var respostaSimulacao = simulacaoService.simularEmprestimo(solicitacaoSimulacao, requestId);

        // Adicionar links "detalhe" para todas as parcelas nos resultados da simulação
        respostaSimulacao.getResultadoSimulacao().forEach(resultado -> {
            resultado.getParcelas().forEach(parcela -> {
                parcela.addLink("detalhe", uriInfo.getBaseUriBuilder()
                    .path(SimulacaoResource.class)
                    .path(String.valueOf(respostaSimulacao.getIdSimulacao()))
                    .path(resultado.getTipo())
                    .path(String.valueOf(parcela.getNumero()))
                    .build().toString());
            });
        });

        logger.info("[requestId={}] Simulação criada com sucesso - SimulacaoId: {}",
                    requestId, respostaSimulacao.getIdSimulacao());

        respostaSimulacao.addLink("self", uriInfo.getBaseUriBuilder().path(SimulacaoResource.class).path(String.valueOf(respostaSimulacao.getIdSimulacao())).build().toString());

        var responseFiltered = fieldFilterUtil.filterFields(respostaSimulacao, campos);

        return Response.ok(responseFiltered).build();
    }
    @GET
    @RateLimited(maxRequests = 50, timeWindowSeconds = 60)
    @Auditado(acao = "LISTAR_SIMULACOES", recurso = "SIMULACAO")
    @Operation(
        summary = "Listar simulações",
        description = "Lista simulações com suporte a paginação. Retorna apenas os campos essenciais."
    )
    @APIResponses({
        @APIResponse(responseCode = "206", description = "Lista de simulações recuperada com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginaSimulacaoSimplificadaDTO.class))),
        @APIResponse(responseCode = "400", description = "Parâmetros de paginação inválidos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })

    public Response listarSimulacoes(@Valid @BeanParam SimulacaoQueryParams parametrosConsulta, @Context UriInfo uriInfo) {
        logger.info("Listando simulações - Página: {}, Registros por página: {}",
                    parametrosConsulta.getPagina(), parametrosConsulta.getQtdRegistrosPagina());



        var paginaSimulacao = simulacaoService.listarSimulacoes(
            parametrosConsulta.getPagina(),
            parametrosConsulta.getQtdRegistrosPagina()
        );

        var simulacoes = paginaSimulacao.getRegistros();
        simulacoes.forEach(p -> p.addLink("detalhe", uriInfo.getBaseUriBuilder().path(SimulacaoResource.class).path(String.valueOf(p.getIdSimulacao())).build().toString()));
        paginaSimulacao.addLink("proximaPagina", uriInfo.getBaseUriBuilder().path(SimulacaoResource.class).queryParam("pagina",paginaSimulacao.getPagina() + 1).build().toString());
        if(paginaSimulacao.getPagina() > 1){
            paginaSimulacao.addLink("paginaAnterior", uriInfo.getBaseUriBuilder().path(SimulacaoResource.class).queryParam("pagina",paginaSimulacao.getPagina() - 1).build().toString());

        }

        var responseFiltered = fieldFilterUtil.filterFields(paginaSimulacao, parametrosConsulta.getCampos());
        return Response.status(206).entity(responseFiltered).build();
    }

    /**
     * Busca simulações separadas por produto e/ou data sem paginação.
     */
    @GET
    @Path("/por-produto-dia")
    @RateLimited(maxRequests = 30, timeWindowSeconds = 60)
    @Auditado(acao = "BUSCAR_SIMULACOES_PRODUTO_DATA", recurso = "SIMULACAO")
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
    public Response buscarSimulacoesPorProdutoEData(@Valid @BeanParam SimulacaoPorProdutoDiaQueryParams parametrosConsulta, @Context HttpHeaders headers, @Context UriInfo uriInfo) {
        var requestId = getOrGenerateRequestId(headers);

        logger.info("[requestId={}] Buscando simulações por produto e data - Data: {}, ProdutoId: {}",
                    requestId, parametrosConsulta.getData(), parametrosConsulta.getProdutoId());

        var simulacoes = simulacaoService.buscarSimulacoesPorProdutoEData(
            parametrosConsulta.getData(),
            parametrosConsulta.getProdutoId(),
            requestId
        );

        // Adicionar links HATEOAS para cada simulação
        simulacoes.getSimulacoes().forEach(simulacao -> {
            // Buscar IDs das simulações do produto para criar links
//            simulacao.addLink("self", uriInfo.getBaseUriBuilder()
//                .path(SimulacaoResource.class)
//                .path("por-produto-dia")
//                .queryParam("data", parametrosConsulta.getData())
//                .queryParam("produtoId", simulacao.getCodigoProduto())
//                .build().toString());

            simulacao.addLink("listarSimulacoes", uriInfo.getBaseUriBuilder()
                .path(SimulacaoResource.class)
                .build().toString());
        });

        // Link para a própria consulta
        simulacoes.addLink("self", uriInfo.getRequestUri().toString());

        // Link para listar todas as simulações
        simulacoes.addLink("listarSimulacoes", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .build().toString());

        logger.info("[requestId={}] Consulta concluída com sucesso - {} simulações retornadas",
                    requestId, simulacoes.getSimulacoes().size());

        var responseFiltered = fieldFilterUtil.filterFields(simulacoes, parametrosConsulta.getCampos());
        return Response.ok(responseFiltered).build();
    }

    /**
     * Busca uma simulação específica pelo seu ID.
     */
    @GET
    @Path("/{id}")
    @RateLimited(maxRequests = 100, timeWindowSeconds = 60)
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
                                       @Context HttpHeaders headers,
                                       @Context UriInfo uriInfo) {
        var requestId = getOrGenerateRequestId(headers);

        logger.info("[requestId={}] Buscando simulação por ID: {}", requestId, id);

        var simulacao = simulacaoService.buscarSimulacaoPorId(id, requestId);

        // Adicionar links "detalhe" para todas as parcelas nos resultados da simulação
        simulacao.getResultadosSimulacao().forEach(resultado -> {
            resultado.getParcelas().forEach(parcela -> {
                parcela.addLink("detalhe", uriInfo.getBaseUriBuilder()
                    .path(SimulacaoResource.class)
                    .path(String.valueOf(id))
                    .path(resultado.getTipo())
                    .path(String.valueOf(parcela.getNumero()))
                    .build().toString());
            });
        });

        // Adicionar links HATEOAS
        simulacao.addLink("self", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .build().toString());

        simulacao.addLink("parcelas-sac", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .path("SAC")
            .build().toString());

        simulacao.addLink("parcelas-price", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .path("PRICE")
            .build().toString());

        simulacao.addLink("listarSimulacoes", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .build().toString());

        logger.info("[requestId={}] Simulação encontrada com sucesso - ID: {}", requestId, id);

        var responseFiltered = fieldFilterUtil.filterFields(simulacao, campos);
        return Response.ok(responseFiltered).build();
    }

    /**
     * Busca todas as parcelas de um tipo específico de amortização para uma simulação.
     */
    @GET
    @Path("/{id}/{tipoAmortizacao}")
    @RateLimited(maxRequests = 80, timeWindowSeconds = 60)
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
                                                   @Context HttpHeaders headers,
                                                   @Context UriInfo uriInfo) {
        var requestId = getOrGenerateRequestId(headers);

        logger.info("[requestId={}] Buscando parcelas por tipo de amortização - SimulacaoId: {}, Tipo: {}",
                    requestId, id, tipoAmortizacao);

        var parcelas = simulacaoService.buscarParcelasPorTipoAmortizacao(id, tipoAmortizacao, requestId);

        // Adicionar links HATEOAS para cada parcela individual
        parcelas.getParcelas().forEach(parcela -> {
            parcela.addLink("detalhe", uriInfo.getBaseUriBuilder()
                .path(SimulacaoResource.class)
                .path(String.valueOf(id))
                .path(tipoAmortizacao)
                .path(String.valueOf(parcela.getNumero()))
                .build().toString());
        });

        // Adicionar links HATEOAS para o objeto principal
        parcelas.addLink("self", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .path(tipoAmortizacao)
            .build().toString());
        parcelas.addLink("simulacao", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .build().toString());

        // Adicionar link para o outro tipo de amortização
        String outroTipo = "SAC".equals(tipoAmortizacao) ? "PRICE" : "SAC";
        parcelas.addLink("parcelas-" + outroTipo.toLowerCase(), uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .path(outroTipo)
            .build().toString());

        parcelas.addLink("listarSimulacoes", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .build().toString());

        logger.info("[requestId={}] Parcelas encontradas com sucesso - SimulacaoId: {}, Tipo: {}, Quantidade: {}",
                    requestId, id, tipoAmortizacao, parcelas.getQuantidadeParcelas());

        var responseFiltered = fieldFilterUtil.filterFields(parcelas, campos);
        return Response.ok(responseFiltered).build();
    }

    /**
     * Busca informações detalhadas de uma parcela específica.
     */
    @GET
    @Path("/{id}/{tipoAmortizacao}/{parcelaId}")
    @RateLimited(maxRequests = 120, timeWindowSeconds = 60)
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
                                          @Context HttpHeaders headers,
                                          @Context UriInfo uriInfo) {
        var requestId = getOrGenerateRequestId(headers);

        logger.info("[requestId={}] Buscando parcela específica - SimulacaoId: {}, Tipo: {}, ParcelaId: {}",
                    requestId, id, tipoAmortizacao, parcelaId);

        var parcela = simulacaoService.buscarParcelaEspecifica(id, tipoAmortizacao, parcelaId, requestId);

        // Adicionar links HATEOAS
        parcela.addLink("self", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .path(tipoAmortizacao)
            .path(String.valueOf(parcelaId))
            .build().toString());

        parcela.addLink("todasParcelas", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .path(tipoAmortizacao)
            .build().toString());

        parcela.addLink("simulacao", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .build().toString());

        // Links para parcelas adjacentes (anterior e próxima)
        if (parcelaId > 1) {
            parcela.addLink("parcelaAnterior", uriInfo.getBaseUriBuilder()
                .path(SimulacaoResource.class)
                .path(String.valueOf(id))
                .path(tipoAmortizacao)
                .path(String.valueOf(parcelaId - 1))
                .build().toString());
        }

        // Assumindo que não temos o total de parcelas aqui, mas poderíamos adicionar
        // um link para a próxima parcela se soubermos que existe
        parcela.addLink("proximaParcela", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .path(tipoAmortizacao)
            .path(String.valueOf(parcelaId + 1))
            .build().toString());

        // Adicionar link para o outro tipo de amortização da mesma parcela
        String outroTipo = "SAC".equals(tipoAmortizacao) ? "PRICE" : "SAC";
        parcela.addLink("parcela-" + outroTipo.toLowerCase(), uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .path(String.valueOf(id))
            .path(outroTipo)
            .path(String.valueOf(parcelaId))
            .build().toString());

        parcela.addLink("listarSimulacoes", uriInfo.getBaseUriBuilder()
            .path(SimulacaoResource.class)
            .build().toString());

        logger.info("[requestId={}] Parcela específica encontrada - SimulacaoId: {}, Tipo: {}, Parcela: {}, Valor: R$ {}",
                    requestId, id, tipoAmortizacao, parcelaId, parcela.getValorPrestacao());

        var responseFiltered = fieldFilterUtil.filterFields(parcela, campos);
        return Response.ok(responseFiltered).build();
    }

    private String getOrGenerateRequestId(HttpHeaders headers) {
        var headerId = headers.getHeaderString("X-Request-ID");
        return (headerId != null && !headerId.isBlank()) ? headerId : UUID.randomUUID().toString();
    }
}
