package com.fatico.winthing.systems.system;

import com.fatico.winthing.windows.SystemException;
import java.util.Map;

/**
 * Platform-independent interface for system operations (shutdown, reboot, etc.).
 *
 * <p>Implementations exist for Windows, Linux, and macOS.
 *
 * @since 2.0.0
 */
public interface SystemService {

    void shutdown() throws SystemException;

    void reboot() throws SystemException;

    void suspend() throws SystemException;

    void hibernate() throws SystemException;

    void run(String command, String parameters, String workingDirectory) throws SystemException;

    void open(String uri) throws SystemException;

    Map<Integer, String> findProcesses(String nameFragment);
}
