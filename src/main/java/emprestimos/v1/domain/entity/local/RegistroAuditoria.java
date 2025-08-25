package emprestimos.v1.domain.entity.local;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Entidade que representa um registro de auditoria no sistema
 */
@Data
@Entity
@Table(name = "registro_auditoria")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAuditoria {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String usuario;
    
    @Column(nullable = false, length = 50)
    private String acao;
    
    @Column(nullable = false, length = 200)
    private String recurso;
    
    @Column(name = "ip_origem", length = 45)
    private String ipOrigem;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(columnDefinition = "TEXT")
    private String detalhes;
    
    @Column(name = "dados_anteriores", columnDefinition = "TEXT")
    private String dadosAnteriores;
    
    @Column(name = "dados_novos", columnDefinition = "TEXT")
    private String dadosNovos;
    
    @Column(nullable = false)
    private String status;
    
    @Column(name = "tempo_execucao")
    private Long tempoExecucao; // em millisegundos
    
    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;
    
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
    
    @Column(name = "sessao_id", length = 100)
    private String sessaoId;
    
    @PrePersist
    public void prePersist() {
        if (dataHora == null) {
            dataHora = LocalDateTime.now();
        }
    }
}
