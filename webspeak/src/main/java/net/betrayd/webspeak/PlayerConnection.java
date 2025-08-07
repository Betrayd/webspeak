package net.betrayd.webspeak;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.function.Consumer;
import java.util.function.Function;

public interface PlayerConnection {


    enum DisconnectReason {
        /**
         * The player was removed from the game server.
         */
        PLAYER_REMOVED,

        /**
         * The WebSpeak server was shut down.
         */
        SERVER_SHUTDOWN,

        /**
         * The user disconnected manually.
         */
        CLIENT_DISCONNECT,

        /**
         * The connection was unexpectedly dropped.
         */
        CONNECTION_LOST,

        /**
         * Unknown disconnection reason
         */
        UNKNOWN
    }

    /**
     * Send a message to the client.
     * @param message Raw JSON message.
     */
    void sendMessage(String message);

    /**
     * Register a listener for when any message is received from the client.
     * @param listener Message receive listener.
     * @implNote May be called on a thread other than the server thread, and outside a tick.
     */
    void onReceiveMessage(Consumer<String> listener);

    /**
     * Check if the client is still connected.
     */
    boolean isConnected();

    /**
     * Disconnect this player connection.
     * @param reason Reason for disconnect.
     */
    void disconnect(DisconnectReason reason);

    /**
     * Register a listener for when the client has disconnected from the server.
     * @param listener Client disconnect listener.
     * @implNote May be called on a thread other than the server thread, and outside a tick.
     */
    void onDisconnected(Consumer<DisconnectReason> listener);
}
