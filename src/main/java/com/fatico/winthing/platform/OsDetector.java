package com.fatico.winthing.platform;

import java.util.Locale;

/**
 * Detects the current operating system at runtime.
 *
 * @since 2.0.0
 */
public final class OsDetector {

    public enum Os {
        WINDOWS,
        LINUX,
        MAC,
        UNKNOWN
    }

    private static final Os CURRENT_OS;

    static {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            CURRENT_OS = Os.WINDOWS;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            CURRENT_OS = Os.MAC;
        } else if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
            CURRENT_OS = Os.LINUX;
        } else {
            CURRENT_OS = Os.UNKNOWN;
        }
    }

    private OsDetector() {
        // Utility class
    }

    public static Os getOs() {
        return CURRENT_OS;
    }

    public static boolean isWindows() {
        return CURRENT_OS == Os.WINDOWS;
    }

    public static boolean isLinux() {
        return CURRENT_OS == Os.LINUX;
    }

    public static boolean isMac() {
        return CURRENT_OS == Os.MAC;
    }
}
