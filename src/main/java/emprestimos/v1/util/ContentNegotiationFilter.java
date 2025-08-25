package emprestimos.v1.util;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

@Provider
public class ContentNegotiationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String HEADER_ACCEPT = "Accept";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String acceptHeader = requestContext.getHeaderString(HEADER_ACCEPT);

        // Se não veio Accept -> default JSON
        if (acceptHeader == null || acceptHeader.isBlank()) {
            requestContext.getHeaders().putSingle(HEADER_ACCEPT, MediaType.APPLICATION_JSON);
            return;
        }

        String normalized = acceptHeader.toLowerCase();

        if (normalized.contains(MediaType.APPLICATION_JSON)) {
            requestContext.getHeaders().putSingle(HEADER_ACCEPT, MediaType.APPLICATION_JSON);

        } else if (normalized.contains(MediaType.APPLICATION_XML) || normalized.contains(MediaType.TEXT_XML)) {
            requestContext.getHeaders().putSingle(HEADER_ACCEPT, MediaType.APPLICATION_XML);

        } else if (acceptHeader.contains("*/*")){;
            // Aceita qualquer tipo -> default JSONs
            requestContext.getHeaders().putSingle(HEADER_ACCEPT, MediaType.APPLICATION_JSON);
        } else {
            // Cabeçalho inválido → aborta requisição com 415
            requestContext.abortWith(
                    Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                            .entity("Tipo não suportado: " + acceptHeader)
                            .type(MediaType.TEXT_PLAIN)
                            .build()
            );
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        // Garante que a resposta traga o Content-Type correto
        String acceptHeader = requestContext.getHeaderString(HEADER_ACCEPT);

        if (acceptHeader != null) {
            responseContext.getHeaders().putSingle("Content-Type", acceptHeader);
        }
    }
}
