package emprestimos.v1.resource;

import emprestimos.v1.util.FieldFilterUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import emprestimos.v1.service.TelemetriaService;

@Path("/telemetria")
@Produces(MediaType.APPLICATION_JSON)
public class TelemetriaResource {
    @Inject
    TelemetriaService telemetriaService;

    @Inject
    FieldFilterUtil fieldFilterUtil;

    @GET
    @Operation(
        summary = "Obter telemetria do sistema",
        description = "Retorna informações de telemetria incluindo estatísticas dos endpoints"
    )
    public Object getTelemetria(@QueryParam("campos") @Schema(description = "Lista de campos separados por vírgula para filtrar a resposta", examples = {"dataReferencia,listaEndpoints"}) String campos) {
        Map<String, Object> result = new HashMap<>();
        result.put("dataReferencia", LocalDate.now().toString());
        result.put("listaEndpoints", telemetriaService.getFormattedStats());

        return fieldFilterUtil.filterFields(result, campos);
    }
}
