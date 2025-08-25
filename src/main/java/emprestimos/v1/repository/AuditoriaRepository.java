package emprestimos.v1.repository;

import emprestimos.v1.domain.entity.local.RegistroAuditoria;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório para operações de auditoria
 */
@ApplicationScoped
public class AuditoriaRepository implements PanacheRepository<RegistroAuditoria> {

    /**
     * Busca registros de auditoria por usuário
     */
    public List<RegistroAuditoria> findByUsuario(String usuario) {
        return list("usuario", usuario);
    }

    /**
     * Busca registros de auditoria por ação
     */
    public List<RegistroAuditoria> findByAcao(String acao) {
        return list("acao", acao);
    }

    /**
     * Busca registros de auditoria por recurso
     */
    public List<RegistroAuditoria> findByRecurso(String recurso) {
        return list("recurso", recurso);
    }

    /**
     * Busca registros de auditoria por período
     */
    public List<RegistroAuditoria> findByPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        return list("dataHora >= ?1 and dataHora <= ?2", dataInicio, dataFim);
    }

    /**
     * Busca registros de auditoria por usuário e período
     */
    public List<RegistroAuditoria> findByUsuarioAndPeriodo(String usuario, LocalDateTime dataInicio, LocalDateTime dataFim) {
        return list("usuario = ?1 and dataHora >= ?2 and dataHora <= ?3", usuario, dataInicio, dataFim);
    }

    /**
     * Busca registros de auditoria com erro
     */
    public List<RegistroAuditoria> findComErros() {
        return list("status = 'ERRO'");
    }

    /**
     * Conta registros por usuário
     */
    public long countByUsuario(String usuario) {
        return count("usuario", usuario);
    }

    /**
     * Remove registros antigos (para limpeza automática)
     */
    public long deleteRegistrosAntigos(LocalDateTime dataLimite) {
        return delete("dataHora < ?1", dataLimite);
    }
}
