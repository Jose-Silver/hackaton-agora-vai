package emprestimos.v1.config;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para aplicar rate limiting em endpoints
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    
    /**
     * Número máximo de requisições permitidas no período
     */
    int maxRequests() default 100;
    
    /**
     * Janela de tempo em segundos
     */
    int timeWindowSeconds() default 60;
    
    /**
     * Chave customizada para agrupamento (opcional)
     */
    String key() default "";
}
