# Release notes

## 2.0.0

- **Cross-platform support**: WinThing now runs on Linux and macOS in addition to Windows.
- Added platform detection (`OsDetector`) to automatically select the correct implementation at startup.
- Extracted platform-independent interfaces for `SystemService`, `DesktopService`, `KeyboardService`, and `MonitoringService`.
- **Windows**: Existing Win32 JNA-based implementations preserved in `platform.windows` package.
- **Linux**: New implementations using `systemctl`, `xdotool`, `xset`, `playerctl`, `/proc/meminfo`, and `/sys/class/power_supply`.
- **macOS**: New implementations using `osascript` (AppleScript), `pmset`, `sysctl`, `vm_stat`, and native Spotify/Music.app integration.
- Refactored `KeyboardKey` and `MouseButton` enums to remove Windows-specific `WinDef.WORD` dependency.
- GUI (`WindowGui`) now supports headless mode for servers without a display.
- `ConsoleLogger` gracefully handles headless environments.
- Moved Launch4j Windows `.exe` packaging to a Maven profile (only active on Windows).
- Updated CI workflow to test on Ubuntu, macOS, and Windows.
- Updated `HomeAssistantDiscovery` device model to be platform-neutral.
- Updated `SystemCommander` whitelist validation to be cross-platform.
- Fixed `HomeAssistantDiscovery.publishBinarySensor` call for the "online" sensor.
- Added `ShellExecutor` utility for running native shell commands on Linux/macOS.
- Platform-specific Guice modules (`WindowsPlatformModule`, `LinuxPlatformModule`, `MacPlatformModule`) for clean dependency injection.
- Radeon display driver support remains Windows-only.

## 1.5.0

- Updated to require OpenJDK 21.
- Updated all dependencies to latest JDK 21-compatible versions.
- Replaced deprecated `finalize()` with `java.lang.ref.Cleaner` in RadeonService.
- Replaced Guava `Charsets` with `java.nio.charset.StandardCharsets`.
- Updated all Maven plugins to modern versions.
- Updated GitHub Actions workflow to JDK 21 with latest action versions.
- Fixed topic prefix to preserve leading slashes for brokers that require them.
- Added system monitoring: CPU usage, RAM (total/available/used/percent), Battery info.
- Added now-playing media track detection (read-only window title scanning).
- Media detection is strictly read-only and will not trigger playback on wake from hibernation.
- Added configurable `monitoring_interval` setting (default: 30 seconds).
- Added dedicated media control commands: play/pause, next track, previous track, stop.
- Media control commands are command-only and never auto-triggered on MQTT connect or reconnect.

## 1.4.2

- Updated build process and minor security fixes.

## 1.4.1

- Fix winthing.ini whitelist file and command execution
- Format log in the console and log file

## 1.4.0

- Fix duplicate system/system in winthing/system/online topic
- Fix Windows executable build for Java 11
- Fix logging to file when -debug parameter is passed
- Change how GUI is created
- Fix check whether file logging should be enabled

## 1.3.0

- Add tray icon and GUI console window
- Read settings from file
- Add configurable whitelist of allowed commands to execute
- Create file logs only when enabled
- Remove prefix from config parameters
- Update dependencies
- Update build for Java 11 

## 1.2.0

- Original version
