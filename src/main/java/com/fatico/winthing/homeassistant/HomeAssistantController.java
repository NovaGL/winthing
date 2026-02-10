package com.fatico.winthing.homeassistant;

import com.fatico.winthing.messaging.Registry;
import com.google.inject.Inject;
import java.util.Objects;

/**
 * Controller for Home Assistant MQTT Discovery integration.
 *
 * <p>Publishes discovery messages on MQTT connection to enable automatic
 * entity discovery in Home Assistant.
 *
 * @since 1.6.0
 */
public class HomeAssistantController {

    private final HomeAssistantDiscovery discovery;

    @Inject
    @SuppressWarnings("this-escape")
    public HomeAssistantController(final Registry registry,
            final HomeAssistantDiscovery discovery) {
        this.discovery = Objects.requireNonNull(discovery);
        registry.addConnectionListener(this::publishDiscovery);
    }

    private void publishDiscovery() {
        discovery.publishDiscovery();
    }
}
