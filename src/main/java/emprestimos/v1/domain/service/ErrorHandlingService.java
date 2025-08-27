package emprestimos.v1.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import emprestimos.v1.service.EventHubService;

/**
 * Serviço responsável pelo tratamento centralizado de erros e logging.
 */
@ApplicationScoped
public class ErrorHandlingService {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandlingService.class);

    @Inject
    EventHubService eventHubService;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Envia mensagem ao Event Hub com tratamento de erro adequado.
     */
    public void enviarMensagemEventHub(Object mensagem) {
        try {
            String json = objectMapper.writeValueAsString(mensagem);
            eventHubService.sendMessage(json);
            LOG.info("Mensagem enviada ao Event Hub com sucesso");
        } catch (Exception exception) {
            LOG.error("Falha ao enviar mensagem ao Event Hub: {}", exception.getMessage(), exception);
            // Não propaga a exceção para não afetar a resposta ao cliente
        }
    }

    /**
     * Loga erro de forma padronizada.
     */
    public void logarErro(String operacao, Exception exception) {
        LOG.error("Erro na operação '{}': {}", operacao, exception.getMessage(), exception);
    }

    /**
     * Loga informação de forma padronizada.
     */
    public void logarInfo(String mensagem) {
        LOG.info(mensagem);
    }

    /**
     * Loga aviso de forma padronizada.
     */
    public void logarAviso(String mensagem) {
        LOG.warn(mensagem);
    }

    /**
     * Loga erro de forma padronizada com contexto.
     */
    public void logarErro(String requestId, String operacao, Exception exception) {
        LOG.error("[requestId={}] Erro na operação '{}': {}", requestId != null ? requestId : "N/A", operacao, exception.getMessage(), exception);
    }

    /**
     * Loga informação de forma padronizada com contexto.
     */
    public void logarInfo(String requestId, String mensagem) {
        LOG.info("[requestId={}] {}", requestId != null ? requestId : "N/A", mensagem);
    }

    /**
     * Loga aviso de forma padronizada com contexto.
     */
    public void logarAviso(String requestId, String mensagem) {
        LOG.warn("[requestId={}] {}", requestId != null ? requestId : "N/A", mensagem);
    }
}
