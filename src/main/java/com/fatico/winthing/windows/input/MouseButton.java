package com.fatico.winthing.windows.input;

public enum MouseButton {

    LEFT(1),
    RIGHT(2),
    MIDDLE(4),
    X1(5),
    X2(6);

    private final int virtualKeyCode;

    MouseButton(final int virtualKeyCode) {
        assert 0 < virtualKeyCode;
        assert virtualKeyCode < 0xFF;
        this.virtualKeyCode = virtualKeyCode;
    }

    public int getVirtualKeyCode() {
        return virtualKeyCode;
    }

}
