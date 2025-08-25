package emprestimos.v1.config;

import jakarta.enterprise.context.RequestScoped;
import lombok.Data;

/**
 * Contexto para armazenar informações de auditoria durante a requisição
 */
@RequestScoped
@Data
public class AuditoriaContext {
    
    private String ipOrigem;
    private String userAgent;
    private String usuario;
    private String sessaoId;
    private Long inicioRequisicao;
    
    public void iniciarTempo() {
        this.inicioRequisicao = System.currentTimeMillis();
    }
    
    public Long calcularTempoExecucao() {
        if (inicioRequisicao != null) {
            return System.currentTimeMillis() - inicioRequisicao;
        }
        return null;
    }
}
