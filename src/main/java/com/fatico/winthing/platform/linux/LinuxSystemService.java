package com.fatico.winthing.platform.linux;

import com.fatico.winthing.platform.ShellExecutor;
import com.fatico.winthing.systems.system.SystemService;
import com.fatico.winthing.windows.SystemException;
import com.google.inject.Inject;
import java.awt.Desktop;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux implementation of system operations using native commands.
 *
 * @since 2.0.0
 */
public class LinuxSystemService implements SystemService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public LinuxSystemService() {
        // No special initialization needed on Linux
    }

    @Override
    public void shutdown() throws SystemException {
        ShellExecutor.execute("systemctl", "poweroff");
    }

    @Override
    public void reboot() throws SystemException {
        ShellExecutor.execute("systemctl", "reboot");
    }

    @Override
    public void suspend() throws SystemException {
        ShellExecutor.execute("systemctl", "suspend");
    }

    @Override
    public void hibernate() throws SystemException {
        ShellExecutor.execute("systemctl", "hibernate");
    }

    @Override
    public void run(final String command, final String parameters, final String workingDirectory)
            throws SystemException {
        try {
            ProcessBuilder pb;
            if (parameters != null && !parameters.isEmpty()) {
                pb = new ProcessBuilder("/bin/sh", "-c",
                    Objects.requireNonNull(command) + " " + parameters);
            } else {
                pb = new ProcessBuilder("/bin/sh", "-c", Objects.requireNonNull(command));
            }
            if (workingDirectory != null && !workingDirectory.isEmpty()) {
                pb.directory(new java.io.File(workingDirectory));
            }
            pb.start();
        } catch (Exception ex) {
            throw new SystemException("Failed to run command: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void open(final String uri) throws SystemException {
        try {
            // Try xdg-open first (works on most Linux distros)
            ShellExecutor.executeAsync("xdg-open", Objects.requireNonNull(uri));
        } catch (Exception ex) {
            // Fallback to Java Desktop API
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(uri));
                } else {
                    throw new SystemException("Cannot open URI: no desktop support");
                }
            } catch (SystemException se) {
                throw se;
            } catch (Exception ex2) {
                throw new SystemException("Cannot open URI: " + uri, ex2);
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:magicnumber")
    public Map<Integer, String> findProcesses(final String nameFragment) {
        Objects.requireNonNull(nameFragment);
        final Map<Integer, String> processIds = new HashMap<>();
        try {
            String output = ShellExecutor.shell("ps -eo pid,comm | grep -i '"
                + nameFragment.replace("'", "") + "'");
            for (String line : output.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    try {
                        int pid = Integer.parseInt(parts[0]);
                        processIds.put(pid, parts[1]);
                    } catch (NumberFormatException ignored) {
                        // skip header lines
                    }
                }
            }
        } catch (Exception ex) {
            logger.debug("Could not find processes: {}", ex.getMessage());
        }
        return processIds;
    }
}
