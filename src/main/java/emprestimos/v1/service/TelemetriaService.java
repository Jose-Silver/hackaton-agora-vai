package emprestimos.v1.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class TelemetriaService {
    public static class EndpointStats {
        public final AtomicInteger qtdRequisicoes = new AtomicInteger();
        public final AtomicInteger qtdSucesso = new AtomicInteger();
        public final AtomicLong tempoTotal = new AtomicLong();
        public final AtomicLong tempoMinimo = new AtomicLong(Long.MAX_VALUE);
        public final AtomicLong tempoMaximo = new AtomicLong(Long.MIN_VALUE);
    }

    private final Map<String, EndpointStats> statsMap = new ConcurrentHashMap<>();

    public void record(String endpoint, long durationMillis, boolean sucesso) {
        EndpointStats stats = statsMap.computeIfAbsent(endpoint, k -> new EndpointStats());
        stats.qtdRequisicoes.incrementAndGet();
        if (sucesso) stats.qtdSucesso.incrementAndGet();
        stats.tempoTotal.addAndGet(durationMillis);
        stats.tempoMinimo.getAndUpdate(prev -> Math.min(prev, durationMillis));
        stats.tempoMaximo.getAndUpdate(prev -> Math.max(prev, durationMillis));
    }

    public Map<String, EndpointStats> getStats() {
        return statsMap;
    }

    public java.util.List<java.util.Map<String, Object>> getFormattedStats() {
        java.util.List<java.util.Map<String, Object>> listaEndpoints = new java.util.ArrayList<>();
        for (Map.Entry<String, EndpointStats> entry : statsMap.entrySet()) {
            String key = entry.getKey(); // e.g., "GET /simulacao/por-produto-dia"
            String metodo = "";
            String path = "";
            int idx = key.indexOf(' ');
            if (idx > 0) {
                metodo = key.substring(0, idx);
                path = key.substring(idx + 1);
            } else {
                metodo = key;
            }
            // Extract nomeApi: first segment of path, first letter uppercase
            String nomeApi = "";
            if (!path.isEmpty()) {
                String[] parts = path.split("/");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        nomeApi = Character.toUpperCase(part.charAt(0)) + part.substring(1);
                        break;
                    }
                }
            }
            EndpointStats stats = entry.getValue();
            int qtd = stats.qtdRequisicoes.get();
            int qtdSucesso = stats.qtdSucesso.get();
            long tempoTotal = stats.tempoTotal.get();
            long tempoMin = stats.tempoMinimo.get() == Long.MAX_VALUE ? 0 : stats.tempoMinimo.get();
            long tempoMax = stats.tempoMaximo.get() == Long.MIN_VALUE ? 0 : stats.tempoMaximo.get();
            double percentualSucesso = qtd == 0 ? 0.0 : ((double) qtdSucesso) / qtd;
            long tempoMedio = qtd == 0 ? 0 : tempoTotal / qtd;
            LinkedHashMap<String, Object> endpointData = new LinkedHashMap<>();
            endpointData.put("nomeApi", nomeApi);
            endpointData.put("metodo", metodo);
            endpointData.put("path", path);
            endpointData.put("qtdRequisicoes", qtd);
            endpointData.put("tempoMedio", tempoMedio);
            endpointData.put("tempoMinimo", tempoMin);
            endpointData.put("tempoMaximo", tempoMax);
            endpointData.put("percentualSucesso", percentualSucesso);
            listaEndpoints.add(endpointData);
        }
        return listaEndpoints;
    }
}
