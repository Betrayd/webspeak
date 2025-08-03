package net.betrayd.webspeak.v1.impl.net;

import net.betrayd.webspeak.v1.PlayerConnection;
import net.betrayd.webspeak.v1.WebSpeakPlayer;

/**
 * Utility functions regarding Websocket networking
 */
public class WebSpeakNet {

    /**
     * Thrown when trying to parse a packet with an invalid ID.
     */
    public static class UnknownPacketException extends IllegalArgumentException {
        private final String packetId;

        public UnknownPacketException(String packetId) {
            super("Unknown packet ID: " + packetId);
            this.packetId = packetId;
        }

        public String getPacketId() {
            return packetId;
        }
    }

    public static <T> String writePacket(S2CPacket<T> packet, T val) {
        return packet.getId() + ";" + packet.write(val);
    }

    public static <T> void sendPacket(Iterable<? extends PlayerConnection> players, S2CPacket<T> packet, T val) {
        String text = writePacket(packet, val);
        for (var connection : players) {
            connection.sendText(text);
        }
    }

    public static <T> void sendPacketTo(Iterable<? extends WebSpeakPlayer> players, S2CPacket<T> packet, T val) {
        String text = writePacket(packet, val);
        for (var player : players) {
            PlayerConnection connection = player.getConnection();
            if (connection != null) {
                connection.sendText(text);
            }
        }
    }

    public static void applyPacket(WebSpeakPlayer player, String data) {
        int colon = data.indexOf(';');
        if (colon < 0) {
            throw new IllegalArgumentException("No semicolon (;) was found to indicate packet type");
        }
        String id = data.substring(0, colon);
        String payload = data.substring(colon + 1);

        C2SPacket<?> packet = C2SPackets.get(id);
        if (packet == null) {
            throw new UnknownPacketException(id);
        }

        applyPacket(player, packet, payload);
    }

    // Aren't generics fun??
    private static <T> void applyPacket(WebSpeakPlayer player, C2SPacket<T> packet, String payload) {
        T val = packet.read(payload);
        packet.apply(player, val);
    }
}
