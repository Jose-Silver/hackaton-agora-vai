package emprestimos.v1.domain.enums;

/**
 * Enum para constantes de configuração do sistema.
 */
public enum SystemConstant {
    // Headers HTTP
    REQUEST_ID_HEADER("X-Request-ID"),

    // Configurações de retry
    MAX_RETRY_ATTEMPTS(3),
    RETRY_DELAY_MS(1000),

    // Configurações de cache
    CACHE_TIMEOUT_MS(300000), // 5 minutos

    // Telemetria
    TELEMETRY_START_TIME("telemetryStartTime");

    private final Object valor;

    SystemConstant(Object valor) {
        this.valor = valor;
    }

    public String getStringValue() {
        return valor.toString();
    }

    public int getIntValue() {
        return (Integer) valor;
    }

    public long getLongValue() {
        if (valor instanceof Integer) {
            return ((Integer) valor).longValue();
        }
        return (Long) valor;
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
