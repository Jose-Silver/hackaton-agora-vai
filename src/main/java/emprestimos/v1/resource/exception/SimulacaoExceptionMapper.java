package emprestimos.v1.resource.exception;

import emprestimos.v1.domain.dto.common.ErrorResponseDTO;
import emprestimos.v1.domain.exception.SimulacaoException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception mapper para tratar exceções relacionadas a simulações.
 * Mapeia SimulacaoException para respostas HTTP apropriadas.
 */
@Provider
public class SimulacaoExceptionMapper implements ExceptionMapper<SimulacaoException> {

    private static final Logger logger = LoggerFactory.getLogger(SimulacaoExceptionMapper.class);

    @Override
    public Response toResponse(SimulacaoException exception) {
        logger.warn("Exceção de simulação capturada: {} - {}", exception.getTitulo(), exception.getDetalhe());

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
