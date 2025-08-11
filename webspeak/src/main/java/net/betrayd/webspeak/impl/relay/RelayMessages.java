package net.betrayd.webspeak.impl.relay;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class RelayMessages {
    private RelayMessages() {};
    public interface RelayMessage {
        String getType();
    }

    public record S2RGetSessionId(int requestId) implements RelayMessage {
        public static final String TYPE = "getSessionId";

        @Override
        public String getType() {
            return TYPE;
        }
    }

    public record S2RDisconnectClient(String id, int statusCode, String reason) implements RelayMessage {
        public static final String TYPE = "disconnectClient";

        @Override
        public String getType() {
            return TYPE;
        }
    }

    public record S2RReleaseSessionId(String id, int statusCode, String reason) implements RelayMessage {
        public static final String TYPE = "releaseSessionId";

        @Override
        public String getType() {
            return TYPE;
        }
    }

    public record R2SReturnSessionId(int requestId, String id) implements RelayMessage {
        public static final String TYPE = "returnSessionId";

        @Override
        public String getType() {
            return TYPE;
        }
    }

    public record R2SAddedClient(String id) implements RelayMessage {
        public static final String TYPE = "addedClient";

        @Override
        public String getType() {
            return TYPE;
        }
    }

    public record R2SClosedClient(String id, int statusCode, String reason) implements RelayMessage {
        public static final String TYPE = "closedClient";

        @Override
        public String getType() {
            return TYPE;
        }
    }

    // Gson to write relay messages
    private static final Gson GSON = new Gson();

    public static String write(RelayMessage message) {
        JsonObject obj = GSON.toJsonTree(message).getAsJsonObject();
        obj.addProperty("type", message.getType());
        return GSON.toJson(obj);
    }
}
