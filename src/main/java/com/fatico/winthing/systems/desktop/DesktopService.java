package com.fatico.winthing.systems.desktop;

/**
 * Platform-independent interface for desktop operations.
 *
 * <p>Implementations exist for Windows, Linux, and macOS.
 *
 * @since 2.0.0
 */
public interface DesktopService {

    /**
     * Closes the currently active/foreground window.
     */
    void closeActiveWindow();

    /**
     * Puts the display to sleep (on true) or wakes it up (on false).
     */
    void setDisplaySleep(boolean sleep);
}
