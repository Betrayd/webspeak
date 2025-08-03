package net.betrayd.webspeak.v1.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.v1.WebSpeakFlags;
import net.betrayd.webspeak.v1.WebSpeakPlayer;
import net.betrayd.webspeak.v1.WebSpeakServer;
import net.betrayd.webspeak.v1.impl.net.packets.RTCPackets;
import net.betrayd.webspeak.v1.impl.net.packets.RTCPackets.RequestOfferS2CPacket;
import net.betrayd.webspeak.v1.impl.net.packets.UpdateTransformS2CPacket;

public class RTCManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebSpeak RTC Manager");

    private final WebSpeakServer server;

    public RTCManager(WebSpeakServer server) {
        this.server = server;
    }

    public WebSpeakServer getServer() {
        return server;
    }

    
    public void connectRTC(WebSpeakPlayer a, WebSpeakPlayer b) {
        if (getServer().getFlag(WebSpeakFlags.DEBUG_CONNECTION_REQUESTS)) {
            LOGGER.info("Requesting player {} to RTC offer to {}", a.getPlayerId(), b.getPlayerId());
        }
        a.getConnection().sendPacket(RTCPackets.REQUEST_OFFER_S2C, new RequestOfferS2CPacket(b.getPlayerId()));

        // I tried moving this to the player code, but it doesn't seem to work and I don't give a shit.
        UpdateTransformS2CPacket.fromPlayer(a).send(b.getConnection());
        UpdateTransformS2CPacket.fromPlayer(b).send(a.getConnection());
    }

    public void disconnectRTC(WebSpeakPlayer a, WebSpeakPlayer b) {
        if (getServer().getFlag(WebSpeakFlags.DEBUG_CONNECTION_REQUESTS)) {
            LOGGER.info("Requesting player {} to disconnect RTC with {}", a.getPlayerId(), b.getPlayerId());
        }
        a.getConnection().sendPacket(RTCPackets.DISCONNECT_RTC_S2C, new RequestOfferS2CPacket(b.getPlayerId()));
        b.getConnection().sendPacket(RTCPackets.DISCONNECT_RTC_S2C, new RequestOfferS2CPacket(a.getPlayerId()));
    }
}
