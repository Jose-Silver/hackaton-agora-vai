package emprestimos.v1.config;

import emprestimos.v1.service.RateLimitingService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Interceptor que aplica rate limiting aos endpoints anotados com @RateLimited
 */
@Interceptor
@RateLimited
@Priority(1000)
@Slf4j
public class RateLimitingInterceptor {

    @Inject
    RateLimitingService rateLimitingService;

    @Context
    ContainerRequestContext requestContext;

    @ConfigProperty(name = "emprestimos.rate-limit.enabled", defaultValue = "true")
    boolean rateLimitEnabled;

    @AroundInvoke
    public Object applyRateLimit(InvocationContext context) throws Exception {
        // Se desativado por configuração, apenas segue o fluxo normal
        if (!rateLimitEnabled) {
            return context.proceed();
        }
        RateLimited rateLimited = getRateLimitedAnnotation(context);
        
        if (rateLimited == null) {
            return context.proceed();
        }

        String clientKey = getClientKey(rateLimited);
        
        RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(
            clientKey,
            rateLimited.maxRequests(),
            rateLimited.timeWindowSeconds()
        );

        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded for key: %s. Current: %d, Max: %d, Retry after: %d seconds",
                     clientKey, result.getCurrentCount(), result.getMaxRequests(), result.getRetryAfterSeconds());
            
            return Response.status(429, "Too Many Requests")
                    .header("X-RateLimit-Limit", result.getMaxRequests())
                    .header("X-RateLimit-Remaining", 0)
                    .header("X-RateLimit-Reset", result.getResetTime())
                    .header("Retry-After", result.getRetryAfterSeconds())
                    .entity("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\",\"retryAfter\":" + result.getRetryAfterSeconds() + "}")
                    .build();
        }

        // Adiciona headers informativos sobre rate limit na resposta
        Object response = context.proceed();
        
        if (response instanceof Response.ResponseBuilder) {
            Response.ResponseBuilder builder = (Response.ResponseBuilder) response;
            return builder
                    .header("X-RateLimit-Limit", result.getMaxRequests())
                    .header("X-RateLimit-Remaining", result.getRemainingRequests())
                    .header("X-RateLimit-Reset", result.getResetTime())
                    .build();
        } else if (response instanceof Response) {
            Response resp = (Response) response;
            return Response.fromResponse(resp)
                    .header("X-RateLimit-Limit", result.getMaxRequests())
                    .header("X-RateLimit-Remaining", result.getRemainingRequests())
                    .header("X-RateLimit-Reset", result.getResetTime())
                    .build();
        }

        return response;
    }

    private RateLimited getRateLimitedAnnotation(InvocationContext context) {
        // Primeiro verifica no método
        RateLimited methodAnnotation = context.getMethod().getAnnotation(RateLimited.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        
        // Se não encontrar no método, verifica na classe
        return context.getTarget().getClass().getAnnotation(RateLimited.class);
    }

    private String getClientKey(RateLimited rateLimited) {
        // Se uma chave customizada foi definida, usa ela
        if (!rateLimited.key().isEmpty()) {
            return rateLimited.key();
        }
        
        // Caso contrário, usa o IP do cliente
        String clientIp = getClientIpAddress();
        return "ip:" + clientIp;
    }

    private String getClientIpAddress() {
        try {
            if (requestContext != null) {
                // Verifica headers de proxy primeiro
                String xForwardedFor = requestContext.getHeaderString("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIp = requestContext.getHeaderString("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
            }
            
            return "unknown";
        } catch (Exception e) {
            log.warn(e.getMessage(), "Erro ao obter IP do cliente");
            return "unknown";
        }
    }
}
