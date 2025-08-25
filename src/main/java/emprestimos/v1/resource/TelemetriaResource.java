package emprestimos.v1.resource;

import emprestimos.v1.service.TelemetriaService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Path("/v1/telemetria")
@Produces(MediaType.APPLICATION_JSON)
public class TelemetriaResource {
    @Inject
    TelemetriaService telemetriaService;

    @GET
    @Path("/detalhes")
    public Map<String, Object> getTelemetria() {
        Map<String, Object> result = new HashMap<>();
        result.put("dataReferencia", LocalDate.now().toString());
        result.put("listaEndpoints", telemetriaService.getFormattedStats());
        return result;
    }

    @GET
    @Path("/simulacoes")
    public Map<String, Object> getTelemetriaSimulacao() {
        Map<String, Object> result = new HashMap<>();
        result.put("dataReferencia", LocalDate.now().toString());
        result.put("listaEndpoints", telemetriaService.getAggregatedStatsByApi("Simulacoes"));
        return result;
    }

}