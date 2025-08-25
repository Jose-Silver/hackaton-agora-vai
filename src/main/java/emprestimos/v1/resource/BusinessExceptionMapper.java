package emprestimos.v1.resource;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.format.DateTimeParseException;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof IllegalArgumentException) {
            String msg = exception.getMessage();
            if (msg != null && msg.contains("Produto não encontrado")) {
                return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
            }
            return Response.status(422).entity(msg).build();
        }
        if (exception instanceof DateTimeParseException) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Data inválida").build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Erro inesperado: " + exception.getMessage()).build();
    }
}

