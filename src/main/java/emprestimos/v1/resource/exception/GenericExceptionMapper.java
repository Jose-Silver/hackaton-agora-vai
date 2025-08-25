package emprestimos.v1.resource.exception;

import emprestimos.v1.domain.dto.common.ErrorResponseDTO;
import emprestimos.v1.domain.enums.MensagemErro;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * ExceptionMapper genérico para capturar todas as exceções não tratadas.
 * Serve como fallback para erros inesperados do sistema.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        String path = uriInfo != null ? uriInfo.getPath() : null;

        // Tratar especificamente NotAllowedException para manter o status correto
        if (exception instanceof NotAllowedException) {
            LOG.warnf("Método HTTP não permitido: %s - Path: %s", exception.getMessage(), path);

            ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                "METHOD_NOT_ALLOWED",
                "Método HTTP não permitido para este endpoint",
                exception.getMessage(),
                405,
                path
            );

            return Response.status(Response.Status.METHOD_NOT_ALLOWED)
                .entity(errorResponse)
                .build();
        }

        // Unwrap and delegate ConstraintViolationException in the cause chain
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof jakarta.validation.ConstraintViolationException) {
                throw (jakarta.validation.ConstraintViolationException) cause;
            }
            cause = cause.getCause();
        }

        // Log completo do erro para investigação
        LOG.errorf(exception, "Erro não tratado capturado: %s - Path: %s",
            exception.getClass().getSimpleName(), path);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            MensagemErro.ERRO_INTERNO.getCodigo(),
            MensagemErro.ERRO_INTERNO.getMensagem(),
            "Erro interno do sistema",
            MensagemErro.ERRO_INTERNO.getHttpStatus(),
            path
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(errorResponse)
            .build();
    }
}
