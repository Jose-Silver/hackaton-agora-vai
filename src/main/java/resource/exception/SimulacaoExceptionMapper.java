package resource.exception;

import domain.dto.ErrorResponseDTO;
import domain.exception.SimulacaoException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * ExceptionMapper específico para SimulacaoException.
 * Fornece tratamento especializado para erros relacionados a simulações.
 */
@Provider
public class SimulacaoExceptionMapper implements ExceptionMapper<SimulacaoException> {

    private static final Logger LOG = Logger.getLogger(SimulacaoExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(SimulacaoException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : null;
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            exception.getCodigo(),
            exception.getMensagemErro().getMensagem(),
            exception.getDetalhe(),
            exception.getHttpStatus(),
            path
        );

        // Log específico para simulações
        LOG.warnf("Erro na simulação: %s - Path: %s - Detalhe: %s", 
            exception.getMensagemErro().getCodigo(),
            path, 
            exception.getDetalhe());

        return Response.status(exception.getHttpStatus())
            .entity(errorResponse)
            .build();
    }
}
