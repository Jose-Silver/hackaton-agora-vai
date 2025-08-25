package emprestimos.v1.resource.exception;

import emprestimos.v1.domain.dto.common.ErrorResponseDTO;
import emprestimos.v1.domain.enums.MensagemErro;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * ExceptionMapper específico para ConstraintViolationException.
 * Trata erros de validação do Bean Validation (@Valid, @NotNull, etc.).
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = Logger.getLogger(ValidationExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : null;
        
        List<ErrorResponseDTO.CampoErroDTO> errosCampos = new ArrayList<>();
        
        // Processa cada violação de constraint
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String campo = extrairNomeCampo(violation.getPropertyPath().toString());
            String mensagem = violation.getMessage();
            Object valorRejeitado = violation.getInvalidValue();
            errosCampos.add(new ErrorResponseDTO.CampoErroDTO(campo, mensagem, valorRejeitado));
        }

        // Concatenate all field messages for the top-level 'mensagem' field
        StringBuilder mensagemBuilder = new StringBuilder();
        for (int i = 0; i < errosCampos.size(); i++) {
            ErrorResponseDTO.CampoErroDTO erro = errosCampos.get(i);
            mensagemBuilder.append(erro.getCampo()).append(": ").append(erro.getMensagem());
            if (i < errosCampos.size() - 1) {
                mensagemBuilder.append("; ");
            }
        }
        String mensagemFinal = mensagemBuilder.length() > 0 ? mensagemBuilder.toString() : MensagemErro.VALIDACAO_GENERICA.getMensagem();

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            MensagemErro.VALIDACAO_GENERICA.getCodigo(),
            mensagemFinal,
            "Um ou mais campos contêm valores inválidos",
            MensagemErro.VALIDACAO_GENERICA.getHttpStatus(),
            path
        );
        
        errorResponse.setErros(errosCampos);

        // Log das violações
        LOG.warnf("Erro de validação: %d violações encontradas - Path: %s", 
            errosCampos.size(), path);

        return Response.status(Response.Status.BAD_REQUEST)
            .entity(errorResponse)
            .build();
    }

    /**
     * Extrai o nome do campo do path da propriedade.
     */
    private String extrairNomeCampo(String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return "campo";
        }
        
        // Pega apenas o último segmento do path (nome do campo)
        String[] segments = propertyPath.split("\\.");
        return segments[segments.length - 1];
    }
}
