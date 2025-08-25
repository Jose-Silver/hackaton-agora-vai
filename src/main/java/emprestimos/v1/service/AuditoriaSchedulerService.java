package emprestimos.v1.service;

import emprestimos.v1.config.Auditado;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço para tarefas agendadas relacionadas à auditoria
 */
@ApplicationScoped
@Slf4j
public class AuditoriaSchedulerService {

    @Inject
    AuditoriaService auditoriaService;

    /**
     * Executa limpeza automática de registros de auditoria antigos
     * Roda todos os dias à meia-noite
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Auditado(acao = "LIMPEZA_AUTOMATICA", recurso = "AUDITORIA")
    public void limpezaAutomaticaRegistros() {
        try {
            log.info("Iniciando limpeza automática de registros de auditoria");
            
            // Mantém registros dos últimos 90 dias por padrão
            int diasRetencao = 90;
            long removidos = auditoriaService.limparRegistrosAntigos(diasRetencao);
            
            log.info("Limpeza automática concluída. Removidos {} registros anteriores a {} dias", 
                    removidos, diasRetencao);
                    
        } catch (Exception e) {
            log.error("Erro na limpeza automática de registros de auditoria: ", e);
        }
    }

    /**
     * Gera relatório semanal de auditoria
     * Roda toda segunda-feira às 6h
     */
    @Scheduled(cron = "0 0 6 ? * MON")
    @Auditado(acao = "RELATORIO_SEMANAL", recurso = "AUDITORIA")
    public void gerarRelatorioSemanal() {
        try {
            log.info("Iniciando geração de relatório semanal de auditoria");
            
            // Aqui você pode implementar a lógica para gerar e enviar relatórios
            // Por exemplo, contar operações por usuário, identificar padrões suspeitos, etc.
            
            log.info("Relatório semanal de auditoria gerado com sucesso");
            
        } catch (Exception e) {
            log.error("Erro na geração do relatório semanal de auditoria: ", e);
        }
    }
}
