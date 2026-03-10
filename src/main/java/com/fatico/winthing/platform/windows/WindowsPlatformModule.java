package com.fatico.winthing.platform.windows;

import com.fatico.winthing.systems.desktop.DesktopService;
import com.fatico.winthing.systems.keyboard.KeyboardService;
import com.fatico.winthing.systems.monitoring.MonitoringService;
import com.fatico.winthing.systems.system.SystemService;
import com.fatico.winthing.windows.WindowsModule;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Guice module that binds all Windows-specific service implementations.
 *
 * @since 2.0.0
 */
public class WindowsPlatformModule extends AbstractModule {

    @Override
    protected void configure() {
        // Install JNA bindings for Win32 APIs
        install(new WindowsModule());

        // Bind platform-specific implementations
        bind(SystemService.class).to(WindowsSystemService.class).in(Singleton.class);
        bind(DesktopService.class).to(WindowsDesktopService.class).in(Singleton.class);
        bind(KeyboardService.class).to(WindowsKeyboardService.class).in(Singleton.class);
        bind(MonitoringService.class).to(WindowsMonitoringService.class).in(Singleton.class);

        // Windows-only: Radeon display driver support
        install(new com.fatico.winthing.systems.radeon.Module());
    }
}
