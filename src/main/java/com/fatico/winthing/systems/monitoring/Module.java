package com.fatico.winthing.systems.monitoring;

import com.google.inject.PrivateModule;
import com.google.inject.Singleton;

public class Module extends PrivateModule {

    @Override
    protected void configure() {
        bind(MonitoringService.class).in(Singleton.class);
        bind(MonitoringController.class).asEagerSingleton();
        expose(MonitoringService.class);
        expose(MonitoringController.class);
    }

}
