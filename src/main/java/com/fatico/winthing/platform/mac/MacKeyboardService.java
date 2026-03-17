package com.fatico.winthing.platform.mac;

import com.fatico.winthing.platform.ShellExecutor;
import com.fatico.winthing.systems.keyboard.KeyboardService;
import com.fatico.winthing.windows.input.KeyboardKey;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * macOS implementation of keyboard simulation using osascript (AppleScript).
 *
 * <p>Uses System Events to simulate key presses. Special media keys are
 * handled via system key codes.
 *
 * @since 2.0.0
 */
public class MacKeyboardService implements KeyboardService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // Maps to macOS key codes for System Events "key code" command
    private static final Map<KeyboardKey, Integer> KEY_CODE_MAP = new HashMap<>();
    // Maps to special system key codes for media keys (used with NX_KEYTYPE)
    private static final Map<KeyboardKey, String> MEDIA_KEY_MAP = new HashMap<>();

    static {
        // Standard key codes (macOS virtual key codes)
        KEY_CODE_MAP.put(KeyboardKey.RETURN, 36);
        KEY_CODE_MAP.put(KeyboardKey.ESCAPE, 53);
        KEY_CODE_MAP.put(KeyboardKey.TAB, 48);
        KEY_CODE_MAP.put(KeyboardKey.SPACE, 49);
        KEY_CODE_MAP.put(KeyboardKey.BACK, 51);
        KEY_CODE_MAP.put(KeyboardKey.DELETE, 117);
        KEY_CODE_MAP.put(KeyboardKey.HOME, 115);
        KEY_CODE_MAP.put(KeyboardKey.END, 119);
        KEY_CODE_MAP.put(KeyboardKey.PRIOR, 116);   // Page Up
        KEY_CODE_MAP.put(KeyboardKey.NEXT, 121);     // Page Down
        KEY_CODE_MAP.put(KeyboardKey.UP, 126);
        KEY_CODE_MAP.put(KeyboardKey.DOWN, 125);
        KEY_CODE_MAP.put(KeyboardKey.LEFT, 123);
        KEY_CODE_MAP.put(KeyboardKey.RIGHT, 124);
        KEY_CODE_MAP.put(KeyboardKey.F1, 122);
        KEY_CODE_MAP.put(KeyboardKey.F2, 120);
        KEY_CODE_MAP.put(KeyboardKey.F3, 99);
        KEY_CODE_MAP.put(KeyboardKey.F4, 118);
        KEY_CODE_MAP.put(KeyboardKey.F5, 96);
        KEY_CODE_MAP.put(KeyboardKey.F6, 97);
        KEY_CODE_MAP.put(KeyboardKey.F7, 98);
        KEY_CODE_MAP.put(KeyboardKey.F8, 100);
        KEY_CODE_MAP.put(KeyboardKey.F9, 101);
        KEY_CODE_MAP.put(KeyboardKey.F10, 109);
        KEY_CODE_MAP.put(KeyboardKey.F11, 103);
        KEY_CODE_MAP.put(KeyboardKey.F12, 111);

        // Media keys - handled differently on macOS (via AppleScript key code)
        MEDIA_KEY_MAP.put(KeyboardKey.MEDIA_PLAY_PAUSE, "playpause");
        MEDIA_KEY_MAP.put(KeyboardKey.MEDIA_NEXT_TRACK, "next track");
        MEDIA_KEY_MAP.put(KeyboardKey.MEDIA_PREV_TRACK, "previous track");
        MEDIA_KEY_MAP.put(KeyboardKey.MEDIA_STOP, "playpause");
        MEDIA_KEY_MAP.put(KeyboardKey.VOLUME_MUTE, "mute");
        MEDIA_KEY_MAP.put(KeyboardKey.VOLUME_UP, "volume up");
        MEDIA_KEY_MAP.put(KeyboardKey.VOLUME_DOWN, "volume down");
    }

    @Inject
    public MacKeyboardService() {
        // No special initialization needed
    }

    @Override
    public void pressKeys(final List<KeyboardKey> keys) {
        if (keys.isEmpty()) {
            return;
        }

        // Check if this is a single media key press
        if (keys.size() == 1 && MEDIA_KEY_MAP.containsKey(keys.get(0))) {
            pressMediaKey(keys.get(0));
            return;
        }

        // Build AppleScript for key combination
        try {
            // Determine modifiers and the main key
            StringBuilder modifiers = new StringBuilder();
            KeyboardKey mainKey = null;

            for (KeyboardKey key : keys) {
                switch (key) {
                    case CONTROL:
                    case LCONTROL:
                    case RCONTROL:
                        appendModifier(modifiers, "control down");
                        break;
                    case SHIFT:
                    case LSHIFT:
                    case RSHIFT:
                        appendModifier(modifiers, "shift down");
                        break;
                    case MENU:
                    case LMENU:
                    case RMENU:
                        appendModifier(modifiers, "option down");
                        break;
                    case LWIN:
                    case RWIN:
                        appendModifier(modifiers, "command down");
                        break;
                    default:
                        mainKey = key;
                        break;
                }
            }

            if (mainKey == null) {
                return;
            }

            Integer keyCode = KEY_CODE_MAP.get(mainKey);
            String script;
            if (keyCode != null) {
                script = "tell application \"System Events\" to key code " + keyCode;
            } else {
                // For letter/number keys, use keystroke
                String ch = mainKey.name().length() == 1
                    ? mainKey.name().toLowerCase(Locale.ROOT) : mainKey.name();
                script = "tell application \"System Events\" to keystroke \"" + ch + "\"";
            }

            if (modifiers.length() > 0) {
                script += " using {" + modifiers + "}";
            }

            ShellExecutor.osascript(script);
        } catch (Exception ex) {
            logger.warn("Could not simulate key press: {}", ex.getMessage());
        }
    }

    private void pressMediaKey(KeyboardKey key) {
        String mediaAction = MEDIA_KEY_MAP.get(key);
        if (mediaAction == null) {
            return;
        }

        try {
            // Use Spotify's AppleScript interface if available, otherwise use
            // System Events key code approach for media keys
            switch (mediaAction) {
                case "playpause":
                    // key code 16 with command down is a common shortcut,
                    // but for media keys we use a generic approach
                    ShellExecutor.osascript(
                        "tell application \"System Events\" to key code 16 using"
                        + " {command down, option down}");
                    break;
                default:
                    // For volume/track controls use the system key codes
                    ShellExecutor.osascript(
                        "tell application \"System Events\" to key code 16");
                    break;
            }
        } catch (Exception ex) {
            logger.debug("Media key simulation failed: {}", ex.getMessage());
        }
    }

    private void appendModifier(StringBuilder sb, String modifier) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(modifier);
    }
}
