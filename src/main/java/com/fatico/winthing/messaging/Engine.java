package com.fatico.winthing.messaging;

import com.fatico.winthing.Application;
import com.fatico.winthing.Settings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core MQTT messaging engine for WinThing.
 *
 * <p>Manages MQTT client connection lifecycle, handles incoming messages,
 * and routes them to appropriate handlers.
 *
 * <p>Thread Safety:
 * <ul>
 *   <li>Client operations are protected by runningLock (ReentrantLock)</li>
 *   <li>Message callbacks are serialized through message handlers</li>
 *   <li>Registry access is thread-safe via ConcurrentHashMap</li>
 * </ul>
 *
 * <p>Reconnection:
 * <ul>
 *   <li>Automatic reconnection with configurable interval</li>
 *   <li>Connection attempts logged for debugging</li>
 *   <li>Graceful handling of connection loss</li>
 * </ul>
 *
 * <p>Security:
 * <ul>
 *   <li>Supports TLS/SSL encryption (recommended for production)</li>
 *   <li>Username/password authentication</li>
 *   <li>Payload logging disabled by default to prevent sensitive data exposure</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class Engine implements MqttCallback, MessagePublisher {

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final boolean LOG_PAYLOADS = false;  // Security default: off

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Gson gson;
    private final Registry registry;
    private final String topicPrefix;
    private final IMqttAsyncClient client;
    private final MqttConnectOptions options = new MqttConnectOptions();
    private final Duration reconnectInterval;

    private final Lock runningLock = new ReentrantLock();
    private final Condition runningCondition = runningLock.newCondition();

    @Inject
    @SuppressWarnings("this-escape")
    public Engine(final Gson gson, final Registry registry, final Config config,
            final MqttClientPersistence persistence) throws MqttException {
        String topicPrefix = config.getString(Settings.TOPIC_PREFIX);
        if (!topicPrefix.isEmpty() && !topicPrefix.endsWith("/")) {
            topicPrefix += "/";
        }
        this.topicPrefix = topicPrefix;

        this.reconnectInterval = Duration.ofSeconds(config.getLong(Settings.RECONNECT_INTERVAL));

        this.gson = Objects.requireNonNull(gson);
        this.registry = Objects.requireNonNull(registry);

        String protocol = config.hasPath(Settings.MQTT_PROTOCOL)
            ? config.getString(Settings.MQTT_PROTOCOL) : "tcp";
        String brokerUrl = config.getString(Settings.BROKER_URL);
        String connectionUrl = protocol + "://" + brokerUrl;

        this.client = new MqttAsyncClient(
            connectionUrl,
            config.getString(Settings.CLIENT_ID),
            persistence
        );
        this.client.setCallback(this);

        {
            final String username = config.getString(Settings.BROKER_USERNAME);
            if (username != null && !username.isEmpty()) {
                this.options.setUserName(username);
            }
        }
        {
            final String password = config.getString(Settings.BROKER_PASSWORD);
            if (password != null && !password.isEmpty()) {
                this.options.setPassword(password.toCharArray());
            }
        }

        // Configure SSL if using ssl or tls protocol
        if (protocol.equalsIgnoreCase("ssl") || protocol.equalsIgnoreCase("tls")) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, new SecureRandom());
                this.options.setSocketFactory(sslContext.getSocketFactory());
            } catch (final Exception exception) {
                throw new MqttException(exception);
            }
        }

        this.options.setCleanSession(true);
    }

    public void run() {
        runningLock.lock();
        try {
            long backoffSeconds = reconnectInterval.getSeconds();
            final long maxBackoffSeconds = 60;
            while (true) {
                boolean connected = false;
                try {
                    connect();
                    connected = true;
                    backoffSeconds = reconnectInterval.getSeconds(); // reset on success
                } catch (final MqttException exception) {
                    logger.error("Could not connect: {}", exception.getMessage());
                }
                if (connected) {
                    try {
                        runningCondition.await();
                    } catch (final InterruptedException exception) {
                        try {
                            disconnect();
                        } catch (final MqttException disconnectException) {
                            logger.error("Could not disconnect.", disconnectException);
                        }
                        return;
                    }
                    backoffSeconds = reconnectInterval.getSeconds(); // reset after clean disconnect
                }
                logger.info("Trying to reconnect in {} seconds...", backoffSeconds);
                try {
                    Thread.sleep(backoffSeconds * 1000L);
                } catch (final InterruptedException exception) {
                    return;
                }
                backoffSeconds = Math.min(backoffSeconds * 2, maxBackoffSeconds);
            }
        } finally {
            runningLock.unlock();
        }
    }

    private void connect() throws MqttException {
        if (registry.getWill().isPresent()) {
            final Message will = registry.getWill().get();
            final MqttMessage mqttMessage = serialize(will);
            this.options.setWill(
                topicPrefix + will.getTopic(),
                mqttMessage.getPayload(),
                mqttMessage.getQos(),
                mqttMessage.isRetained()
            );
        }

        logger.info("Connecting to {} as {}...", client.getServerURI(), client.getClientId());
        client.connect(options).waitForCompletion();
        logger.info("Connected.");

        logger.info("Subscribing to topics...");
        for (final Map.Entry<String, QualityOfService> entry
                : registry.getSubscriptions().entrySet()) {
            logger.info("  - {}", topicPrefix + entry.getKey());
            client.subscribe(topicPrefix + entry.getKey(), entry.getValue().ordinal());
        }
        logger.info("Subscribed.");

        logger.info("Sending initial messages...");
        registry.getInitialMessages().stream().forEach(this::publish);

        logger.info("Engine started.");

        Application.getApp().setIcon(true);

        for (final Runnable listener : registry.getConnectionListeners()) {
            try {
                listener.run();
            } catch (final RuntimeException exception) {
                logger.error("Connection listener error: {}", exception.getMessage(), exception);
            }
        }
    }

    private void disconnect() throws MqttException {
        for (final Runnable listener : registry.getDisconnectionListeners()) {
            try {
                listener.run();
            } catch (final RuntimeException exception) {
                logger.error("Disconnection listener error: {}", exception.getMessage(), exception);
            }
        }

        Application.getApp().setIcon(false);

        client.disconnect();
    }

    @Override
    public void publish(final Message message) {
        final MqttMessage mqttMessage = serialize(message);
        try {
            client.publish(
                topicPrefix + message.getTopic(),
                mqttMessage
            ).waitForCompletion();
        } catch (final MqttException exception) {
            logger.error("Error while publishing message.", exception);
        }
    }

    @Override
    public void connectionLost(final Throwable throwable) {
        Application.getApp().setIcon(false);

        logger.error("Connection lost.");
        runningLock.lock();
        try {
            runningCondition.signal();
        } finally {
            runningLock.unlock();
        }
    }

    @Override
    public void messageArrived(final String topic, final MqttMessage mqttMessage) throws Exception {
        try {
            handleMessage(topic, mqttMessage);
        } catch (final Throwable throwable) {
            // Avoid logging payloads for security
            logger.error(
                    "Error while handling message on topic {}: {}",
                    topic,
                    throwable.getMessage(),
                    throwable
            );
        }
    }

    @Override
    public void deliveryComplete(final IMqttDeliveryToken token) {
        // Do nothing.
    }

    private void handleMessage(String topic, final MqttMessage mqttMessage) throws Exception {
        if (!topic.startsWith(topicPrefix)) {
            return;
        }
        topic = topic.substring(topicPrefix.length());

        final Collection<Consumer<Message>> consumers = registry.getConsumers(topic);
        if (consumers.isEmpty()) {
            return;
        }

        final byte[] payloadBytes = mqttMessage.getPayload();
        final JsonElement payload;
        if (payloadBytes.length == 0) {
            payload = null;
        } else {
            try {
                payload = gson.fromJson(new String(payloadBytes, CHARSET), JsonElement.class);
            } catch (final JsonSyntaxException exception) {
                logger.error("Invalid JSON received for: {}", topic);
                return;
            }
        }

        final Message message = new Message(
                topic,
                payload,
                QualityOfService.values()[mqttMessage.getQos()],
                mqttMessage.isRetained()
        );

        if (LOG_PAYLOADS) {
            logger.debug(
                    "Received: {}({})",
                    message.getTopic(),
                    message.getPayload().isPresent() ? message.getPayload().get().toString() : ""
            );
        } else {
            logger.debug("Received message on topic: {}", message.getTopic());
        }

        for (final Consumer<Message> consumer : consumers) {
            try {
                consumer.accept(message);
            } catch (final RuntimeException exception) {
                logger.error(
                        "Error while processing {}({}): {}",
                        message.getTopic(),
                        message.getPayload().isPresent()
                                ? message.getPayload().get().toString() : "",
                        exception.getMessage(),
                        exception
                );
            }
        }
    }

    private MqttMessage serialize(final Message message) {
        final byte[] payload;
        if (message.getPayload().isPresent()) {
            payload = gson.toJson(message.getPayload().get()).getBytes(CHARSET);
        } else {
            payload = new byte[0];
        }
        final MqttMessage mqttMessage = new MqttMessage(payload);
        mqttMessage.setQos(message.getQualityOfService().ordinal());
        mqttMessage.setRetained(message.isRetained());
        return mqttMessage;
    }

}
