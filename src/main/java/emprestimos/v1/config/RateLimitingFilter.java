package emprestimos.v1.config;

import emprestimos.v1.service.RateLimitingService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Filtro JAX-RS que aplica rate limiting aos endpoints anotados com @RateLimited
 */
@Provider
@Priority(1000)
@Slf4j
public class RateLimitingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Inject
    RateLimitingService rateLimitingService;

    @Context
    ResourceInfo resourceInfo;

    @ConfigProperty(name = "emprestimos.rate-limit.enabled", defaultValue = "true")
    boolean rateLimitEnabled;

    private static final String RATE_LIMIT_RESULT_PROPERTY = "rate.limit.result";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Se desativado por configuração, não aplica rate limiting
        if (!rateLimitEnabled) {
            return;
        }
        RateLimited rateLimited = getRateLimitedAnnotation();
        
        if (rateLimited == null) {
            return;
        }

        String clientKey = getClientKey(requestContext, rateLimited);
        
        RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(
            clientKey,
            rateLimited.maxRequests(),
            rateLimited.timeWindowSeconds()
        );

        // Armazena o resultado para uso no response filter
        requestContext.setProperty(RATE_LIMIT_RESULT_PROPERTY, result);

        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded for key: {}. Current: {}, Max: {}, Retry after: {} seconds",
                     clientKey, result.getCurrentCount(), result.getMaxRequests(), result.getRetryAfterSeconds());
            
            Response response = Response.status(429, "Too Many Requests")
                    .header("X-RateLimit-Limit", result.getMaxRequests())
                    .header("X-RateLimit-Remaining", 0)
                    .header("X-RateLimit-Reset", result.getResetTime())
                    .header("Retry-After", result.getRetryAfterSeconds())
                    .header("Content-Type", "application/json")
                    .entity("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\",\"retryAfter\":" + result.getRetryAfterSeconds() + "}")
                    .build();
            
            requestContext.abortWith(response);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Se desativado por configuração, não adiciona headers informativos
        if (!rateLimitEnabled) {
            return;
        }
        // Adiciona headers informativos sobre rate limit na resposta de sucesso
        RateLimitingService.RateLimitResult result = 
            (RateLimitingService.RateLimitResult) requestContext.getProperty(RATE_LIMIT_RESULT_PROPERTY);
        
        if (result != null && result.isAllowed()) {
            responseContext.getHeaders().add("X-RateLimit-Limit", result.getMaxRequests());
            responseContext.getHeaders().add("X-RateLimit-Remaining", result.getRemainingRequests());
            responseContext.getHeaders().add("X-RateLimit-Reset", result.getResetTime());
        }
    }

    private RateLimited getRateLimitedAnnotation() {
        if (resourceInfo == null) {
            return null;
        }

        Method method = resourceInfo.getResourceMethod();
        Class<?> resourceClass = resourceInfo.getResourceClass();
        
        // Primeiro verifica no método
        if (method != null && method.isAnnotationPresent(RateLimited.class)) {
            return method.getAnnotation(RateLimited.class);
        }
        
        // Se não encontrar no método, verifica na classe
        if (resourceClass != null && resourceClass.isAnnotationPresent(RateLimited.class)) {
            return resourceClass.getAnnotation(RateLimited.class);
        }
        
        return null;
    }

    private String getClientKey(ContainerRequestContext requestContext, RateLimited rateLimited) {
        // Se uma chave customizada foi definida, usa ela
        if (!rateLimited.key().isEmpty()) {
            return rateLimited.key();
        }
        
        // Caso contrário, usa o IP do cliente
        String clientIp = getClientIpAddress(requestContext);
        return "ip:" + clientIp;
    }

    private String getClientIpAddress(ContainerRequestContext requestContext) {
        try {
            // Verifica headers de proxy primeiro
            String xForwardedFor = requestContext.getHeaderString("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = requestContext.getHeaderString("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            // Tenta obter do contexto da requisição
            String remoteAddr = requestContext.getHeaderString("Remote-Addr");
            if (remoteAddr != null && !remoteAddr.isEmpty()) {
                return remoteAddr;
            }
            
            return "127.0.0.1"; // fallback para localhost
        } catch (Exception e) {
            log.warn("Erro ao obter IP do cliente", e);
            return "127.0.0.1";
        }
    }
}
