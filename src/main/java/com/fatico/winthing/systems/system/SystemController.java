package com.fatico.winthing.systems.system;

import com.fatico.winthing.common.BaseController;
import com.fatico.winthing.messaging.Message;
import com.fatico.winthing.messaging.QualityOfService;
import com.fatico.winthing.messaging.Registry;
import com.fatico.winthing.systems.keyboard.KeyboardService;
import com.fatico.winthing.windows.SystemException;
import com.fatico.winthing.windows.input.KeyboardKey;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;

public class SystemController extends BaseController {

    private final SystemService systemService;
    private final KeyboardService keyboardService;
    private final SystemCommander systemCommander;

    @Inject
    @SuppressWarnings("this-escape")
    public SystemController(final Registry registry, final SystemService systemService,
            final KeyboardService keyboardService)
            throws SystemException {
        super("system");
        this.systemService = Objects.requireNonNull(systemService);
        this.keyboardService = Objects.requireNonNull(keyboardService);
        registry.queueInitialMessage(
            makeMessage(
                "online",
                new JsonPrimitive(true),
                QualityOfService.AT_LEAST_ONCE,
                true
            )
        );
        registry.setWill(
            makeMessage(
                "online",
                new JsonPrimitive(false),
                QualityOfService.AT_LEAST_ONCE,
                true
            )
        );
        registry.subscribe(prefix + "commands/shutdown", this::shutdown);
        registry.subscribe(prefix + "commands/suspend", this::suspend);
        registry.subscribe(prefix + "commands/hibernate", this::hibernate);
        registry.subscribe(prefix + "commands/reboot", this::reboot);
        registry.subscribe(prefix + "commands/open", this::open);
        registry.subscribe(prefix + "commands/run", this::run);
        registry.subscribe(prefix + "commands/media_play_pause", this::mediaPlayPause);
        registry.subscribe(prefix + "commands/media_next", this::mediaNext);
        registry.subscribe(prefix + "commands/media_prev", this::mediaPrev);
        registry.subscribe(prefix + "commands/media_stop", this::mediaStop);

        systemCommander = new SystemCommander();
        systemCommander.parseConfig();
    }

    public void shutdown(final Message message) {
        systemService.shutdown();
    }

    void reboot(final Message message) {
        systemService.reboot();
    }

    public void suspend(final Message message) {
        systemService.suspend();
    }

    public void hibernate(final Message message) {
        systemService.hibernate();
    }

    public void run(final Message message) {
        String command;
        String parameters;
        String workingDirectory;

        try {
            final JsonArray arguments = message.getPayload().get().getAsJsonArray();
            command = arguments.get(0).getAsString();
            parameters = arguments.size() > 1 ? arguments.get(1).getAsString() : "";
            workingDirectory = arguments.size() > 2 ? arguments.get(2).getAsString() : null;
        } catch (final NoSuchElementException | IllegalStateException exception) {
            throw new IllegalArgumentException("Invalid arguments.");
        }

        if (systemCommander.isEnabled()) {
            String commander = systemCommander.getCommand(command);
            if (commander == null) {
                throw new SystemException("Invalid command.");
            }
            command = commander;
        }

        systemService.run(command, parameters, workingDirectory);
    }

    public void open(final Message message) {
        final String uri;
        try {
            uri = message.getPayload().get().getAsString();
        } catch (final NoSuchElementException | IllegalStateException exception) {
            throw new IllegalArgumentException("Invalid arguments.");
        }
        systemService.open(uri);
    }

    /**
     * Toggles media play/pause.
     * This is a command-only handler; it is never auto-triggered on MQTT connect.
     */
    public void mediaPlayPause(final Message message) {
        keyboardService.pressKeys(
            Collections.singletonList(KeyboardKey.MEDIA_PLAY_PAUSE)
        );
    }

    /**
     * Skips to the next media track.
     * This is a command-only handler; it is never auto-triggered on MQTT connect.
     */
    public void mediaNext(final Message message) {
        keyboardService.pressKeys(
            Collections.singletonList(KeyboardKey.MEDIA_NEXT_TRACK)
        );
    }

    /**
     * Goes back to the previous media track.
     * This is a command-only handler; it is never auto-triggered on MQTT connect.
     */
    public void mediaPrev(final Message message) {
        keyboardService.pressKeys(
            Collections.singletonList(KeyboardKey.MEDIA_PREV_TRACK)
        );
    }

    /**
     * Stops media playback.
     * This is a command-only handler; it is never auto-triggered on MQTT connect.
     */
    public void mediaStop(final Message message) {
        keyboardService.pressKeys(
            Collections.singletonList(KeyboardKey.MEDIA_STOP)
        );
    }

}
