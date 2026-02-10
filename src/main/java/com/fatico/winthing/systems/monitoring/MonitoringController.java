package com.fatico.winthing.systems.monitoring;

import com.fatico.winthing.Settings;
import com.fatico.winthing.common.BaseController;
import com.fatico.winthing.messaging.MessagePublisher;
import com.fatico.winthing.messaging.QualityOfService;
import com.fatico.winthing.messaging.Registry;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitoringController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MonitoringService monitoringService;
    private final MessagePublisher publisher;
    private final long intervalSeconds;

    private volatile ScheduledExecutorService scheduler;

    @Inject
    @SuppressWarnings("this-escape")
    public MonitoringController(
            final Registry registry,
            final MonitoringService monitoringService,
            final MessagePublisher publisher,
            final Config config) {
        super("system");
        this.monitoringService = Objects.requireNonNull(monitoringService);
        this.publisher = Objects.requireNonNull(publisher);

        long interval = 30;
        try {
            interval = config.getLong(Settings.MONITORING_INTERVAL);
        } catch (final Exception exception) {
            logger.info("Using default monitoring interval of 30 seconds.");
        }
        this.intervalSeconds = interval;

        registry.addConnectionListener(this::start);
        registry.addDisconnectionListener(this::stop);
    }

    /**
     * Starts the periodic monitoring loop.  Called after MQTT connects.
     */
    public synchronized void start() {
        stop();
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "monitoring");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(
            this::publishMonitoringData,
            0,
            intervalSeconds,
            TimeUnit.SECONDS
        );
        logger.info(
            "Monitoring started (interval: {} seconds).", intervalSeconds
        );
    }

    /**
     * Stops the monitoring loop.  Called on MQTT disconnect.
     */
    public synchronized void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void publishMonitoringData() {
        try {
            publishCpuUsage();
            publishMemoryInfo();
            publishBatteryInfo();
            publishNowPlaying();
        } catch (final Exception exception) {
            logger.error("Error publishing monitoring data: {}", exception.getMessage());
        }
    }

    private void publishCpuUsage() {
        final double cpuUsage = monitoringService.getCpuUsage();
        if (cpuUsage >= 0) {
            publisher.publish(makeMessage(
                "monitoring/cpu_usage",
                new JsonPrimitive(cpuUsage),
                QualityOfService.AT_MOST_ONCE
            ));
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void publishMemoryInfo() {
        final long[] memInfo = monitoringService.getMemoryInfo();
        if (memInfo[0] >= 0) {
            final long totalMb = memInfo[0] / (1024 * 1024);
            final long availableMb = memInfo[1] / (1024 * 1024);
            final long usedMb = totalMb - availableMb;
            final long usagePercent = memInfo[2];

            final JsonObject ramJson = new JsonObject();
            ramJson.addProperty("total_mb", totalMb);
            ramJson.addProperty("available_mb", availableMb);
            ramJson.addProperty("used_mb", usedMb);
            ramJson.addProperty("usage_percent", usagePercent);

            publisher.publish(makeMessage(
                "monitoring/memory",
                ramJson,
                QualityOfService.AT_MOST_ONCE
            ));
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void publishBatteryInfo() {
        final int[] batteryInfo = monitoringService.getBatteryInfo();
        final int acLine = batteryInfo[0];
        final int percent = batteryInfo[1];
        final int lifeTime = batteryInfo[2];

        final JsonObject batteryJson = new JsonObject();
        batteryJson.addProperty("ac_plugged", acLine == 1);
        if (percent != 255) {
            batteryJson.addProperty("level", percent);
        }
        if (lifeTime >= 0) {
            batteryJson.addProperty("remaining_seconds", lifeTime);
        }

        // Only publish if battery data is meaningful (not a desktop without battery)
        if (percent != 255 || acLine != 255) {
            publisher.publish(makeMessage(
                "monitoring/battery",
                batteryJson,
                QualityOfService.AT_MOST_ONCE
            ));
        }
    }

    private void publishNowPlaying() {
        final String nowPlaying = monitoringService.getNowPlayingTitle();
        if (nowPlaying != null && !nowPlaying.isEmpty()) {
            publisher.publish(makeMessage(
                "monitoring/now_playing",
                new JsonPrimitive(nowPlaying),
                QualityOfService.AT_MOST_ONCE
            ));
        } else {
            publisher.publish(makeMessage(
                "monitoring/now_playing",
                new JsonPrimitive(""),
                QualityOfService.AT_MOST_ONCE
            ));
        }
    }
}
