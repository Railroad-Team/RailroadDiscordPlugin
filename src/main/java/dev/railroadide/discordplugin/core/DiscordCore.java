package dev.railroadide.discordplugin.core;

import dev.railroadide.discordplugin.DiscordPlugin;
import dev.railroadide.discordplugin.activity.DiscordActivityManager;
import dev.railroadide.discordplugin.data.*;
import dev.railroadide.discordplugin.event.DiscordCommand;
import dev.railroadide.discordplugin.event.DiscordEventHandler;
import dev.railroadide.discordplugin.event.DiscordEvents;
import dev.railroadide.discordplugin.impl.UnixDiscordIPCChannel;
import dev.railroadide.discordplugin.impl.WindowsDiscordIPCChannel;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class DiscordCore implements AutoCloseable {
    public static final Consumer<DiscordResult> DEFAULT_CALLBACK = result -> {
        if (result != DiscordResult.OK)
            throw new DiscordException(result);
    };

    private final Queue<CommandWithCallback> commandQueue = new ArrayDeque<>();
    private final DiscordIPCChannel ipcChannel;
    private String clientId;
    @Getter
    private final DiscordActivityManager activityManager;
    private final Map<String, Consumer<DiscordCommand>> handlers = new ConcurrentHashMap<>();
    private final DiscordEvents events;

    private long nonce;
    private DiscordConnectionState connectionState;
    @Setter
    @Getter
    private DiscordUser currentUser;
    @Setter
    @Getter
    private long pid = ProcessHandle.current().pid();
    private boolean isShuttingDown = false;

    public DiscordCore(String clientId) throws DiscordException {
        this.clientId = clientId;

        this.connectionState = DiscordConnectionState.HANDSHAKE;
        this.nonce = 0L;
        this.events = new DiscordEvents(this);

        try {
            this.ipcChannel = findIPCChannel();
            this.ipcChannel.configureBlocking(false);
        } catch (IOException exception) {
            // Railroad.showErrorAlert("Failed to connect to Discord", "Failed to connect to Discord IPC channel", null);
            throw new RuntimeException("Failed to connect to Discord IPC channel", exception);
        }

        this.activityManager = new DiscordActivityManager(this);
    }

    public static DiscordIPCChannel findIPCChannel() throws IOException {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows") ?
                new WindowsDiscordIPCChannel() : new UnixDiscordIPCChannel();
    }

    public void start() throws RuntimeException {
        if (this.isShuttingDown)
            throw new RuntimeException("Discord IPC is shutting down");

        try {
            sendHandshake();
            runCallbacks();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to send handshake to Discord IPC channel", exception);
        }
    }

    private void runCallbacks() {
        var thread = new Thread(() -> {
            while (true) {
                if (this.isShuttingDown)
                    break;

                try {
                    var response = receiveString();
                    if (response == null)
                        continue;

                    DiscordCommand command = DiscordPlugin.GSON.fromJson(response.payload(), DiscordCommand.class);
                    if (command == null)
                        continue;

                    handleCommand(command);
                    Thread.sleep(100);
                } catch (ClosedChannelException exception) {
                    break;
                } catch (IOException | InterruptedException exception) {
                    throw new RuntimeException("Failed to receive command from Discord IPC channel", exception);
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private void handleCommand(DiscordCommand command) {
        if (command.isError()) {
            DiscordPlugin.getLogger().error("Received error from Discord IPC channel: {}", command);
            return;
        }

        if (command.getNonce() != null) {
            this.handlers.remove(command.getNonce()).accept(command);
        } else if (command.getEvt() != null) {
            DiscordEventHandler<?> handler = this.events.getHandler(command.getEvt());
            Object data = DiscordPlugin.GSON.fromJson(command.getData(), handler.getDataClass());
            handler.handleObject(command, data);
        }
    }

    private DiscordResponse receiveString() throws IOException {
        var header = ByteBuffer.allocate(Integer.BYTES * 2);
        this.ipcChannel.read(header);
        header.flip();
        header.order(ByteOrder.LITTLE_ENDIAN);
        if (header.remaining() == 0)
            return null;

        int status = header.getInt();
        int length = header.getInt();

        var data = ByteBuffer.allocate(length);
        int read = 0;
        do {
            read += (int) this.ipcChannel.read(new ByteBuffer[]{data}, 0, 1);
        } while (read < length);

        var message = new String(data.flip().array(), StandardCharsets.UTF_8);
        var state = DiscordConnectionState.VALUES[status];
        return new DiscordResponse(state, message);
    }

    private void sendHandshake() throws IOException {
        var message = new HandshakeMessage(this.clientId);
        sendString(DiscordPlugin.GSON.toJson(message));
    }

    private void sendBytes(byte[] bytes) throws IOException {
        var buffer = ByteBuffer.allocate((Integer.BYTES * 2) + bytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(this.connectionState.ordinal());
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        this.ipcChannel.write(buffer.flip());
    }

    private void sendString(String string) throws IOException {
        sendBytes(string.getBytes(StandardCharsets.UTF_8));
    }

    public void onReady() {
        this.connectionState = DiscordConnectionState.CONNECTED;
        registerEvents();
        DiscordPlugin.getLogger().info("Discord IPC channel is ready");

        while (!this.commandQueue.isEmpty()) {
            CommandWithCallback commandWithCallback = this.commandQueue.poll();
            sendCommand(commandWithCallback.command(), commandWithCallback.callback());
        }
    }

    private void registerEvents() {
        for (Map.Entry<DiscordCommand.Event, DiscordEventHandler<?>> handler : events.getHandlers()) {
            DiscordCommand.Event event = handler.getKey();
            DiscordEventHandler<?> eventHandler = handler.getValue();
            if (!eventHandler.shouldRegister())
                continue;

            var command = new DiscordCommand();
            command.setCmd(DiscordCommand.Type.SUBSCRIBE);
            command.setEvt(event);
            command.setArgs(DiscordPlugin.GSON.toJsonTree(eventHandler.getRegistrationArgs()));
            command.setNonce(Long.toString(++this.nonce));
            sendCommand(command, response -> DiscordPlugin.getLogger().debug("Registered event {}", event.name()));
        }
    }

    private void sendCommand(DiscordCommand command, Consumer<DiscordCommand> callback) {
        if (this.connectionState == DiscordConnectionState.HANDSHAKE && command.getEvt() != DiscordCommand.Event.READY) {
            this.commandQueue.add(new CommandWithCallback(command, callback));
            return;
        }

        this.handlers.put(command.getNonce(), callback);

        try {
            sendString(DiscordPlugin.GSON.toJson(command));
        } catch (IOException exception) {
            throw new RuntimeException("Failed to send command to Discord IPC channel", exception);
        }
    }

    public DiscordResult checkError(DiscordCommand command) {
        if (command.getEvt() == DiscordCommand.Event.ERROR) {
            var error = DiscordPlugin.GSON.fromJson(command.getData(), DiscordError.class);
            DiscordPlugin.getLogger().error("Received error from Discord IPC channel: {}", error.getMessage());

            return DiscordResult.fromCode(error.getCode());
        }

        return DiscordResult.OK;
    }

    public void sendCommand(DiscordCommand.Type type, Object args, Consumer<DiscordCommand> object) {
        var command = new DiscordCommand();
        command.setCmd(type);
        command.setArgs(DiscordPlugin.GSON.toJsonTree(args).getAsJsonObject());
        command.setNonce(Long.toString(++this.nonce));
        sendCommand(command, object);
    }

    @Override
    public void close() throws RuntimeException {
        try {
            this.ipcChannel.close();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to close Discord IPC channel", exception);
        }

        this.isShuttingDown = true;
    }

    public void setClientId(String id) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("Client ID cannot be null or blank");

        this.clientId = id;

    }

    private record CommandWithCallback(DiscordCommand command, Consumer<DiscordCommand> callback) {}
}
