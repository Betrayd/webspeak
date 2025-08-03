package net.betrayd.webspeak.v1.impl.net.packets;

import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.v1.WebSpeakPlayer;
import net.betrayd.webspeak.v1.impl.net.C2SPacket;

public class TestC2SPacket implements C2SPacket<String> {

    public static final C2SPacket<String> PACKET = new TestC2SPacket();

    @Override
    public String read(String payload) {
        return payload;
    }

    @Override
    public void apply(WebSpeakPlayer player, String val) {
        LoggerFactory.getLogger(getClass()).info("{} sent {}", player, val);
    }
    
}
