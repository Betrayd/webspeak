package net.betrayd.webspeak;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.function.Consumer;

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
     * Get the webspeak player that this connection belongs to.
     */
    WebSpeakPlayer getPlayer();

    /**
     * Send a message to the client.
     * @param message Raw JSON message.
     */
    void sendMessage(JsonElement message);

    /**
     * Register a listener for when any message is received from the client.
     * @param listener Message receive listener.
     */
    void onReceiveMessage(Consumer<JsonElement> listener);

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
     */
    void onDisconnected(Consumer<DisconnectReason> listener);

}
