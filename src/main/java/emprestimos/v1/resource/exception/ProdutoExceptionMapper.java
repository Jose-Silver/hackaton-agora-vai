package emprestimos.v1.resource.exception;


import emprestimos.v1.domain.dto.common.ErrorResponseDTO;
import emprestimos.v1.domain.exception.ProdutoException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExceptionMapper específico para ProdutoException.
 * Fornece tratamento especializado para erros relacionados a produtos financeiros.
 */
@Provider
public class ProdutoExceptionMapper implements ExceptionMapper<ProdutoException> {

    private static final Logger LOG = LoggerFactory.getLogger(ProdutoExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ProdutoException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : null;

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            exception.getCodigo(),
            exception.getMensagemErro().getMensagem(),
            exception.getDetalhe(),
            exception.getHttpStatus(),
            path
        );

        // Log específico para produtos
        LOG.warn("Erro relacionado a produto: {} - Path: {} - Detalhe: {}",
            exception.getMensagemErro().getCodigo(),
            path,
            exception.getDetalhe());

        return Response.status(exception.getHttpStatus())
            .entity(errorResponse)
            .build();
    }
}
