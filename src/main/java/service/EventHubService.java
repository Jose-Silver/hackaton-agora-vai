package service;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class EventHubService {
    private static final Logger LOG = Logger.getLogger(EventHubService.class);

    @ConfigProperty(name = "azure.eventhubs.connection-string")
    String connectionString;

    @ConfigProperty(name = "azure.eventhubs.hub-name")
    String hubName;

    private EventHubProducerClient producerClient;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @PostConstruct
    void init() {
        String maskedConnStr = (connectionString == null) ? "null" : connectionString.replaceAll("(?<=.{10}).", "*");
        LOG.infof("[EventHub] Inicializando EventHubService com hubName='%s' e connectionString='%s'", hubName, maskedConnStr);

        if (connectionString == null || connectionString.isBlank()) {
            LOG.error("[EventHub] A connection string está ausente ou em branco. O EventHub não será inicializado.");
            return;
        }
        if (hubName == null || hubName.isBlank()) {
            LOG.error("[EventHub] O nome do hub está ausente ou em branco. O EventHub não será inicializado.");
            return;
        }

        try {
            producerClient = new EventHubClientBuilder()
                    .connectionString(connectionString, hubName)
                    .buildProducerClient();
            initialized.set(true);
            LOG.infof("[EventHub] Conectado ao Event Hub '%s' com sucesso.", hubName);
        } catch (Exception e) {
            LOG.error("[EventHub] Falha ao inicializar o EventHubProducerClient.", e);
        }
    }

    public void sendMessage(String message) {
        if (!initialized.get() || producerClient == null) {
            String maskedConnStr = (connectionString == null) ? "null" : connectionString.replaceAll("(?<=.{10}).", "*");
            LOG.errorf("[EventHub] O cliente produtor não está inicializado. Mensagem não enviada. hubName='%s', connectionString='%s'", hubName, maskedConnStr);
            return;
        }

        try {
            EventDataBatch batch = producerClient.createBatch();
            boolean added = batch.tryAdd(new EventData(message));
            if (!added) {
                LOG.error("[EventHub] Mensagem muito grande para o lote. Mensagem não enviada.");
                return;
            }
            producerClient.send(batch);
            LOG.info("[EventHub] Mensagem enviada com sucesso.");
        } catch (Exception e) {
            LOG.errorf(e, "[EventHub] Falha ao enviar mensagem: %s", message);
        }
    }

    @PreDestroy
    void close() {
        if (producerClient != null) {
            try {
                producerClient.close();
                LOG.info("[EventHub] 🔒 Conexão com o Event Hub fechada.");
            } catch (Exception e) {
                LOG.error("[EventHub] Falha ao fechar o EventHubProducerClient.", e);
            }
        }
    }
}
