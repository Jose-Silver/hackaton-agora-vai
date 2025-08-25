package emprestimos.v1.resource;

import emprestimos.v1.domain.entity.local.RegistroAuditoria;
import emprestimos.v1.service.AuditoriaService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Recurso REST para consultar logs de auditoria
 */
@Path("/auditoria")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class AuditoriaResource {

    @Inject
    AuditoriaService auditoriaService;

    /**
     * Busca registros de auditoria por período
     * Formato de data: yyyy-MM-dd
     */
    @GET
    @Path("/periodo")
    public Response buscarPorPeriodo(
            @QueryParam("dataInicio") String dataInicioStr,
            @QueryParam("dataFim") String dataFimStr) {

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDateTime dataInicio = dataInicioStr != null ?
                LocalDate.parse(dataInicioStr, formatter).atStartOfDay() :
                LocalDateTime.now().minusDays(30); // Padrão: últimos 30 dias

            LocalDateTime dataFim = dataFimStr != null ?
                LocalDate.parse(dataFimStr, formatter).atTime(23, 59, 59) :
                LocalDateTime.now();

            List<RegistroAuditoria> registros = auditoriaService.buscarPorPeriodo(dataInicio, dataFim);
            return Response.ok(registros).build();

        } catch (Exception e) {
            log.error("Erro ao buscar registros por período: ", e);
            return Response.serverError().entity("Erro interno do servidor").build();
        }
    }

    /**
     * Busca registros de auditoria com erro
     */
    @GET
    @Path("/erros")
    public Response buscarRegistrosComErros() {
        try {
            List<RegistroAuditoria> registros = auditoriaService.buscarRegistrosComErros();
            return Response.ok(registros).build();
        } catch (Exception e) {
            log.error("Erro ao buscar registros com erro: ", e);
            return Response.serverError().entity("Erro interno do servidor").build();
        }
    }

    /**
     * Remove registros antigos (operação administrativa)
     */
    @DELETE
    @Path("/limpeza/{diasRetencao}")
    public Response limparRegistrosAntigos(@PathParam("diasRetencao") int diasRetencao) {
        try {
            if (diasRetencao < 1) {
                return Response.status(400).entity("Dias de retenção deve ser maior que zero").build();
            }

            long removidos = auditoriaService.limparRegistrosAntigos(diasRetencao);
            return Response.ok()
                    .entity("Removidos " + removidos + " registros anteriores a " + diasRetencao + " dias")
                    .build();

        } catch (Exception e) {
            log.error("Erro ao limpar registros antigos: ", e);
            return Response.serverError().entity("Erro interno do servidor").build();
        }
    }
}
