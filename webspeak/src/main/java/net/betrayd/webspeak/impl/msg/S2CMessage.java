package net.betrayd.webspeak.impl.msg;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * A simple data class that can be sent as a server-to-client message.
 */
public interface S2CMessage {

    /**
     * Convert this message into Json for sending.
     *
     * @param gson Gson instance to use.
     * @return Json object representing this object.
     * @implNote Default implementation uses gson's reflection-based serialization.
     */
    default JsonObject toJson(Gson gson) {
        return gson.toJsonTree(this).getAsJsonObject();
    }

    /**
     * Get the message type tag to apply to this message. Must be unique.
     */
    String getMsgType();
}
