package net.betrayd.webspeak.v1;

import org.eclipse.jetty.websocket.api.StatusCode;

import net.betrayd.webspeak.v1.impl.net.S2CPacket;
import net.betrayd.webspeak.v1.impl.net.WebSpeakNet;

public interface PlayerConnection {

    public WebSpeakServer getServer();

    public WebSpeakPlayer getPlayer();

    public boolean isConnected();


    public void sendText(String message);

    default <T> void sendPacket(S2CPacket<T> packet, T val) {
        sendText(WebSpeakNet.writePacket(packet, val));
    }

    public void disconnect(int statusCode, String reason);
    
    default void disconnect(String reason) {
        this.disconnect(StatusCode.NORMAL, reason);
    }

    public String getRemoteAddress();
}
