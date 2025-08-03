package net.betrayd.webspeak.v1.impl.net.packets;

import net.betrayd.webspeak.v1.PlayerConnection;
import net.betrayd.webspeak.v1.impl.net.S2CPacket;
import net.betrayd.webspeak.v1.util.AudioModifier;

public record SetAudioModifierS2CPacket(String playerID, AudioModifier audioModifier) {
    public static final S2CPacket<SetAudioModifierS2CPacket> PACKET = new S2CPacket.JsonS2CPacket<>("setAudioModifier");

    public void send(PlayerConnection connection) {
        connection.sendPacket(PACKET, this);
    }
}
