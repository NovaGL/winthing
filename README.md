# WinThing

![Build Status](https://github.com/msiedlarek/winthing/workflows/build/badge.svg)

A modular background service that makes Windows remotely controllable through MQTT. For home automation and Internet of Things.

## :warning: Deprecation Notice :warning:

**WinThing is no longer actively maintained.** As an alternative, please consider [IOT Link](https://iotlink.gitlab.io/), which is actively maintained and has a lot more features, including most of WinThing's features.

## Requirements

Java 21 or greater.

## Running

Download either JAR or EXE file from [Releases page](https://github.com/msiedlarek/winthing/releases) and execute it:

	target/winthing-1.5.0.exe
    java -jar target/winthing-1.5.0.jar

## Configuration

Configuration parameters can be passed from command line or they can be placed in configuration files in the working directory from where you launch WinThing.

<table>
<tr><th>Property</th><th>Description</th><th>Default</th>
<tr><td>broker</td><td>URL of the MQTT broker to use</td><td>127.0.0.1:1883</td></tr>
<tr><td>mqtt_protocol</td><td>Protocol to use for MQTT connection: "tcp", "ssl", or "tls". Use "ssl" for encrypted connections (recommended)</td><td>ssl</td></tr>
<tr><td>username</td><td>Username used when connecting to MQTT broker</td><td>mqtt</td></tr>
<tr><td>password</td><td>Password used when connecting to MQTT broker</td><td>mqtt</td></tr>
<tr><td>clientid</td><td>Client ID to present to the broker</td><td>WinThing</td></tr>
<tr><td>prefix</td><td>Prefix for all MQTT topics used by this WinThing instance. Leading slashes are preserved for brokers that require them.</td><td>winthing</td></tr>
<tr><td>reconnect</td><td>Time interval between connection attempts in seconds</td><td>5</td></tr>
<tr><td>monitoring_interval</td><td>How often to publish system monitoring data in seconds</td><td>30</td></tr>
</table>

### Command line parameters

Example how to pass parameters from command line:

	java -Dbroker="127.0.0.1:1883" -jar winthing-1.2.0.jar

### winthing.conf

WinThing will look for this file in the current working directory (directory from where you launched WinThing). Create this file and put desired parameters into it.

Example file:

	broker = "127.0.0.1:1883"
	username = "mqtt"
	password = "somesecret"

### winthing.ini

By default WinThing executes any command it receives in the system/commands/run topic. Create this file in the current working directory to whitelist only specific commands. The file contains an unique string identifier (used as payload in the MQTT message, see below) and path to executable.

Example file:

	notepad = "c:/windows/system32/notepad.exe"
	adobe = "c:\\program files\\adobe\\reader.exe"

*Note you can use slash* ' / ' *or double backslash* ' \\\\ ' *as path separator.*

## Security Considerations

WinThing provides remote control capabilities for Windows systems. Follow these security best practices:

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
- Running WinThing with a limited user account (not Administrator) when possible
- Using Windows Firewall to restrict outbound connections
- Monitoring MQTT traffic for anomalies
- Implementing rate limiting on MQTT broker to prevent abuse

## Logging

You can open application log by clicking on the tray icon. To log into **winthing.log** file in the current working directory run WinThing with the **-debug** parameter.

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
If whitelist is enabled, only the command as unique identifier is required. The identifier is checked against the whitelist file (see **whitelist.ini** above).

---

**Topic:** winthing/system/commands/open<br>
**Payload:** uri:string

Opens an URI, like a website in a browser or a disk location in a file browser.

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

The window title of a detected media player (Spotify, VLC, foobar2000, MusicBee, AIMP, Winamp, iTunes, YouTube in browser, etc.). An empty string is published when nothing is playing. Detection is **read-only** (window title scanning only) and will never cause media to play, pause, or resume, including after waking from hibernation or sleep.

---

#### ATI Radeon display driver

**Topic:** winthing/radeon/commands/set_best_resolution<br>
**Payload:** -

Sets the screen to the best available resolution.

---

**Topic:** winthing/radeon/commands/set_resolution<br>
**Payload:** [widthInPixels:integer, heightInPixels:integer]

Sets the screen to the given resolution.

## Building

Maven is required to build the application. For convenience the Maven build file contains execution to produce a Windows executable.

    mvn clean package

To run static analysis tools, use these commands:

    mvn checkstyle:check
    mvn pmd:check
    mvn spotbugs:check

## License

Copyright 2015-2020 Mikołaj Siedlarek &lt;mikolaj@siedlarek.pl&gt;

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this software except in compliance with the License.
You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
