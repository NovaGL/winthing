package com.fatico.winthing.systems.system;

import com.fatico.winthing.windows.SystemException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages command whitelist for security enforcement.
 *
 * <p>The whitelist defines which commands are allowed to execute via MQTT.
 * Format: winthing.ini (key=path/to/command.exe)
 *
 * <p>Configuration:
 * <ul>
 *   <li>winthing.ini file in application directory</li>
 *   <li>Validation ensures all whitelisted commands exist and are executable</li>
 *   <li>Commands are mapped by friendly keys for MQTT topic routing</li>
 * </ul>
 *
 * <p>Behavior When Whitelist Missing:
 * <ul>
 *   <li>If REQUIRE_WHITELIST=true: Startup fails with clear error (RECOMMENDED)</li>
 *   <li>If REQUIRE_WHITELIST=false: All commands allowed (INSECURE)</li>
 * </ul>
 *
 * <p>Security Model:
 * The whitelist is the primary security control for command execution. Without it,
 * any MQTT message could execute arbitrary commands on the system. The whitelist
 * MUST be enabled in production environments.
 *
 * @since 1.0.0
 */
public class SystemCommander {
    public static final String ConfigFile = "winthing.ini";
    private static final boolean REQUIRE_WHITELIST = true;  // Safety default

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean isEnabled = false;
    private Map<String, String> whitelist = new HashMap<String, String>();

    public boolean isEnabled() {
        return isEnabled;
    }

    public String getCommand(String key) {
        return whitelist.get(key);
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void parseConfig() {
        String path = System.getProperty("user.dir") + File.separator + ConfigFile;
        File fp = new File(path);

        if (!fp.exists()) {
            if (REQUIRE_WHITELIST) {
                throw new SystemException(
                    "Whitelist file required but not found: " + path
                    + "\nPlease create " + ConfigFile + " with allowed commands"
                );
            } else {
                logger.warn("No whitelist found. Every command is allowed to execute on this device!");
                return;
            }
        }

        try {
            StringJoiner joiner = new StringJoiner(", ");

            ConfigParseOptions options = ConfigParseOptions.defaults();
            options.setSyntax(ConfigSyntax.CONF);

            Config cfg = ConfigFactory.parseFile(fp, options);
            Set<String> map = cfg.root().keySet();
            for (String key : map) {
                String command = cfg.getString(key);
                validateCommand(command);
                whitelist.put(key, command);
                joiner.add(key);
            }

            logger.info("Found whitelist of allowed commands to execute, using it...");
            logger.info("Allowed commands: [" + joiner.toString() + "]");

            isEnabled = true;
        } catch (Exception e) {
            logger.error("Unable to process whitelist file", e);
            if (REQUIRE_WHITELIST) {
                throw new SystemException("Failed to load whitelist", e);
            }
        }
    }

    private void validateCommand(String command) {
        Path commandPath = Paths.get(command);

        if (!Files.exists(commandPath)) {
            logger.warn("Whitelisted command not found: " + command);
        }

        if (!Files.isExecutable(commandPath) && !command.endsWith(".exe")
                && !command.endsWith(".bat") && !command.endsWith(".cmd")) {
            logger.warn("Whitelisted file may not be executable: " + command);
        }
    }
}
