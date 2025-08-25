package emprestimos.v1.resource.exception;

import emprestimos.v1.domain.dto.common.ErrorResponseDTO;
import emprestimos.v1.domain.exception.BusinessException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * ExceptionMapper genérico para BusinessException e suas subclasses.
 * Responsável por converter exceções de negócio em respostas HTTP padronizadas.
 */
@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

    private static final Logger LOG = Logger.getLogger(BusinessExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(BusinessException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : null;
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            exception.getCodigo(),
            exception.getMensagemErro().getMensagem(),
            exception.getDetalhe(),
            exception.getHttpStatus(),
            path
        );

        // Log do erro para auditoria
        LOG.warnf("BusinessException capturada: %s - Path: %s - Detalhe: %s", 
            exception.getMensagemErro().getDescricaoCompleta(), 
            path, 
            exception.getDetalhe());

        return Response.status(exception.getHttpStatus())
            .entity(errorResponse)
            .build();
    }
}
