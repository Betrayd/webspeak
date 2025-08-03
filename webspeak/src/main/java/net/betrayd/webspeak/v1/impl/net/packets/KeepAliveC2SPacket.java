package net.betrayd.webspeak.v1.impl.net.packets;

import java.util.Date;

import net.betrayd.webspeak.v1.WebSpeakFlags;
import net.betrayd.webspeak.v1.WebSpeakServer;
import net.betrayd.webspeak.v1.impl.net.C2SPacket;

public record KeepAliveC2SPacket(long timestamp) {
    public static final C2SPacket<KeepAliveC2SPacket> PACKET = new C2SPacket.JsonC2SPacket<>(KeepAliveC2SPacket.class,
            (player, val) -> {
                if (player.getServer().getFlag(WebSpeakFlags.DEBUG_KEEPALIVE)) {
                    WebSpeakServer.LOGGER.info("{} sent keepalive packet at {}", player.getPlayerId(), new Date(val.timestamp));
                }
            });
}
