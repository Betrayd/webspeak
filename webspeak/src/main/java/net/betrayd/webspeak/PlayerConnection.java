package net.betrayd.webspeak;

import com.google.gson.JsonObject;
import net.betrayd.webspeak.event.WebSpeakEvent;

import java.util.function.Consumer;

public interface PlayerConnection {
    /**
     * Get the webspeak player that this connection belongs to.
     */
    WebSpeakPlayer getPlayer();

    /**
     * Send a message to the client.
     * @param message Raw JSON message.
     */
    void sendMessage(JsonObject message);

    /**
     * Register a listener for when any message is received from the client.
     * @param listener Message receive listener.
     */
    void onReceiveMessage(Consumer<JsonObject> listener);

    /**
     * Check if the client is still connected.
     */
    boolean isConnected();

    /**
     * Register a listener for when the client has disconnected from the server.
     * @param listener Client disconnect listener.
     */
    void onDisconnected(Runnable listener);
}
