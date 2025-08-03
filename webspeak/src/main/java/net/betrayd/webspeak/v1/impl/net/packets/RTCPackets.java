package net.betrayd.webspeak.v1.impl.net.packets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import net.betrayd.webspeak.v1.PlayerConnection;
import net.betrayd.webspeak.v1.WebSpeakFlags;
import net.betrayd.webspeak.v1.WebSpeakPlayer;
import net.betrayd.webspeak.v1.impl.net.C2SPacket;
import net.betrayd.webspeak.v1.impl.net.C2SPacket.JsonC2SPacket;
import net.betrayd.webspeak.v1.impl.net.S2CPacket;
import net.betrayd.webspeak.v1.impl.net.S2CPacket.JsonS2CPacket;

public class RTCPackets {

    private static final Logger LOGGER = LoggerFactory.getLogger("WebSpeak RTCPackets");

    public static record RTCPacketData(String playerID, JsonElement payload) {};
    public static record RequestOfferS2CPacket(String playerID) {};

    public static final S2CPacket<RequestOfferS2CPacket> REQUEST_OFFER_S2C = new JsonS2CPacket<>("requestOffer");
    public static final S2CPacket<RequestOfferS2CPacket> DISCONNECT_RTC_S2C = new JsonS2CPacket<>("disconnectRTC");
    
    public static final S2CPacket<RTCPacketData> HAND_OFFER_S2C = new JsonS2CPacket<>("handOffer");
    public static final S2CPacket<RTCPacketData> HAND_ANSWER_S2C = new JsonS2CPacket<>("handAnswer");

    public static final S2CPacket<RTCPacketData> HAND_ICE_S2C = new JsonS2CPacket<>("handIce");
    
    public static final C2SPacket<RTCPacketData> RETURN_OFFER_C2S = new JsonC2SPacket<>(
            RTCPacketData.class, RTCPackets::onReturnOffer);

    public static final C2SPacket<RTCPacketData> RETURN_ANSWER_C2S = new JsonC2SPacket<>(
            RTCPacketData.class, RTCPackets::onReturnAnswer);

    public static final C2SPacket<RTCPacketData> RETURN_ICE_C2S = new JsonC2SPacket<>(
            RTCPacketData.class, RTCPackets::onReturnIce);

    private static void onReturnOffer(WebSpeakPlayer player, RTCPacketData data) {
        WebSpeakPlayer targetPlayer = player.getServer().getPlayer(data.playerID);

        if (targetPlayer == null) {
            throw new IllegalArgumentException("Unknown player: " + data.playerID);
        }
        
        PlayerConnection targetConnection = targetPlayer.getConnection();
        if (targetConnection == null) {
            LOGGER.warn("Tried to send rtc offer from {} to disconnected player {}",
                    player.getPlayerId(), targetPlayer.getPlayerId());
            return;
        }

        if (player.getServer().getFlag(WebSpeakFlags.DEBUG_RTC_OFFERS)) {
            LOGGER.info("Sending RTC offer from {} to {}", player.getPlayerId(), targetPlayer.getPlayerId());
        }

        // Hand offer to other client, giving it the source player's ID.
        targetConnection.sendPacket(HAND_OFFER_S2C, new RTCPacketData(player.getPlayerId(), data.payload));
    }

    private static void onReturnAnswer(WebSpeakPlayer player, RTCPacketData data) {
        WebSpeakPlayer targetPlayer = player.getServer().getPlayer(data.playerID);
        
        if (targetPlayer == null) {
            throw new IllegalArgumentException("Unknown player: " + data.playerID);
        }
        
        PlayerConnection targetConnection = targetPlayer.getConnection();
        if (targetConnection == null) {
            LOGGER.warn("Tried to send rtc offer from {} to disconnected player {}",
                    player.getPlayerId(), targetPlayer.getPlayerId());
            return;
        }

        if (player.getServer().getFlag(WebSpeakFlags.DEBUG_RTC_OFFERS)) {
            LOGGER.info("Sending RTC answer from {} to {}", player.getPlayerId(), targetPlayer.getPlayerId());
        }

        targetConnection.sendPacket(HAND_ANSWER_S2C, new RTCPacketData(player.getPlayerId(), data.payload));
    }

    private static void onReturnIce(WebSpeakPlayer player, RTCPacketData data) {
        WebSpeakPlayer targetPlayer = player.getServer().getPlayer(data.playerID);
        
        if (targetPlayer == null) {
            throw new IllegalArgumentException("Unknown player: " + data.playerID);
        }

        PlayerConnection targetConnection = targetPlayer.getConnection();
        if (targetConnection == null) {
            LOGGER.warn("Tried to send ice response from {} to disconnected player {}",
                    player.getPlayerId(), targetPlayer.getPlayerId());
            return;
        }

        if (player.getServer().getFlag(WebSpeakFlags.DEBUG_RTC_OFFERS)) {
            LOGGER.info("Sending ICE responce from {} to {}", player.getPlayerId(), targetPlayer.getPlayerId());
        }

         targetConnection.sendPacket(HAND_ICE_S2C, new RTCPacketData(player.getPlayerId(), data.payload));
    }
}
