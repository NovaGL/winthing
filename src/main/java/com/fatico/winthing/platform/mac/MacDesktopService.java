package com.fatico.winthing.platform.mac;

import com.fatico.winthing.platform.ShellExecutor;
import com.fatico.winthing.systems.desktop.DesktopService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * macOS implementation of desktop operations using osascript (AppleScript).
 *
 * @since 2.0.0
 */
public class MacDesktopService implements DesktopService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public MacDesktopService() {
        // No special initialization needed
    }

    @Override
    public void closeActiveWindow() {
        try {
            ShellExecutor.osascript(
                "tell application \"System Events\" to keystroke \"w\" using command down");
        } catch (Exception ex) {
            logger.warn("Could not close active window: {}", ex.getMessage());
        }
    }

    @Override
    public void setDisplaySleep(final boolean sleep) {
        try {
            if (sleep) {
                ShellExecutor.execute("pmset", "displaysleepnow");
            } else {
                // Wake display by simulating a key press
                ShellExecutor.execute("caffeinate", "-u", "-t", "1");
            }
        } catch (Exception ex) {
            logger.warn("Could not control display power: {}", ex.getMessage());
        }
    }
}
