package com.fatico.winthing.platform.linux;

import com.fatico.winthing.platform.ShellExecutor;
import com.fatico.winthing.systems.desktop.DesktopService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux implementation of desktop operations using xdotool and xset.
 *
 * <p>Requires xdotool to be installed for window management and
 * xset for display power management.
 *
 * @since 2.0.0
 */
public class LinuxDesktopService implements DesktopService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public LinuxDesktopService() {
        // No special initialization needed
    }

    @Override
    public void closeActiveWindow() {
        try {
            ShellExecutor.execute("xdotool", "getactivewindow", "windowclose");
        } catch (Exception ex) {
            // Fallback: try wmctrl
            try {
                ShellExecutor.execute("wmctrl", "-c", ":ACTIVE:");
            } catch (Exception ex2) {
                logger.warn("Could not close active window. "
                    + "Install xdotool or wmctrl for window management.");
            }
        }
    }

    @Override
    public void setDisplaySleep(final boolean sleep) {
        try {
            if (sleep) {
                ShellExecutor.execute("xset", "dpms", "force", "off");
            } else {
                ShellExecutor.execute("xset", "dpms", "force", "on");
            }
        } catch (Exception ex) {
            logger.warn("Could not control display power. Install xset (xorg-xset).");
        }
    }
}
