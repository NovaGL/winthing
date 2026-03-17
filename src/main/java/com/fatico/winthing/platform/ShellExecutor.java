package com.fatico.winthing.platform;

import com.fatico.winthing.windows.SystemException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for executing native shell commands on Linux and macOS.
 *
 * @since 2.0.0
 */
public final class ShellExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellExecutor.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private ShellExecutor() {
        // Utility class
    }

    /**
     * Executes a command and returns the trimmed stdout output.
     */
    public static String execute(String... command) {
        return execute(DEFAULT_TIMEOUT_SECONDS, command);
    }

    /**
     * Executes a command with a timeout and returns the trimmed stdout output.
     */
    public static String execute(int timeoutSeconds, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // merge stderr into stdout so failures are visible
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append('\n');
                    }
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new SystemException("Command timed out: " + String.join(" ", command));
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                LOGGER.debug("Command exited with code {}: {} — {}",
                    exitCode, String.join(" ", command), output.toString().trim());
            }

            return output.toString().trim();
        } catch (SystemException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.debug("Command failed: {}", String.join(" ", command), ex);
            throw new SystemException("Command execution failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Executes a command without waiting for output (fire-and-forget).
     */
    public static void executeAsync(String... command) {
        try {
            new ProcessBuilder(command).start();
        } catch (Exception ex) {
            throw new SystemException("Failed to launch command: " + ex.getMessage(), ex);
        }
    }

    /**
     * Executes a shell command via /bin/sh -c (Linux/macOS).
     */
    public static String shell(String command) {
        return execute("/bin/sh", "-c", command);
    }

    /**
     * Executes an AppleScript command on macOS.
     */
    public static String osascript(String script) {
        return execute("osascript", "-e", script);
    }
}
