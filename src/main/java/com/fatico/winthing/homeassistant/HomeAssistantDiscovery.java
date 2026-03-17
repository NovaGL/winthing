package com.fatico.winthing.homeassistant;

import com.fatico.winthing.Settings;
import com.fatico.winthing.messaging.Message;
import com.fatico.winthing.messaging.MessagePublisher;
import com.fatico.winthing.messaging.QualityOfService;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes Home Assistant MQTT Discovery messages.
 *
 * <p>Enables automatic device and entity discovery in Home Assistant without manual configuration.
 * Discovery messages are published on connection and include all sensors, buttons, and switches.
 *
 * <p>Configuration:
 * <ul>
 *   <li>homeassistant_discovery: Enable/disable discovery (default: true)</li>
 *   <li>homeassistant_prefix: Discovery topic prefix (default: "homeassistant")</li>
 *   <li>device_name: Friendly device name (default: hostname)</li>
 * </ul>
 *
 * @see <a href="https://www.home-assistant.io/integrations/mqtt/#mqtt-discovery">
 *     Home Assistant MQTT Discovery</a>
 * @since 1.6.0
 */
public class HomeAssistantDiscovery {

    private static final String VERSION = "2.0.0";
    private static final Logger logger = LoggerFactory.getLogger(HomeAssistantDiscovery.class);

    private final boolean enabled;
    private final String discoveryPrefix;
    private final String deviceName;
    private final String deviceId;
    private final String topicPrefix;
    private final MessagePublisher publisher;

    @Inject
    public HomeAssistantDiscovery(final Config config, final MessagePublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher);

        this.enabled = !config.hasPath(Settings.HOMEASSISTANT_DISCOVERY)
            || config.getBoolean(Settings.HOMEASSISTANT_DISCOVERY);

        this.discoveryPrefix = config.hasPath(Settings.HOMEASSISTANT_PREFIX)
            ? config.getString(Settings.HOMEASSISTANT_PREFIX) : "homeassistant";

        String topicPrefix = config.getString(Settings.TOPIC_PREFIX);
        if (!topicPrefix.isEmpty() && !topicPrefix.endsWith("/")) {
            topicPrefix += "/";
        }
        this.topicPrefix = topicPrefix;

        String defaultName = getHostname();
        this.deviceName = config.hasPath(Settings.DEVICE_NAME)
            ? config.getString(Settings.DEVICE_NAME) : defaultName;

        this.deviceId = "winthing_"
            + deviceName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "_");
    }

    /**
     * Publishes all discovery messages to Home Assistant.
     * Should be called after MQTT connection is established.
     */
    public void publishDiscovery() {
        if (!enabled) {
            logger.info("Home Assistant discovery disabled");
            return;
        }

        logger.info("Publishing Home Assistant discovery for device: {}", deviceName);

        // Binary sensors
        publishBinarySensor("online", "system/online", "connectivity", "mdi:lan-connect");

        // Sensors
        publishSensor("cpu_usage", "system/monitoring/cpu_usage", null, "%",
            "measurement", "mdi:cpu-64-bit");
        publishSensor("memory_usage", "system/monitoring/memory", "$.usage_percent", "%",
            "measurement", "mdi:memory");
        publishSensor("memory_total", "system/monitoring/memory", "$.total_mb", "MB",
            "data_size", "mdi:memory");
        publishSensor("memory_available", "system/monitoring/memory", "$.available_mb", "MB",
            "data_size", "mdi:memory");
        publishSensor("battery_level", "system/monitoring/battery", "$.level", "%",
            "battery", "mdi:battery");
        publishBinarySensor("battery_charging", "system/monitoring/battery", "$.ac_plugged",
            "plug", "mdi:power-plug");
        publishSensor("now_playing", "system/monitoring/now_playing", null, null,
            null, "mdi:music");

        // Buttons
        publishButton("shutdown", "system/commands/shutdown", "restart", "mdi:power");
        publishButton("reboot", "system/commands/reboot", "restart", "mdi:restart");
        publishButton("suspend", "system/commands/suspend", "restart", "mdi:power-sleep");
        publishButton("hibernate", "system/commands/hibernate", "restart", "mdi:power-sleep");
        publishButton("media_play_pause", "system/commands/media_play_pause",
            null, "mdi:play-pause");
        publishButton("media_next", "system/commands/media_next", null, "mdi:skip-next");
        publishButton("media_prev", "system/commands/media_prev", null, "mdi:skip-previous");
        publishButton("media_stop", "system/commands/media_stop", null, "mdi:stop");

        // Switches
        publishSwitch("display_sleep", "desktop/commands/set_display_sleep", "switch",
            "mdi:monitor-off");

        logger.info("Home Assistant discovery published successfully");
    }

    private void publishSensor(String name, String stateTopic, String valueTemplate,
            String unit, String deviceClass, String icon) {
        final String topic = String.format("%s/sensor/%s/%s/config",
            discoveryPrefix, deviceId, name);
        String friendlyName = formatName(name);

        JsonObject config = new JsonObject();
        config.addProperty("name", friendlyName);
        config.addProperty("unique_id", deviceId + "_" + name);
        config.addProperty("state_topic", topicPrefix + stateTopic);

        if (valueTemplate != null) {
            config.addProperty("value_template", "{{ value_json" + valueTemplate + " }}");
        }
        if (unit != null) {
            config.addProperty("unit_of_measurement", unit);
        }
        if (deviceClass != null) {
            config.addProperty("device_class", deviceClass);
        }
        if (icon != null) {
            config.addProperty("icon", icon);
        }

        config.add("device", getDeviceInfo());

        publish(topic, config);
    }

    private void publishBinarySensor(String name, String stateTopic, String deviceClass,
            String icon) {
        publishBinarySensor(name, stateTopic, null, deviceClass, icon);
    }

    private void publishBinarySensor(String name, String stateTopic, String valueTemplate,
            String deviceClass, String icon) {
        final String topic = String.format("%s/binary_sensor/%s/%s/config",
            discoveryPrefix, deviceId, name);
        String friendlyName = formatName(name);

        JsonObject config = new JsonObject();
        config.addProperty("name", friendlyName);
        config.addProperty("unique_id", deviceId + "_" + name);
        config.addProperty("state_topic", topicPrefix + stateTopic);

        if (valueTemplate != null) {
            config.addProperty("value_template", "{{ value_json" + valueTemplate + " }}");
        }
        if (deviceClass != null) {
            config.addProperty("device_class", deviceClass);
        }
        if (icon != null) {
            config.addProperty("icon", icon);
        }

        config.add("device", getDeviceInfo());

        publish(topic, config);
    }

    private void publishButton(String name, String commandTopic, String deviceClass,
            String icon) {
        final String topic = String.format("%s/button/%s/%s/config",
            discoveryPrefix, deviceId, name);
        String friendlyName = formatName(name);

        JsonObject config = new JsonObject();
        config.addProperty("name", friendlyName);
        config.addProperty("unique_id", deviceId + "_" + name);
        config.addProperty("command_topic", topicPrefix + commandTopic);
        config.addProperty("payload_press", "");

        if (deviceClass != null) {
            config.addProperty("device_class", deviceClass);
        }
        if (icon != null) {
            config.addProperty("icon", icon);
        }

        config.add("device", getDeviceInfo());

        publish(topic, config);
    }

    private void publishSwitch(String name, String commandTopic, String deviceClass,
            String icon) {
        final String topic = String.format("%s/switch/%s/%s/config",
            discoveryPrefix, deviceId, name);
        String friendlyName = formatName(name);

        JsonObject config = new JsonObject();
        config.addProperty("name", friendlyName);
        config.addProperty("unique_id", deviceId + "_" + name);
        config.addProperty("command_topic", topicPrefix + commandTopic);
        config.addProperty("payload_on", "true");
        config.addProperty("payload_off", "false");

        if (deviceClass != null) {
            config.addProperty("device_class", deviceClass);
        }
        if (icon != null) {
            config.addProperty("icon", icon);
        }

        config.add("device", getDeviceInfo());

        publish(topic, config);
    }

    private JsonObject getDeviceInfo() {
        JsonObject device = new JsonObject();
        device.addProperty("identifiers", deviceId);
        device.addProperty("name", deviceName);
        device.addProperty("manufacturer", "WinThing");
        device.addProperty("model", "MQTT Control");
        device.addProperty("sw_version", VERSION);

        return device;
    }

    private void publish(String topic, JsonObject payload) {
        Message message = new Message(
            topic,
            payload,
            QualityOfService.AT_LEAST_ONCE,
            true  // Retained for discovery
        );
        publisher.publish(message);
    }

    private String formatName(String name) {
        return deviceName + " " + name.replace("_", " ")
            .substring(0, 1).toUpperCase(Locale.ROOT) + name.replace("_", " ").substring(1);
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            logger.warn("Could not get hostname, using default", e);
            return "WinThing";
        }
    }
}
