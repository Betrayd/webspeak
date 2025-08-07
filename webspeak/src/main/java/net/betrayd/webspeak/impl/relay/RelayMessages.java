package net.betrayd.webspeak.impl.relay;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class RelayMessages {
    private RelayMessages() {};

    public record S2RGetSessionId(int requestId) {
        public static final String TYPE = "getSessionId";
    }

    public record S2RDisconnectClient(String id, int statusCode, String reason) {
        public static final String TYPE = "disconnectClient";
    }

    public record S2RReleaseSessionId(String id, int statusCode, String reason) {
        public static final String TYPE = "releaseSessionId";
    }

    public record R2SReturnSessionId(int requestId, String id) {
        public static final String TYPE = "returnSessionId";
    }

    public record R2SAddedClient(String id) {
        public static final String TYPE = "addedClient";
    }

    public record R2SClosedClient(String id, int statusCode, String reason) {
        public static final String TYPE = "closedClient";
    }

    private static final Gson GSON = new Gson();

    public static String serializeRelayMessage(String type, Object message) {
        JsonObject obj = GSON.toJsonTree(message).getAsJsonObject();
        obj.addProperty("type", type);
        return GSON.toJson(obj);
    }
}
