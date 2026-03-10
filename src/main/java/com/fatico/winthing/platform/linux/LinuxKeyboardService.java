package com.fatico.winthing.platform.linux;

import com.fatico.winthing.platform.ShellExecutor;
import com.fatico.winthing.systems.keyboard.KeyboardService;
import com.fatico.winthing.windows.input.KeyboardKey;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux implementation of keyboard simulation using xdotool.
 *
 * <p>Requires xdotool to be installed.
 *
 * @since 2.0.0
 */
public class LinuxKeyboardService implements KeyboardService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Map<KeyboardKey, String> KEY_MAP = new HashMap<>();

    static {
        // Map Windows virtual key names to X11 key names used by xdotool
        KEY_MAP.put(KeyboardKey.RETURN, "Return");
        KEY_MAP.put(KeyboardKey.ESCAPE, "Escape");
        KEY_MAP.put(KeyboardKey.TAB, "Tab");
        KEY_MAP.put(KeyboardKey.SPACE, "space");
        KEY_MAP.put(KeyboardKey.BACK, "BackSpace");
        KEY_MAP.put(KeyboardKey.DELETE, "Delete");
        KEY_MAP.put(KeyboardKey.INSERT, "Insert");
        KEY_MAP.put(KeyboardKey.HOME, "Home");
        KEY_MAP.put(KeyboardKey.END, "End");
        KEY_MAP.put(KeyboardKey.PRIOR, "Prior");
        KEY_MAP.put(KeyboardKey.NEXT, "Next");
        KEY_MAP.put(KeyboardKey.UP, "Up");
        KEY_MAP.put(KeyboardKey.DOWN, "Down");
        KEY_MAP.put(KeyboardKey.LEFT, "Left");
        KEY_MAP.put(KeyboardKey.RIGHT, "Right");
        KEY_MAP.put(KeyboardKey.SHIFT, "Shift_L");
        KEY_MAP.put(KeyboardKey.CONTROL, "Control_L");
        KEY_MAP.put(KeyboardKey.MENU, "Alt_L");
        KEY_MAP.put(KeyboardKey.LWIN, "Super_L");
        KEY_MAP.put(KeyboardKey.RWIN, "Super_R");
        KEY_MAP.put(KeyboardKey.CAPITAL, "Caps_Lock");
        KEY_MAP.put(KeyboardKey.NUMLOCK, "Num_Lock");
        KEY_MAP.put(KeyboardKey.SCROLL, "Scroll_Lock");
        KEY_MAP.put(KeyboardKey.F1, "F1");
        KEY_MAP.put(KeyboardKey.F2, "F2");
        KEY_MAP.put(KeyboardKey.F3, "F3");
        KEY_MAP.put(KeyboardKey.F4, "F4");
        KEY_MAP.put(KeyboardKey.F5, "F5");
        KEY_MAP.put(KeyboardKey.F6, "F6");
        KEY_MAP.put(KeyboardKey.F7, "F7");
        KEY_MAP.put(KeyboardKey.F8, "F8");
        KEY_MAP.put(KeyboardKey.F9, "F9");
        KEY_MAP.put(KeyboardKey.F10, "F10");
        KEY_MAP.put(KeyboardKey.F11, "F11");
        KEY_MAP.put(KeyboardKey.F12, "F12");
        KEY_MAP.put(KeyboardKey.VOLUME_MUTE, "XF86AudioMute");
        KEY_MAP.put(KeyboardKey.VOLUME_DOWN, "XF86AudioLowerVolume");
        KEY_MAP.put(KeyboardKey.VOLUME_UP, "XF86AudioRaiseVolume");
        KEY_MAP.put(KeyboardKey.MEDIA_NEXT_TRACK, "XF86AudioNext");
        KEY_MAP.put(KeyboardKey.MEDIA_PREV_TRACK, "XF86AudioPrev");
        KEY_MAP.put(KeyboardKey.MEDIA_STOP, "XF86AudioStop");
        KEY_MAP.put(KeyboardKey.MEDIA_PLAY_PAUSE, "XF86AudioPlay");
        KEY_MAP.put(KeyboardKey.BROWSER_BACK, "XF86Back");
        KEY_MAP.put(KeyboardKey.BROWSER_FORWARD, "XF86Forward");
        KEY_MAP.put(KeyboardKey.BROWSER_REFRESH, "XF86Reload");
        KEY_MAP.put(KeyboardKey.BROWSER_HOME, "XF86HomePage");
    }

    @Inject
    public LinuxKeyboardService() {
        // No special initialization needed
    }

    @Override
    public void pressKeys(final List<KeyboardKey> keys) {
        if (keys.isEmpty()) {
            return;
        }

        StringBuilder keyCombo = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                keyCombo.append('+');
            }
            keyCombo.append(mapKey(keys.get(i)));
        }

        try {
            ShellExecutor.execute("xdotool", "key", keyCombo.toString());
        } catch (Exception ex) {
            logger.warn("Could not simulate key press. Install xdotool for keyboard support.");
        }
    }

    private String mapKey(final KeyboardKey key) {
        String mapped = KEY_MAP.get(key);
        if (mapped != null) {
            return mapped;
        }
        // For letter keys (A-Z), xdotool uses lowercase
        String name = key.name();
        if (name.length() == 1 && Character.isLetter(name.charAt(0))) {
            return name.toLowerCase();
        }
        // For number keys
        if (name.startsWith("NUM") && name.length() == 4) {
            return name.substring(3);
        }
        return name.toLowerCase();
    }
}
