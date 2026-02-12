package com.fatico.winthing.platform.linux;

import com.fatico.winthing.systems.desktop.DesktopService;
import com.fatico.winthing.systems.keyboard.KeyboardService;
import com.fatico.winthing.systems.monitoring.MonitoringService;
import com.fatico.winthing.systems.system.SystemService;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Guice module that binds all Linux-specific service implementations.
 *
 * @since 2.0.0
 */
public class LinuxPlatformModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SystemService.class).to(LinuxSystemService.class).in(Singleton.class);
        bind(DesktopService.class).to(LinuxDesktopService.class).in(Singleton.class);
        bind(KeyboardService.class).to(LinuxKeyboardService.class).in(Singleton.class);
        bind(MonitoringService.class).to(LinuxMonitoringService.class).in(Singleton.class);
    }
}
