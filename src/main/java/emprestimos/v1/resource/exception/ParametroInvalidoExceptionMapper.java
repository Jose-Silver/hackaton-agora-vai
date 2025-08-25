package emprestimos.v1.resource.exception;

import emprestimos.v1.domain.dto.common.ErrorResponseDTO;
import emprestimos.v1.domain.exception.ParametroInvalidoException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * ExceptionMapper específico para ParametroInvalidoException.
 * Fornece tratamento especializado para erros de validação de parâmetros.
 */
@Provider
public class ParametroInvalidoExceptionMapper implements ExceptionMapper<ParametroInvalidoException> {

    private static final Logger LOG = Logger.getLogger(ParametroInvalidoExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ParametroInvalidoException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : null;
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            exception.getCodigo(),
            exception.getMensagemErro().getMensagem(),
            exception.getDetalhe(),
            exception.getHttpStatus(),
            path
        );

        // Log específico para parâmetros inválidos
        LOG.warnf("Parâmetro inválido: %s - Path: %s - Detalhe: %s", 
            exception.getMensagemErro().getCodigo(),
            path, 
            exception.getDetalhe());

        return Response.status(exception.getHttpStatus())
            .entity(errorResponse)
            .build();
    }
}
