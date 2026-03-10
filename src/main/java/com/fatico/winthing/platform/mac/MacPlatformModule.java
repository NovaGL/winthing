package com.fatico.winthing.platform.mac;

import com.fatico.winthing.systems.desktop.DesktopService;
import com.fatico.winthing.systems.keyboard.KeyboardService;
import com.fatico.winthing.systems.monitoring.MonitoringService;
import com.fatico.winthing.systems.system.SystemService;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Guice module that binds all macOS-specific service implementations.
 *
 * @since 2.0.0
 */
public class MacPlatformModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SystemService.class).to(MacSystemService.class).in(Singleton.class);
        bind(DesktopService.class).to(MacDesktopService.class).in(Singleton.class);
        bind(KeyboardService.class).to(MacKeyboardService.class).in(Singleton.class);
        bind(MonitoringService.class).to(MacMonitoringService.class).in(Singleton.class);
    }
}
