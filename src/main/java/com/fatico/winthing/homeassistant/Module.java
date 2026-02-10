package com.fatico.winthing.homeassistant;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Guice module for Home Assistant MQTT Discovery integration.
 *
 * @since 1.6.0
 */
public class Module extends AbstractModule {

    @Override
    protected void configure() {
        bind(HomeAssistantDiscovery.class).in(Singleton.class);
        bind(HomeAssistantController.class).asEagerSingleton();
    }
}
