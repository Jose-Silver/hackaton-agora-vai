package emprestimos.v1.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.jbosslog.JBossLog;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Serviço responsável por gerenciar rate limiting usando cache local
 */
@ApplicationScoped
@Slf4j
public class RateLimitingService {

    // Cache para armazenar contadores de rate limit
    private final ConcurrentMap<String, RateLimitCounter> rateLimitCache = new ConcurrentHashMap<>();

    /**
     * Verifica se a requisição está dentro do limite permitido
     *
     * @param key Chave identificadora (ex: IP, user ID)
     * @param maxRequests Número máximo de requisições
     * @param timeWindowSeconds Janela de tempo em segundos
     * @return RateLimitResult contendo informações sobre o limite
     */
    public RateLimitResult checkRateLimit(String key, int maxRequests, int timeWindowSeconds) {
        long currentTime = Instant.now().getEpochSecond();

        String cacheKey = "rate_limit:" + key + ":" + (currentTime / timeWindowSeconds);

        try {
            RateLimitCounter counter = rateLimitCache.compute(cacheKey, (k, v) -> {
                if (v == null) {
                    return new RateLimitCounter(1, currentTime + timeWindowSeconds);
                } else if (v.expiresAt <= currentTime) {
                    return new RateLimitCounter(1, currentTime + timeWindowSeconds);
                } else {
                    return new RateLimitCounter(v.count + 1, v.expiresAt);
                }
            });

            // Limpa contadores expirados periodicamente
            cleanExpiredCounters(currentTime);

            boolean allowed = counter.count <= maxRequests;
            long remainingRequests = Math.max(0, maxRequests - counter.count);
            long resetTime = counter.expiresAt;
            long retryAfter = resetTime - currentTime;
            
            log.info("Rate limit check - Key: %s, Current: %d, Max: %d, Allowed: %s, Retry after: %d",
                     key, counter.count, maxRequests, allowed, retryAfter);

            return RateLimitResult.builder()
                    .allowed(allowed)
                    .currentCount(counter.count)
                    .maxRequests(maxRequests)
                    .remainingRequests(remainingRequests)
                    .resetTime(resetTime)
                    .retryAfterSeconds(retryAfter)
                    .build();
                    
        } catch (Exception e) {
            log.error(e.getMessage(), "Erro ao verificar rate limit para key: %s", key);
            // Em caso de erro, permite a requisição
            return RateLimitResult.builder()
                    .allowed(true)
                    .currentCount(1L)
                    .maxRequests(maxRequests)
                    .remainingRequests(maxRequests - 1L)
                    .resetTime(currentTime + timeWindowSeconds)
                    .retryAfterSeconds(0L)
                    .build();
        }
    }

    private void cleanExpiredCounters(long currentTime) {
        // Remove contadores expirados a cada 100 verificações para não impactar performance
        if (currentTime % 100 == 0) {
            rateLimitCache.entrySet().removeIf(entry -> entry.getValue().expiresAt <= currentTime);
        }
    }

    /**
     * Classe para armazenar contador e tempo de expiração
     */
    private static class RateLimitCounter {
        final long count;
        final long expiresAt;

        RateLimitCounter(long count, long expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * Classe para representar o resultado da verificação de rate limit
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final long currentCount;
        private final long maxRequests;
        private final long remainingRequests;
        private final long resetTime;
        private final long retryAfterSeconds;

        private RateLimitResult(Builder builder) {
            this.allowed = builder.allowed;
            this.currentCount = builder.currentCount;
            this.maxRequests = builder.maxRequests;
            this.remainingRequests = builder.remainingRequests;
            this.resetTime = builder.resetTime;
            this.retryAfterSeconds = builder.retryAfterSeconds;
        }

        public boolean isAllowed() { return allowed; }
        public long getCurrentCount() { return currentCount; }
        public long getMaxRequests() { return maxRequests; }
        public long getRemainingRequests() { return remainingRequests; }
        public long getResetTime() { return resetTime; }
        public long getRetryAfterSeconds() { return retryAfterSeconds; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean allowed;
            private long currentCount;
            private long maxRequests;
            private long remainingRequests;
            private long resetTime;
            private long retryAfterSeconds;

            public Builder allowed(boolean allowed) { this.allowed = allowed; return this; }
            public Builder currentCount(long currentCount) { this.currentCount = currentCount; return this; }
            public Builder maxRequests(long maxRequests) { this.maxRequests = maxRequests; return this; }
            public Builder remainingRequests(long remainingRequests) { this.remainingRequests = remainingRequests; return this; }
            public Builder resetTime(long resetTime) { this.resetTime = resetTime; return this; }
            public Builder retryAfterSeconds(long retryAfterSeconds) { this.retryAfterSeconds = retryAfterSeconds; return this; }

            public RateLimitResult build() {
                return new RateLimitResult(this);
            }
        }
    }
}
