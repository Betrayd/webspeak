package net.betrayd.webspeak.v1.impl.net.packets;

import net.betrayd.webspeak.v1.PlayerConnection;
import net.betrayd.webspeak.v1.WebSpeakPlayer;
import net.betrayd.webspeak.v1.impl.net.S2CPacket;
import net.betrayd.webspeak.v1.impl.net.S2CPacket.JsonS2CPacket;
import net.betrayd.webspeak.v1.util.WebSpeakVector;

public record UpdateTransformS2CPacket(String playerID, WebSpeakVector pos, WebSpeakVector forward, WebSpeakVector up) {
    public static final S2CPacket<UpdateTransformS2CPacket> PACKET = new JsonS2CPacket<>("updateTransform");

    public static UpdateTransformS2CPacket fromPlayer(WebSpeakPlayer player) {
        return new UpdateTransformS2CPacket(player.getPlayerId(), player.getLocation(), player.getForward(), player.getUp());
    }

    public void send(PlayerConnection connection) {
        connection.sendPacket(PACKET, this);
    }
}
