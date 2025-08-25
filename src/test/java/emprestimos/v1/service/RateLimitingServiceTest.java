package emprestimos.v1.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RateLimitingServiceTest {

    @Inject
    RateLimitingService rateLimitingService;

    @Test
    void testRateLimitingAllowed() {
        // Teste com requisições dentro do limite
        String key = "test-key-1";
        int maxRequests = 5;
        int timeWindow = 60;

        // Primeiras 5 requisições devem ser permitidas
        for (int i = 1; i <= 5; i++) {
            RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(key, maxRequests, timeWindow);

            assertTrue(result.isAllowed(), "Requisição " + i + " deveria ser permitida");
            assertEquals(i, result.getCurrentCount(), "Contador deveria ser " + i);
            assertEquals(maxRequests, result.getMaxRequests(), "Limite máximo incorreto");
            assertEquals(maxRequests - i, result.getRemainingRequests(), "Requisições restantes incorretas");
            assertTrue(result.getRetryAfterSeconds() > 0, "Retry after deveria ser positivo");
        }
    }

    @Test
    void testRateLimitingBlocked() {
        // Teste com requisições que excedem o limite
        String key = "test-key-2";
        int maxRequests = 3;
        int timeWindow = 60;

        // Primeiras 3 requisições permitidas
        for (int i = 1; i <= 3; i++) {
            RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(key, maxRequests, timeWindow);
            assertTrue(result.isAllowed(), "Requisição " + i + " deveria ser permitida");
        }

        // 4ª requisição deve ser bloqueada
        RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(key, maxRequests, timeWindow);
        assertFalse(result.isAllowed(), "4ª requisição deveria ser bloqueada");
        assertEquals(4, result.getCurrentCount(), "Contador deveria ser 4");
        assertEquals(0, result.getRemainingRequests(), "Não deveria ter requisições restantes");
        assertTrue(result.getRetryAfterSeconds() > 0, "Deveria ter tempo de retry");
    }

    @Test
    void testDifferentKeysIndependent() {
        // Teste que chaves diferentes têm contadores independentes
        String key1 = "test-key-3";
        String key2 = "test-key-4";
        int maxRequests = 2;
        int timeWindow = 60;

        // Usar todo o limite da primeira chave
        for (int i = 1; i <= 2; i++) {
            RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(key1, maxRequests, timeWindow);
            assertTrue(result.isAllowed());
        }

        // Terceira requisição na primeira chave deve ser bloqueada
        RateLimitingService.RateLimitResult result1 = rateLimitingService.checkRateLimit(key1, maxRequests, timeWindow);
        assertFalse(result1.isAllowed());

        // Segunda chave deve ainda permitir requisições
        RateLimitingService.RateLimitResult result2 = rateLimitingService.checkRateLimit(key2, maxRequests, timeWindow);
        assertTrue(result2.isAllowed());
        assertEquals(1, result2.getCurrentCount());
    }

    @Test
    void testRateLimitResultBuilder() {
        // Teste do builder pattern
        RateLimitingService.RateLimitResult result = RateLimitingService.RateLimitResult.builder()
                .allowed(true)
                .currentCount(5)
                .maxRequests(10)
                .remainingRequests(5)
                .resetTime(System.currentTimeMillis() / 1000 + 60)
                .retryAfterSeconds(0)
                .build();

        assertTrue(result.isAllowed());
        assertEquals(5, result.getCurrentCount());
        assertEquals(10, result.getMaxRequests());
        assertEquals(5, result.getRemainingRequests());
        assertTrue(result.getResetTime() > 0);
        assertEquals(0, result.getRetryAfterSeconds());
    }
}
