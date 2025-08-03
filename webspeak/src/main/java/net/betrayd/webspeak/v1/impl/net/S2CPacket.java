package net.betrayd.webspeak.v1.impl.net;

import com.google.gson.Gson;

import net.betrayd.webspeak.v1.PlayerConnection;

public abstract class S2CPacket<T> {
    private final String id;

    public S2CPacket(String id) {
        this.id = id;
    }

    public abstract String write(T val);

    public String getId() {
        return id;
    }

    /**
     * A utility function to send this packet to a player.
     * @param context Websocket context to send to.
     * @param val Packet value.
     */
    @Deprecated
    public void send(PlayerConnection context, T val) {
        context.sendText(WebSpeakNet.writePacket(this, val));
    }

    public static class JsonS2CPacket<T> extends S2CPacket<T> {
        private static final Gson GSON = new Gson();

        public JsonS2CPacket(String id) {
            super(id);
        }

        @Override
        public String write(T val) {
            return GSON.toJson(val);
        }
    }

    public static class StringS2CPacket extends S2CPacket<String> {

        public StringS2CPacket(String id) {
            super(id);
        }

        @Override
        public String write(String val) {
            return val;
        }
        
    }
}
