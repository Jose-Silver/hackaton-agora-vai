package emprestimos.v1.util;

import emprestimos.v1.service.TelemetriaService;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Provider
public class FiltroTelemetria implements ContainerResponseFilter, ContainerRequestFilter {
    @Inject
    TelemetriaService telemetriaService;

    private static final Logger LOG = LoggerFactory.getLogger(FiltroTelemetria.class);

    private static final String START_TIME = "telemetryStartTime";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object startObj = requestContext.getProperty(START_TIME);
        if (startObj instanceof Long startTime) {
            long duration = System.currentTimeMillis() - startTime;
            String method = requestContext.getMethod();
            String path = requestContext.getUriInfo().getPath();
            String key = method + " " + path;
            boolean sucesso = responseContext.getStatus() == 200;
            LOG.info("Registrando telemetria: {} - Sucesso: {}", key, sucesso);
            telemetriaService.record(key, duration, sucesso);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        setStartTime(requestContext);
    }

    public static void setStartTime(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_TIME, System.currentTimeMillis());
    }
}