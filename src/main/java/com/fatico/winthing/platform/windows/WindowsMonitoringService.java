package com.fatico.winthing.platform.windows;

import com.fatico.winthing.systems.monitoring.MonitoringService;
import com.fatico.winthing.windows.jna.Kernel32;
import com.fatico.winthing.windows.jna.SystemPowerStatus;
import com.fatico.winthing.windows.jna.User32;
import com.google.inject.Inject;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import java.lang.management.ManagementFactory;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Windows implementation of system monitoring via JNA/Win32 API.
 *
 * @since 1.5.0
 */
@SuppressWarnings({
    "checkstyle:abbreviationaswordinname",
    "checkstyle:membername"})
public class WindowsMonitoringService implements MonitoringService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Kernel32 kernel32;
    private final User32 user32;

    @Inject
    public WindowsMonitoringService(final Kernel32 kernel32, final User32 user32) {
        this.kernel32 = Objects.requireNonNull(kernel32);
        this.user32 = Objects.requireNonNull(user32);
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
            final WinBase.MEMORYSTATUSEX memStatus = new WinBase.MEMORYSTATUSEX();
            if (kernel32.GlobalMemoryStatusEx(memStatus)) {
                return new long[]{
                    memStatus.ullTotalPhys.longValue(),
                    memStatus.ullAvailPhys.longValue(),
                    memStatus.dwMemoryLoad.intValue()
                };
            }
        } catch (final Exception exception) {
            logger.debug("Could not read memory info: {}", exception.getMessage());
        }
        return new long[]{-1, -1, -1};
    }

    @Override
    @SuppressWarnings("checkstyle:magicnumber")
    public int[] getBatteryInfo() {
        try {
            final SystemPowerStatus status = new SystemPowerStatus();
            if (kernel32.GetSystemPowerStatus(status)) {
                return new int[]{
                    Byte.toUnsignedInt(status.acLineStatus),
                    Byte.toUnsignedInt(status.batteryLifePercent),
                    status.batteryLifeTime
                };
            }
        } catch (final Exception exception) {
            logger.debug("Could not read battery info: {}", exception.getMessage());
        }
        return new int[]{255, 255, -1};
    }

    @Override
    public String getNowPlayingTitle() {
        final String[] result = new String[]{null};

        user32.EnumWindows(new WinUser.WNDENUMPROC() {
            @Override
            public boolean callback(final WinDef.HWND hwnd, final Pointer data) {
                if (!user32.IsWindowVisible(hwnd)) {
                    return true;
                }
                final int length = user32.GetWindowTextLength(hwnd);
                if (length == 0) {
                    return true;
                }
                final char[] buffer = new char[length + 1];
                user32.GetWindowText(hwnd, buffer, buffer.length);
                final String title = Native.toString(buffer).trim();

                if (isMediaTitle(title)) {
                    result[0] = title;
                    return false;
                }
                return true;
            }
        }, null);

        return result[0];
    }

    @SuppressWarnings("checkstyle:cyclomaticcomplexity")
    private boolean isMediaTitle(final String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        final String lower = title.toLowerCase();
        if (lower.contains("spotify") && title.contains(" - ")
                && !title.equals("Spotify")
                && !title.equals("Spotify Premium")
                && !title.equals("Spotify Free")) {
            return true;
        }
        if (lower.contains("vlc media player") && title.contains(" - ")) {
            return true;
        }
        if (lower.contains("windows media player") && title.contains(" - ")) {
            return true;
        }
        if (lower.contains("foobar2000") && (title.contains(" - ") || title.contains("["))) {
            return true;
        }
        if (lower.contains("musicbee") && title.contains(" - ")) {
            return true;
        }
        if (lower.contains("aimp") && title.contains(" - ")) {
            return true;
        }
        if (lower.contains("winamp") && title.contains(" - ")) {
            return true;
        }
        if (lower.contains("youtube") && title.contains(" - ")) {
            return true;
        }
        if ((lower.contains("itunes") || lower.contains("apple music"))
                && title.contains(" - ")) {
            return true;
        }
        return false;
    }
}
