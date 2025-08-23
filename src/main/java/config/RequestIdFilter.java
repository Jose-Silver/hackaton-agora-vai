package config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Filtro que gerencia automaticamente o Request-ID para todas as requisições.
 */
@Provider
@Priority(1000)
public class RequestIdFilter implements ContainerRequestFilter {
    
    private static final Logger LOG = Logger.getLogger(RequestIdFilter.class);
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(REQUEST_ID_HEADER);
        
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            requestContext.getHeaders().putSingle(REQUEST_ID_HEADER, requestId);
        }
        
        LOG.debugf("RequestId: %s for %s %s", requestId,
                  requestContext.getMethod(),
                  requestContext.getUriInfo().getPath());
    }
}
