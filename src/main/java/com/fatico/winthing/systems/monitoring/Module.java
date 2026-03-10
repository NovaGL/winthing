package com.fatico.winthing.systems.monitoring;

import com.google.inject.AbstractModule;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        bind(MonitoringController.class).asEagerSingleton();
    }

}
