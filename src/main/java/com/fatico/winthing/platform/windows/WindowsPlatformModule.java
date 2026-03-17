package com.fatico.winthing.platform.windows;

import com.fatico.winthing.systems.desktop.DesktopService;
import com.fatico.winthing.systems.keyboard.KeyboardService;
import com.fatico.winthing.systems.monitoring.MonitoringService;
import com.fatico.winthing.systems.system.SystemService;
import com.fatico.winthing.windows.WindowsModule;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module that binds all Windows-specific service implementations.
 *
 * @since 2.0.0
 */
public class WindowsPlatformModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(WindowsPlatformModule.class);

    @Override
    protected void configure() {
        // Install JNA bindings for Win32 APIs
        install(new WindowsModule());

        // Bind platform-specific implementations
        bind(SystemService.class).to(WindowsSystemService.class).in(Singleton.class);
        bind(DesktopService.class).to(WindowsDesktopService.class).in(Singleton.class);
        bind(KeyboardService.class).to(WindowsKeyboardService.class).in(Singleton.class);
        bind(MonitoringService.class).to(WindowsMonitoringService.class).in(Singleton.class);

        // Windows-only: Radeon display driver support (optional - requires AMD GPU + drivers)
        // Force AtiAdl class initialization here (before install) so that any native load
        // failure - which Java wraps in ExceptionInInitializerError, an Error not caught by
        // "Exception" - is caught by our Throwable handler instead of leaking into Guice.
        // If the load succeeds the class is already initialized and Module.configure() reuses
        // the cached static instance safely.
        try {
            // Access INSTANCE to force class initialization; throws if AMD library absent.
            // Objects.requireNonNull prevents a dead-store warning while keeping the check.
            java.util.Objects.requireNonNull(com.fatico.winthing.systems.radeon.jna.AtiAdl.INSTANCE);
            install(new com.fatico.winthing.systems.radeon.Module());
        } catch (Throwable e) {
            logger.info("AMD Radeon ADL library not available, skipping Radeon support.");
        }
    }
}
