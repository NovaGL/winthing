# WinThing

![Build Status](https://github.com/msiedlarek/winthing/workflows/build/badge.svg)

A modular background service that makes your computer remotely controllable through MQTT. For home automation and Internet of Things. Supports **Windows**, **Linux**, and **macOS**.

## Requirements

Java 21 or greater.

### Platform-Specific Requirements

**Linux** (optional, for full functionality):
- `xdotool` - window management and keyboard simulation
- `xset` - display power management (X11)
- `playerctl` - media player detection (MPRIS)
- `systemctl` - power management (shutdown/reboot/suspend/hibernate)

**macOS** (no additional requirements):
- Uses native `osascript` (AppleScript), `pmset`, and system commands

**Windows** (no additional requirements):
- Uses native Win32 API via JNA

## Running

Download the JAR file from [Releases page](https://github.com/msiedlarek/winthing/releases) and execute it:

    java -jar winthing-2.0.0.jar

On Windows, you can also use the `.exe` wrapper:

    winthing-2.0.0.exe

For Linux/macOS headless servers (no GUI):

    java -Djava.awt.headless=true -jar winthing-2.0.0.jar

## Configuration

Configuration parameters can be passed from command line or they can be placed in configuration files in the working directory from where you launch WinThing.

<table>
<tr><th>Property</th><th>Description</th><th>Default</th><th>Env Variable</th>
<tr><td>broker</td><td>URL of the MQTT broker to use</td><td>127.0.0.1:1883</td><td>WINTHING_BROKER</td></tr>
<tr><td>mqtt_protocol</td><td>Protocol to use for MQTT connection: "tcp", "ssl", or "tls". Use "ssl" for encrypted connections (recommended)</td><td>ssl</td><td>WINTHING_MQTT_PROTOCOL</td></tr>
<tr><td>username</td><td>Username used when connecting to MQTT broker</td><td>mqtt</td><td>WINTHING_USERNAME</td></tr>
<tr><td>password</td><td>Password used when connecting to MQTT broker</td><td>mqtt</td><td>WINTHING_PASSWORD</td></tr>
<tr><td>clientid</td><td>Client ID to present to the broker</td><td>WinThing</td><td>WINTHING_CLIENT_ID</td></tr>
<tr><td>prefix</td><td>Prefix for all MQTT topics used by this WinThing instance. Leading slashes are preserved for brokers that require them.</td><td>winthing</td><td>WINTHING_PREFIX</td></tr>
<tr><td>reconnect</td><td>Time interval between connection attempts in seconds</td><td>5</td><td>-</td></tr>
<tr><td>monitoring_interval</td><td>How often to publish system monitoring data in seconds</td><td>30</td><td>-</td></tr>
<tr><td>homeassistant_discovery</td><td>Enable Home Assistant MQTT Auto-Discovery</td><td>true</td><td>WINTHING_HA_DISCOVERY</td></tr>
<tr><td>homeassistant_prefix</td><td>Home Assistant discovery topic prefix</td><td>homeassistant</td><td>WINTHING_HA_PREFIX</td></tr>
<tr><td>device_name</td><td>Friendly device name for Home Assistant</td><td>hostname</td><td>WINTHING_DEVICE_NAME</td></tr>
</table>

### Configuration Priority

WinThing loads configuration from multiple sources in this order (highest priority first):

1. **System Properties** (command line: `-Dkey=value`)
2. **Environment Variables** (e.g., `WINTHING_BROKER`)
3. **winthing.conf** file (in working directory)
4. **Built-in Defaults**

### Command line parameters

Example how to pass parameters from command line:

	java -Dbroker="127.0.0.1:1883" -jar winthing-2.0.0.jar

### Environment variables

For better security, use environment variables instead of command-line parameters:

	export WINTHING_BROKER="192.168.1.100:8883"
	export WINTHING_MQTT_PROTOCOL="ssl"
	export WINTHING_PASSWORD="secret"
	java -jar winthing-2.0.0.jar

### winthing.conf

WinThing will look for this file in the current working directory (directory from where you launched WinThing). Create this file and put desired parameters into it.

Example file:

	broker = "127.0.0.1:1883"
	username = "mqtt"
	password = "somesecret"

### winthing.ini

By default WinThing executes any command it receives in the system/commands/run topic. Create this file in the current working directory to whitelist only specific commands. The file contains an unique string identifier (used as payload in the MQTT message, see below) and path to executable.

Example file (Windows):

	notepad = "c:/windows/system32/notepad.exe"
	adobe = "c:\\program files\\adobe\\reader.exe"

Example file (Linux/macOS):

	editor = "/usr/bin/nano"
	browser = "/usr/bin/firefox"

*Note: On Windows you can use slash* ' / ' *or double backslash* ' \\\\ ' *as path separator.*

## Cross-Platform Support

WinThing automatically detects the operating system at startup and loads the appropriate platform-specific implementation:

| Feature | Windows | Linux | macOS |
|---------|---------|-------|-------|
| Shutdown/Reboot | Win32 API | systemctl | AppleScript |
| Suspend/Hibernate | Win32 API | systemctl | pmset |
| Run commands | ShellExecute | /bin/sh | /bin/sh |
| Open URIs | ShellExecute | xdg-open | open |
| Close window | Win32 API | xdotool/wmctrl | AppleScript |
| Display sleep | Win32 API | xset | pmset |
| Keyboard simulation | SendInput | xdotool | AppleScript |
| CPU monitoring | JMX | JMX | JMX |
| Memory monitoring | Win32 API | /proc/meminfo | sysctl/vm_stat |
| Battery monitoring | Win32 API | /sys/class/power_supply | pmset |
| Now playing | Window titles | playerctl (MPRIS) | AppleScript |
| Radeon display | ATI ADL | Not available | Not available |

## Home Assistant Integration

WinThing supports **MQTT Auto-Discovery** for seamless integration with Home Assistant. When enabled, all sensors, buttons, and switches automatically appear in Home Assistant without manual configuration.

### Features

**Automatic Discovery** - All entities appear in Home Assistant on connection
**Device Grouping** - All sensors/controls grouped under one device
**Rich Metadata** - Proper device classes, icons, and units pre-configured
**Zero Configuration** - Works out of the box with default settings

### What Gets Discovered

**Sensors** (Read-only monitoring):
- Online/Offline status
- CPU usage (%)
- Memory usage and availability (MB, %)
- Battery level and charging status
- Now playing media

**Buttons** (One-time actions):
- Shutdown, Reboot, Suspend, Hibernate
- Media controls (Play/Pause, Next, Previous, Stop)

**Switches** (On/Off controls):
- Display sleep

### Configuration

```conf
# winthing.conf
homeassistant_discovery = true              # Enable discovery (default: true)
homeassistant_prefix = "homeassistant"      # HA discovery prefix (default)
device_name = "My Desktop PC"               # Friendly name in HA (default: hostname)
```

### Example Home Assistant Result

After WinThing connects, you'll see a device named "My Desktop PC" with all entities:
- `sensor.my_desktop_pc_cpu_usage`
- `sensor.my_desktop_pc_memory_usage`
- `button.my_desktop_pc_shutdown`
- `switch.my_desktop_pc_display_sleep`
- etc.

All entities include proper icons, units, and device classes for seamless integration.

## Security Considerations

WinThing provides remote control capabilities for your system. Follow these security best practices:

### Command Execution Protection

1. **Whitelist is REQUIRED**: The `winthing.ini` whitelist file is now mandatory. WinThing will not start without it. This prevents arbitrary command execution.
2. **Input Validation**: All command parameters are validated to prevent injection attacks. Commands with potentially dangerous characters (&, |, ;, `) are rejected.
3. **Path Traversal Protection**: Working directory paths are validated to ensure they stay within user home or temp directories.

### Network Security

1. **Use TLS/SSL**: Configure MQTT with SSL encryption to protect credentials and commands in transit.
   ```
   mqtt_protocol = "ssl"
   broker = "your-broker.example.com:8883"
   ```
2. **Secure Broker**: Only connect to trusted MQTT brokers with strong authentication enabled.
3. **Network Isolation**: Run WinThing on trusted networks only. Use firewall rules to restrict MQTT broker access.

### Configuration Security

1. **File-based Credentials**: Use `winthing.conf` file instead of command-line parameters to avoid password exposure in process lists.
2. **File Permissions**: Restrict access to `winthing.conf` (contains credentials) to authorized users only.
3. **Never Commit Secrets**: Do not commit credentials to version control. Use environment variables or secure vaults.

### Monitoring

1. **Review Logs**: Regularly check logs for suspicious activity or unauthorized command attempts.
2. **Audit Whitelist**: Periodically review `winthing.ini` to ensure only necessary commands are whitelisted.
3. **Update Regularly**: Keep WinThing and Java runtime updated for security patches.

### Defense in Depth

Even with these protections, WinThing grants significant system access. Consider:
- Running WinThing with a limited user account when possible
- Using firewall rules to restrict outbound connections
- Monitoring MQTT traffic for anomalies
- Implementing rate limiting on MQTT broker to prevent abuse

## Logging

You can open application log by clicking on the tray icon (Windows/Linux with GUI). To log into **winthing.log** file in the current working directory run WinThing with the **-debug** parameter.

	java -jar winthing-2.0.0.jar -debug

On Windows:

	winthing.exe -debug

## Supported messages

The payload of all messages is either empty or a valid JSON element (possibly a primitive, like a single integer). This means, specifically, that if an argument is supposed to be a single string, it should be sent in double quotes.

Example valid message payloads:

* `123`
* `true`
* `"notepad.exe"`
* `[1024, 768]`
* `["notepad.exe", "C:\\file.txt", "C:\\"]` (note that JSON string requires escaped backslash)

### Broadcast status

#### System

**Topic:** winthing/system/online<br>
**Payload:** state:boolean<br>
**QoS:** 2<br>
 **Persistent:** yes<br>

True when WinThing is running, false otherwise. WinThing registers a "last will" message with the broker to notify clients when WinThing disconnects.

### Commands

#### System

**Topic:** winthing/system/commands/shutdown<br>
**Payload:** -

Trigger immediate system shutdown.

---

**Topic:** winthing/system/commands/reboot<br>
**Payload:** -

Trigger immediate system reboot.

---

**Topic:** winthing/system/commands/suspend<br>
**Payload:** -

Trigger immediate system suspend.

---

**Topic:** winthing/system/commands/hibernate<br>
**Payload:** -

Trigger immediate system hibernate.

---

**Topic:** winthing/system/commands/run<br>
**Payload:** [command:string, arguments:string, workingDirectory:string]

Run a command. Arguments and working directory are optional (empty string and null by default).<br>
If whitelist is enabled, only the command as unique identifier is required. The identifier is checked against the whitelist file (see **winthing.ini** above).

---

**Topic:** winthing/system/commands/open<br>
**Payload:** uri:string

Opens a URI, like a website in a browser or a disk location in a file browser.

#### Media Controls

**Topic:** winthing/system/commands/media_play_pause<br>
**Payload:** -

Toggles media play/pause. This command is only executed when explicitly sent; it is never auto-triggered on MQTT connect or reconnect.

---

**Topic:** winthing/system/commands/media_next<br>
**Payload:** -

Skips to the next media track.

---

**Topic:** winthing/system/commands/media_prev<br>
**Payload:** -

Goes back to the previous media track.

---

**Topic:** winthing/system/commands/media_stop<br>
**Payload:** -

Stops media playback.

#### Desktop

**Topic:** winthing/desktop/commands/close_active_window<br>
**Payload:** -

Closes currently active window.

---

**Topic:** winthing/desktop/commands/set_display_sleep<br>
**Payload:** displaySleep:boolean

Puts the display to sleep (on true) or wakes it up (on false).

#### Keyboard

**Topic:** winthing/keyboard/commands/press_keys<br>
**Payload:** [key:string...]

Simulates pressing of given set of keyboard keys. Keys are specified by name. List of available key names and aliases can be found [here](src/main/java/com/fatico/winthing/windows/input/KeyboardKey.java).

### Monitoring

WinThing periodically publishes system monitoring data. The interval is configurable via the `monitoring_interval` setting (default: 30 seconds). Monitoring data is published automatically while WinThing is connected to the MQTT broker.

#### CPU Usage

**Topic:** winthing/system/monitoring/cpu_usage<br>
**Payload:** cpuPercent:number<br>
**QoS:** 0

Current system CPU usage as a percentage (0.0 - 100.0).

---

#### Memory

**Topic:** winthing/system/monitoring/memory<br>
**Payload:** `{"total_mb":number, "available_mb":number, "used_mb":number, "usage_percent":number}`<br>
**QoS:** 0

Current system memory information in megabytes and usage percentage.

---

#### Battery

**Topic:** winthing/system/monitoring/battery<br>
**Payload:** `{"ac_plugged":boolean, "level":number, "remaining_seconds":number}`<br>
**QoS:** 0

Battery information. Only published on systems with a battery. `level` is 0-100 percent. `remaining_seconds` is the estimated battery life remaining (omitted if unknown).

---

#### Now Playing

**Topic:** winthing/system/monitoring/now_playing<br>
**Payload:** title:string<br>
**QoS:** 0

The currently playing media title. Detection method varies by platform:
- **Windows**: Window title scanning (Spotify, VLC, foobar2000, MusicBee, AIMP, Winamp, iTunes, YouTube in browser, etc.)
- **Linux**: MPRIS via playerctl (Spotify, VLC, and any MPRIS-compatible player)
- **macOS**: AppleScript queries to Spotify and Music.app

An empty string is published when nothing is playing.

---

#### ATI Radeon display driver (Windows only)

**Topic:** winthing/radeon/commands/set_best_resolution<br>
**Payload:** -

Sets the screen to the best available resolution.

---

**Topic:** winthing/radeon/commands/set_resolution<br>
**Payload:** [widthInPixels:integer, heightInPixels:integer]

Sets the screen to the given resolution.

## Building

Maven is required to build the application.

    mvn clean package

On Windows, a `.exe` wrapper is automatically created during the build. On Linux/macOS, only the JAR is produced.

To run static analysis tools, use these commands:

    mvn checkstyle:check
    mvn pmd:check
    mvn spotbugs:check

## License

Copyright 2015-2020 Mikolaj Siedlarek &lt;mikolaj@siedlarek.pl&gt;

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this software except in compliance with the License.
You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
