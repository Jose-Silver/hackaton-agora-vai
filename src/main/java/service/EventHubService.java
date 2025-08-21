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
        LOG.infof("[EventHub] Initializing EventHubService with hubName='%s' and connectionString='%s'", hubName, maskedConnStr);

        if (connectionString == null || connectionString.isBlank()) {
            LOG.error("[EventHub] Connection string is missing or blank. EventHub will not be initialized.");
            return;
        }
        if (hubName == null || hubName.isBlank()) {
            LOG.error("[EventHub] Hub name is missing or blank. EventHub will not be initialized.");
            return;
        }

        try {
            producerClient = new EventHubClientBuilder()
                    .connectionString(connectionString, hubName)
                    .buildProducerClient();
            initialized.set(true);
            LOG.infof("[EventHub] ✅ Connected to Event Hub '%s' successfully.", hubName);
        } catch (Exception e) {
            LOG.error("[EventHub] ❌ Failed to initialize EventHubProducerClient.", e);
        }
    }

    public void sendMessage(String message) {
        if (!initialized.get() || producerClient == null) {
            String maskedConnStr = (connectionString == null) ? "null" : connectionString.replaceAll("(?<=.{10}).", "*");
            LOG.errorf("[EventHub] Producer client is not initialized. Message not sent. hubName='%s', connectionString='%s'", hubName, maskedConnStr);
            return;
        }

        try {
            EventDataBatch batch = producerClient.createBatch();
            boolean added = batch.tryAdd(new EventData(message));
            if (!added) {
                LOG.error("[EventHub] Message too large for batch. Message not sent.");
                return;
            }
            producerClient.send(batch);
            LOG.info("[EventHub] 📤 Message sent successfully.");
        } catch (Exception e) {
            LOG.errorf(e, "[EventHub] ❌ Failed to send message: %s", message);
        }
    }

    @PreDestroy
    void close() {
        if (producerClient != null) {
            try {
                producerClient.close();
                LOG.info("[EventHub] 🔒 Connection to Event Hub closed.");
            } catch (Exception e) {
                LOG.error("[EventHub] ❌ Failed to close EventHubProducerClient.", e);
            }
        }
    }
}
