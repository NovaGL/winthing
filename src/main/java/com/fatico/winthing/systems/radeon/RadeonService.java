package com.fatico.winthing.systems.radeon;

import com.fatico.winthing.systems.radeon.jna.AtiAdl;
import com.google.common.collect.ComparisonChain;
import com.google.inject.Inject;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.lang.ref.Cleaner;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;

public class RadeonService {

    private static final Cleaner CLEANER = Cleaner.create();
    private static final int MIN_WIDTH = 640;
    private static final int MIN_HEIGHT = 480;
    private static final int MAX_WIDTH = 7680;   // 8K resolution
    private static final int MAX_HEIGHT = 4320;

    private final AtiAdl atiAdl;
    private final Pointer context;

    @Inject
    @SuppressWarnings("this-escape")
    public RadeonService(final AtiAdl atiAdl) {
        this.atiAdl = Objects.requireNonNull(atiAdl);
        {
            final PointerByReference contextReference = new PointerByReference();
            final int result = atiAdl.ADL2_Main_Control_Create(
                new MallocCallback(),
                1,
                contextReference
            );
            if (result != AtiAdl.ADL_OK) {
                throw new AdlException("ADL2_Main_Control_Create", result);
            }
            this.context = contextReference.getValue();
        }
        CLEANER.register(this, new CleanupAction(atiAdl, context));
    }

    private static class CleanupAction implements Runnable {
        private final AtiAdl atiAdl;
        private final Pointer context;

        CleanupAction(final AtiAdl atiAdl, final Pointer context) {
            this.atiAdl = atiAdl;
            this.context = context;
        }

        @Override
        public void run() {
            atiAdl.ADL2_Main_Control_Destroy(context);
        }
    }

    public int getPrimaryAdapterIndex() {
        final IntByReference adapterIndexReference = new IntByReference();
        final int result = atiAdl.ADL2_Adapter_Primary_Get(
            context,
            adapterIndexReference
        );
        if (result != AtiAdl.ADL_OK) {
            throw new AdlException("ADL2_Display_CustomizedModeListNum_Get", result);
        }
        return adapterIndexReference.getValue();
    }

    public void setBestResolution(final int adapterIndex) {
        final AtiAdl.ADLMode mode = getBestMode(adapterIndex);
        setMode(adapterIndex, mode);
    }

    public void setResolution(final int adapterIndex, final int width, final int height) {
        validateResolution(width, height);
        final AtiAdl.ADLMode mode = getBestMode(adapterIndex);
        mode.iXRes = width;
        mode.iYRes = height;
        setMode(adapterIndex, mode);
    }

    private void validateResolution(int width, int height) {
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            throw new AdlException(
                "Width out of valid range (" + MIN_WIDTH + "-" + MAX_WIDTH + "): " + width,
                -1
            );
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            throw new AdlException(
                "Height out of valid range (" + MIN_HEIGHT + "-" + MAX_HEIGHT + "): " + height,
                -1
            );
        }
    }

    private void setMode(final int adapterIndex, final AtiAdl.ADLMode mode) {
        final int result = atiAdl.ADL2_Display_Modes_Set(
            context,
            adapterIndex,
            -1,
            1,
            (AtiAdl.ADLMode[]) mode.toArray(1)
        );
        if (result != AtiAdl.ADL_OK) {
            throw new AdlException("ADL2_Display_Modes_Set", result);
        }
    }

    private AtiAdl.ADLMode getBestMode(final int adapterIndex) {
        final AtiAdl.ADLMode[] modes;
        {
            final IntByReference numberOfModesReference = new IntByReference();
            final PointerByReference pointer = new PointerByReference();
            final int result = atiAdl.ADL2_Display_PossibleMode_Get(
                context,
                adapterIndex,
                numberOfModesReference,
                pointer
            );
            if (result != AtiAdl.ADL_OK) {
                throw new AdlException("ADL2_Display_Modes_Get", result);
            }
            modes = (AtiAdl.ADLMode[]) new AtiAdl.ADLMode(pointer.getValue()).toArray(
                numberOfModesReference.getValue()
            );
        }
        if (modes.length == 0) {
            throw new NoSuchElementException();
        }
        return Collections.max(Arrays.asList(modes), (left, right) -> ComparisonChain.start()
                .compare(left.iColourDepth, right.iColourDepth)
                .compare(left.iXRes, right.iXRes)
                .compare(left.iYRes, right.iYRes)
                .compare(left.fRefreshRate, right.fRefreshRate)
                .result()
        );
    }

    private static class MallocCallback extends Memory implements AtiAdl.ADL_MAIN_MALLOC_CALLBACK {
        @Override
        public Pointer invoke(int size) {
            return new Pointer(malloc(size));
        }
    }
}
