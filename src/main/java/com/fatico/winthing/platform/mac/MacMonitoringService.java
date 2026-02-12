package com.fatico.winthing.platform.mac;

import com.fatico.winthing.platform.ShellExecutor;
import com.fatico.winthing.systems.monitoring.MonitoringService;
import com.google.inject.Inject;
import java.lang.management.ManagementFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * macOS implementation of system monitoring using sysctl, vm_stat, pmset,
 * and osascript.
 *
 * @since 2.0.0
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MacMonitoringService implements MonitoringService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public MacMonitoringService() {
        // No special initialization needed
    }

    @Override
    public double getCpuUsage() {
        try {
            final com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean)
                    ManagementFactory.getOperatingSystemMXBean();
            double load = osBean.getCpuLoad();
            if (load < 0) {
                return -1.0;
            }
            return Math.round(load * 10000.0) / 100.0;
        } catch (final Exception exception) {
            logger.debug("Could not read CPU usage: {}", exception.getMessage());
            return -1.0;
        }
    }

    @Override
    public long[] getMemoryInfo() {
        try {
            // Get total physical memory via sysctl
            String totalOutput = ShellExecutor.execute("sysctl", "-n", "hw.memsize");
            long totalBytes = Long.parseLong(totalOutput.trim());

            // Get page size
            String pageSizeOutput = ShellExecutor.execute("sysctl", "-n", "hw.pagesize");
            long pageSize = Long.parseLong(pageSizeOutput.trim());

            // Get free + inactive pages from vm_stat
            String vmStat = ShellExecutor.execute("vm_stat");
            long freePages = 0;
            long inactivePages = 0;
            for (String line : vmStat.split("\n")) {
                if (line.startsWith("Pages free:")) {
                    freePages = parseVmStatValue(line);
                } else if (line.startsWith("Pages inactive:")) {
                    inactivePages = parseVmStatValue(line);
                }
            }

            long availableBytes = (freePages + inactivePages) * pageSize;
            long usagePercent = Math.round(
                (double) (totalBytes - availableBytes) / totalBytes * 100.0);

            return new long[]{totalBytes, availableBytes, usagePercent};
        } catch (final Exception exception) {
            logger.debug("Could not read memory info: {}", exception.getMessage());
        }
        return new long[]{-1, -1, -1};
    }

    private long parseVmStatValue(String line) {
        // Format: "Pages free:                             1234."
        String value = line.replaceAll("[^0-9]", "");
        return value.isEmpty() ? 0 : Long.parseLong(value);
    }

    @Override
    public int[] getBatteryInfo() {
        try {
            String output = ShellExecutor.execute("pmset", "-g", "batt");
            if (output.contains("No battery") || output.contains("InternalBattery") == false) {
                return new int[]{255, 255, -1};
            }

            int acLine = output.contains("AC Power") ? 1 : 0;
            int percent = 255;
            int lifeTime = -1;

            // Parse: "InternalBattery-0 (id=...)  85%; charging; 0:45 remaining"
            for (String line : output.split("\n")) {
                if (line.contains("InternalBattery")) {
                    // Extract percentage
                    java.util.regex.Matcher pctMatcher =
                        java.util.regex.Pattern.compile("(\\d+)%").matcher(line);
                    if (pctMatcher.find()) {
                        percent = Integer.parseInt(pctMatcher.group(1));
                    }
                    // Extract remaining time "H:MM remaining"
                    java.util.regex.Matcher timeMatcher =
                        java.util.regex.Pattern.compile("(\\d+):(\\d+) remaining")
                            .matcher(line);
                    if (timeMatcher.find()) {
                        int hours = Integer.parseInt(timeMatcher.group(1));
                        int minutes = Integer.parseInt(timeMatcher.group(2));
                        lifeTime = hours * 3600 + minutes * 60;
                    }
                }
            }

            return new int[]{acLine, percent, lifeTime};
        } catch (final Exception exception) {
            logger.debug("Could not read battery info: {}", exception.getMessage());
        }
        return new int[]{255, 255, -1};
    }

    @Override
    public String getNowPlayingTitle() {
        // Try getting now playing info from Spotify
        try {
            String output = ShellExecutor.execute(5,
                "osascript", "-e",
                "tell application \"System Events\" to "
                    + "(name of processes) contains \"Spotify\"");
            if ("true".equals(output.trim())) {
                String state = ShellExecutor.execute(5,
                    "osascript", "-e",
                    "tell application \"Spotify\" to player state as string");
                if ("playing".equals(state.trim())) {
                    String artist = ShellExecutor.execute(5,
                        "osascript", "-e",
                        "tell application \"Spotify\" to artist of current track as string");
                    String track = ShellExecutor.execute(5,
                        "osascript", "-e",
                        "tell application \"Spotify\" to name of current track as string");
                    return artist + " - " + track;
                }
            }
        } catch (Exception ex) {
            logger.debug("Could not get Spotify info: {}", ex.getMessage());
        }

        // Try getting now playing info from Music.app (iTunes replacement)
        try {
            String output = ShellExecutor.execute(5,
                "osascript", "-e",
                "tell application \"System Events\" to "
                    + "(name of processes) contains \"Music\"");
            if ("true".equals(output.trim())) {
                String state = ShellExecutor.execute(5,
                    "osascript", "-e",
                    "tell application \"Music\" to player state as string");
                if ("playing".equals(state.trim())) {
                    String artist = ShellExecutor.execute(5,
                        "osascript", "-e",
                        "tell application \"Music\" to artist of current track as string");
                    String track = ShellExecutor.execute(5,
                        "osascript", "-e",
                        "tell application \"Music\" to name of current track as string");
                    return artist + " - " + track;
                }
            }
        } catch (Exception ex) {
            logger.debug("Could not get Music.app info: {}", ex.getMessage());
        }

        return null;
    }
}
