package com.fatico.winthing.systems.monitoring;

/**
 * Platform-independent interface for system monitoring.
 *
 * <p>Implementations exist for Windows, Linux, and macOS.
 *
 * @since 2.0.0
 */
public interface MonitoringService {

    /**
     * Returns the system CPU load as a percentage (0.0 - 100.0), or -1 if unavailable.
     */
    double getCpuUsage();

    /**
     * Returns memory information: [totalBytes, availableBytes, usagePercent].
     */
    long[] getMemoryInfo();

    /**
     * Returns battery information: [acLineStatus, batteryPercent, batteryLifeTime].
     * acLineStatus: 0=offline, 1=online, 255=unknown
     * batteryPercent: 0-100, or 255 if unknown
     * batteryLifeTime: seconds remaining, or -1 if unknown
     */
    int[] getBatteryInfo();

    /**
     * Returns the title of the currently playing media, or null.
     */
    String getNowPlayingTitle();
}
