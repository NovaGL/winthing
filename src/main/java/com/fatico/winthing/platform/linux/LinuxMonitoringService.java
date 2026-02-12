package com.fatico.winthing.platform.linux;

import com.fatico.winthing.platform.ShellExecutor;
import com.fatico.winthing.systems.monitoring.MonitoringService;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux implementation of system monitoring using /proc, /sys, and playerctl.
 *
 * @since 2.0.0
 */
@SuppressWarnings("checkstyle:magicnumber")
public class LinuxMonitoringService implements MonitoringService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public LinuxMonitoringService() {
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
            long totalKb = 0;
            long availableKb = 0;
            Path meminfo = Paths.get("/proc/meminfo");
            try (BufferedReader reader = new BufferedReader(
                    new FileReader(meminfo.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("MemTotal:")) {
                        totalKb = parseMemInfoValue(line);
                    } else if (line.startsWith("MemAvailable:")) {
                        availableKb = parseMemInfoValue(line);
                    }
                }
            }
            if (totalKb > 0) {
                long totalBytes = totalKb * 1024;
                long availableBytes = availableKb * 1024;
                long usagePercent = Math.round(
                    (double) (totalKb - availableKb) / totalKb * 100.0);
                return new long[]{totalBytes, availableBytes, usagePercent};
            }
        } catch (final Exception exception) {
            logger.debug("Could not read memory info: {}", exception.getMessage());
        }
        return new long[]{-1, -1, -1};
    }

    private long parseMemInfoValue(String line) {
        // Format: "MemTotal:       16384000 kB"
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            return Long.parseLong(parts[1]);
        }
        return 0;
    }

    @Override
    public int[] getBatteryInfo() {
        try {
            Path batteryPath = findBatteryPath();
            if (batteryPath == null) {
                return new int[]{255, 255, -1};
            }

            int acLine = 255;
            int percent = 255;
            int lifeTime = -1;

            Path statusFile = batteryPath.resolve("status");
            if (Files.exists(statusFile)) {
                String status = Files.readString(statusFile).trim();
                acLine = status.equalsIgnoreCase("Charging")
                    || status.equalsIgnoreCase("Full") ? 1 : 0;
            }

            Path capacityFile = batteryPath.resolve("capacity");
            if (Files.exists(capacityFile)) {
                percent = Integer.parseInt(Files.readString(capacityFile).trim());
            }

            // Estimate remaining time from energy_now and power_now
            Path energyNow = batteryPath.resolve("energy_now");
            Path powerNow = batteryPath.resolve("power_now");
            if (Files.exists(energyNow) && Files.exists(powerNow) && acLine == 0) {
                long energy = Long.parseLong(Files.readString(energyNow).trim());
                long power = Long.parseLong(Files.readString(powerNow).trim());
                if (power > 0) {
                    lifeTime = (int) (energy * 3600L / power);
                }
            }

            return new int[]{acLine, percent, lifeTime};
        } catch (final Exception exception) {
            logger.debug("Could not read battery info: {}", exception.getMessage());
        }
        return new int[]{255, 255, -1};
    }

    private Path findBatteryPath() {
        Path powerSupply = Paths.get("/sys/class/power_supply");
        if (!Files.exists(powerSupply)) {
            return null;
        }
        try {
            return Files.list(powerSupply)
                .filter(p -> p.getFileName().toString().startsWith("BAT"))
                .findFirst()
                .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public String getNowPlayingTitle() {
        // Try playerctl (supports Spotify, VLC, etc. via MPRIS)
        try {
            String output = ShellExecutor.execute(5,
                "playerctl", "metadata", "--format",
                "{{ artist }} - {{ title }}");
            if (output != null && !output.isEmpty()
                    && !output.contains("No players found")) {
                return output;
            }
        } catch (Exception ex) {
            logger.debug("playerctl not available: {}", ex.getMessage());
        }
        return null;
    }
}
