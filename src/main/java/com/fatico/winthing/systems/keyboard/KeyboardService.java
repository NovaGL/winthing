package com.fatico.winthing.systems.keyboard;

import com.fatico.winthing.windows.input.KeyboardKey;
import java.util.List;

/**
 * Platform-independent interface for keyboard simulation.
 *
 * <p>Implementations exist for Windows, Linux, and macOS.
 *
 * @since 2.0.0
 */
public interface KeyboardService {

    void pressKeys(List<KeyboardKey> keys);
}
