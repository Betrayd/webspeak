package net.betrayd.webspeak.impl.relay.RelayBackend;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.betrayd.webspeak.impl.msg.C2SMessageType;
import net.betrayd.webspeak.impl.msg.S2CMessage;
import net.betrayd.webspeak.impl.net.WSMultiConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message handlers involving a relay rather than a client.
 */
public final class RelayMessages {
    public static final Gson GSON = new GsonBuilder().create();


    public static String writeMessage(S2CMessage message) {
        var p = message.toJson(GSON);
        p.addProperty("type", message.getMsgType());
        return GSON.toJson(p);
    }

    public record AddClientMessage(String sessionId) {}

    public record CloseClientMessage(String sessionId, int statusCode, String reason) implements S2CMessage {

        @Override
        public String getMsgType() {
            return "closeClient";
        }
    }

    public record DisconnectClientMessage(String sessionId) implements S2CMessage {

        @Override
        public String getMsgType() {
            return "disconnectClient";
        }
    }

//    public record C2SMessage(String type, JsonObject payload) {};
//
//    public static C2SMessage readMessage(String msg) {
//        JsonObject p = GSON.fromJson(msg, JsonObject.class);
//        String type = p.get("type").getAsString();
//        return new C2SMessage(type, p);
//    }

}
