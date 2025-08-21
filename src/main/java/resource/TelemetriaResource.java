package resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import service.TelemetriaService;

@Path("/telemetria")
@Produces(MediaType.APPLICATION_JSON)
public class TelemetriaResource {
    @Inject
    TelemetriaService telemetriaService;

    @GET
    public Map<String, Object> getTelemetria() {
        Map<String, Object> result = new HashMap<>();
        result.put("dataReferencia", LocalDate.now().toString());
        result.put("listaEndpoints", telemetriaService.getFormattedStats());
        return result;
    }
}
