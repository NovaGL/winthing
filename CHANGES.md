# Release notes

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
