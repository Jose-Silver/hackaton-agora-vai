package domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import service.EventHubService;

/**
 * Serviço responsável pelo tratamento centralizado de erros e logging.
 */
@ApplicationScoped
public class ErrorHandlingService {

    private static final Logger LOG = Logger.getLogger(ErrorHandlingService.class);

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
            LOG.infof("Mensagem enviada ao Event Hub com sucesso");
        } catch (Exception exception) {
            LOG.errorf(exception, "Falha ao enviar mensagem ao Event Hub: %s", exception.getMessage());
            // Não propaga a exceção para não afetar a resposta ao cliente
        }
    }

    /**
     * Loga erro de forma padronizada.
     */
    public void logarErro(String operacao, Exception exception) {
        LOG.errorf(exception, "Erro na operação '%s': %s", operacao, exception.getMessage());
    }

    /**
     * Loga informação de forma padronizada.
     */
    public void logarInfo(String mensagem) {
        LOG.infof(mensagem);
    }

    /**
     * Loga aviso de forma padronizada.
     */
    public void logarAviso(String mensagem) {
        LOG.warnf(mensagem);
    }

    /**
     * Loga erro de forma padronizada com contexto.
     */
    public void logarErro(String requestId, String operacao, Exception exception) {
        LOG.errorf(exception, "[requestId=%s] Erro na operação '%s': %s", requestId != null ? requestId : "N/A", operacao, exception.getMessage());
    }

    /**
     * Loga informação de forma padronizada com contexto.
     */
    public void logarInfo(String requestId, String mensagem) {
        LOG.infof("[requestId=%s] %s", requestId != null ? requestId : "N/A", mensagem);
    }

    /**
     * Loga aviso de forma padronizada com contexto.
     */
    public void logarAviso(String requestId, String mensagem) {
        LOG.warnf("[requestId=%s] %s", requestId != null ? requestId : "N/A", mensagem);
    }
}
