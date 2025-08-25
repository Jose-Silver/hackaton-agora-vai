package emprestimos.v1.service;

import emprestimos.v1.domain.entity.local.RegistroAuditoria;
import emprestimos.v1.repository.AuditoriaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço responsável por gerenciar a auditoria do sistema
 */
@ApplicationScoped
@Slf4j
public class AuditoriaService {

    @Inject
    AuditoriaRepository auditoriaRepository;

    /**
     * Registra uma operação de auditoria
     */
    @Transactional
    public void registrarAuditoria(RegistroAuditoria registro) {
        try {
            auditoriaRepository.persist(registro);
            log.debug("Registro de auditoria salvo: {}", registro.getAcao());
        } catch (Exception e) {
            log.error("Erro ao salvar registro de auditoria: ", e);
        }
    }

    /**
     * Cria e registra um log de auditoria de sucesso
     */
    @Transactional
    public void registrarSucesso(String usuario, String acao, String recurso,
                                String ipOrigem, String userAgent, String detalhes,
                                String dadosAnteriores, String dadosNovos,
                                long tempoExecucao, String sessaoId) {

        RegistroAuditoria registro = RegistroAuditoria.builder()
                .usuario(usuario)
                .acao(acao)
                .recurso(recurso)
                .ipOrigem(ipOrigem)
                .userAgent(userAgent)
                .detalhes(detalhes)
                .dadosAnteriores(dadosAnteriores)
                .dadosNovos(dadosNovos)
                .status("SUCESSO")
                .tempoExecucao(tempoExecucao)
                .dataHora(LocalDateTime.now())
                .sessaoId(sessaoId)
                .build();

        registrarAuditoria(registro);
    }

    /**
     * Cria e registra um log de auditoria de erro
     */
    @Transactional
    public void registrarErro(String usuario, String acao, String recurso,
                             String ipOrigem, String userAgent, String detalhes,
                             String mensagemErro, long tempoExecucao, String sessaoId) {

        RegistroAuditoria registro = RegistroAuditoria.builder()
                .usuario(usuario)
                .acao(acao)
                .recurso(recurso)
                .ipOrigem(ipOrigem)
                .userAgent(userAgent)
                .detalhes(detalhes)
                .status("ERRO")
                .mensagemErro(mensagemErro)
                .tempoExecucao(tempoExecucao)
                .dataHora(LocalDateTime.now())
                .sessaoId(sessaoId)
                .build();

        registrarAuditoria(registro);
    }

    /**
     * Busca registros de auditoria por usuário
     */
    public List<RegistroAuditoria> buscarPorUsuario(String usuario) {
        return auditoriaRepository.findByUsuario(usuario);
    }

    /**
     * Busca registros de auditoria por período
     */
    public List<RegistroAuditoria> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        return auditoriaRepository.findByPeriodo(dataInicio, dataFim);
    }

    /**
     * Busca registros de auditoria com erro
     */
    public List<RegistroAuditoria> buscarRegistrosComErros() {
        return auditoriaRepository.findComErros();
    }

    /**
     * Remove registros antigos (para limpeza automática)
     * Remove registros mais antigos que o número de dias especificado
     */
    @Transactional
    public long limparRegistrosAntigos(int diasRetencao) {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(diasRetencao);
        long removidos = auditoriaRepository.deleteRegistrosAntigos(dataLimite);
        log.info("Removidos {} registros de auditoria anteriores a {}", removidos, dataLimite);
        return removidos;
    }

    /**
     * Converte objeto para string JSON simplificada para auditoria
     */
    public String objetoParaString(Object objeto) {
        if (objeto == null) {
            return null;
        }

        try {
            // Implementação simples - em produção, usar Jackson ou Gson
            return objeto.toString();
        } catch (Exception e) {
            log.warn("Erro ao converter objeto para string: ", e);
            return "Erro na conversão: " + e.getMessage();
        }
    }
}
