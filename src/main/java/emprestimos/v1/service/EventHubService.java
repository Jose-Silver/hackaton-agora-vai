package emprestimos.v1.service;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class EventHubService {
    private static final Logger LOG = LoggerFactory.getLogger(EventHubService.class);

    @ConfigProperty(name = "azure.eventhubs.connection-string")
    String connectionString;

    @ConfigProperty(name = "azure.eventhubs.hub-name")
    String hubName;

    private EventHubProducerClient producerClient;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private volatile boolean initializationFailed = false;

    @PostConstruct
    void init() {
        String maskedConnStr = (connectionString == null) ? "null" : connectionString.replaceAll("(?<=.{10}).", "*");
        LOG.info("[EventHub] Inicializando EventHubService com hubName='{}' e connectionString='{}'", hubName, maskedConnStr);

        if (connectionString == null || connectionString.isBlank()) {
            LOG.warn("[EventHub] A connection string está ausente ou em branco. O EventHub não será inicializado mas a aplicação continuará funcionando.");
            initializationFailed = true;
            return;
        }
        if (hubName == null || hubName.isBlank()) {
            LOG.warn("[EventHub] O nome do hub está ausente ou em branco. O EventHub não será inicializado mas a aplicação continuará funcionando.");
            initializationFailed = true;
            return;
        }

        try {
            producerClient = new EventHubClientBuilder()
                    .connectionString(connectionString, hubName)
                    .buildProducerClient();
            initialized.set(true);
            LOG.info("[EventHub]  Conectado ao Event Hub '{}' com sucesso.", hubName);
        } catch (Exception e) {
            LOG.warn("[EventHub] ⚠ Falha ao inicializar o EventHubProducerClient. A aplicação continuará funcionando normalmente sem o EventHub.", e);
            initializationFailed = true;
            initialized.set(false);
        }
    }

    public void sendMessage(String message) {
        // Se a inicialização falhou, apenas loga e retorna sem afetar a aplicação
        if (initializationFailed) {
            LOG.debug("[EventHub] 🔇 EventHub não inicializado. Mensagem não enviada (aplicação continua funcionando): {}",
                message != null && message.length() > 100 ? message.substring(0, 100) + "..." : message);
            return;
        }

        if (!initialized.get() || producerClient == null) {
            String maskedConnStr = (connectionString == null) ? "null" : connectionString.replaceAll("(?<=.{10}).", "*");
            LOG.debug("[EventHub] 🔇 O cliente produtor não está inicializado. Mensagem não enviada (aplicação continua funcionando). hubName='{}', connectionString='{}'", hubName, maskedConnStr);
            return;
        }

        try {
            EventDataBatch batch = producerClient.createBatch();
            boolean added = batch.tryAdd(new EventData(message));
            if (!added) {
                LOG.debug("[EventHub] ⚠ Mensagem muito grande para o lote. Mensagem não enviada (aplicação continua funcionando).");
                return;
            }
            producerClient.send(batch);
            LOG.debug("[EventHub]  Mensagem enviada com sucesso.");
        } catch (Exception e) {
            LOG.debug("[EventHub] ⚠ Falha ao enviar mensagem (aplicação continua funcionando): {}",
                message != null && message.length() > 100 ? message.substring(0, 100) + "..." : message, e);
        }
    }

    @PreDestroy
    void close() {
        if (producerClient != null) {
            try {
                producerClient.close();
                LOG.info("[EventHub] 🔒 Conexão com o Event Hub fechada.");
            } catch (Exception e) {
                LOG.warn("[EventHub] ⚠ Falha ao fechar o EventHubProducerClient (não afeta o encerramento da aplicação).", e);
            }
        }
    }
}
