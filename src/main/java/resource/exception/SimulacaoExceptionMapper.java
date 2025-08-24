package resource.exception;

import domain.dto.common.ErrorResponseDTO;
import domain.exception.SimulacaoException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Exception mapper para tratar exceções relacionadas a simulações.
 * Mapeia SimulacaoException para respostas HTTP apropriadas.
 */
@Provider
public class SimulacaoExceptionMapper implements ExceptionMapper<SimulacaoException> {

    private static final Logger logger = Logger.getLogger(SimulacaoExceptionMapper.class);

    @Override
    public Response toResponse(SimulacaoException exception) {
        logger.warnf("Exceção de simulação capturada: %s - %s", exception.getTitulo(), exception.getDetalhe());

        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setCodigo("SIMULACAO_NAO_ENCONTRADA");
        errorResponse.setMensagem(exception.getTitulo());
        errorResponse.setDetalhe(exception.getDetalhe());
        errorResponse.setStatus(404);

        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse)
                .build();
    }
}
