package resource.exception;

import domain.dto.ErrorResponseDTO;
import domain.exception.ProdutoException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * ExceptionMapper específico para ProdutoException.
 * Fornece tratamento especializado para erros relacionados a produtos financeiros.
 */
@Provider
public class ProdutoExceptionMapper implements ExceptionMapper<ProdutoException> {

    private static final Logger LOG = Logger.getLogger(ProdutoExceptionMapper.class);

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
        LOG.warnf("Erro relacionado a produto: %s - Path: %s - Detalhe: %s",
            exception.getMensagemErro().getCodigo(),
            path,
            exception.getDetalhe());

        return Response.status(exception.getHttpStatus())
            .entity(errorResponse)
            .build();
    }
}
