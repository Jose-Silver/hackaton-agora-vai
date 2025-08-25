package emprestimos.v1.domain.dto.auditoria.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para resposta de consultas de auditoria
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaResponseDTO {
    
    private Long id;
    private String usuario;
    private String acao;
    private String recurso;
    private String ipOrigem;
    private String userAgent;
    private String detalhes;
    private String dadosAnteriores;
    private String dadosNovos;
    private String status;
    private Long tempoExecucao;
    private String mensagemErro;
    private LocalDateTime dataHora;
    private String sessaoId;
}
