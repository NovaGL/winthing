package com.fatico.winthing.windows.jna;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * JNA structure for the Windows SYSTEM_POWER_STATUS struct.
 * Used by GetSystemPowerStatus to retrieve battery information.
 */
@SuppressWarnings({"checkstyle:membername", "checkstyle:visibilitymodifier"})
public class SystemPowerStatus extends Structure {

    public byte acLineStatus;
    public byte batteryFlag;
    public byte batteryLifePercent;
    public byte systemStatusFlag;
    public int batteryLifeTime;
    public int batteryFullLifeTime;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
            "acLineStatus", "batteryFlag", "batteryLifePercent",
            "systemStatusFlag", "batteryLifeTime", "batteryFullLifeTime"
        );
    }
}
