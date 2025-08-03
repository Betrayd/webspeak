package net.betrayd.webspeak.v1.impl.net;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.betrayd.webspeak.v1.impl.net.packets.KeepAliveC2SPacket;
import net.betrayd.webspeak.v1.impl.net.packets.RTCPackets;
import net.betrayd.webspeak.v1.impl.net.packets.TestC2SPacket;
import net.betrayd.webspeak.v1.impl.relay.packets.RelayC2SPackets;

public class C2SPackets {
    public static final BiMap<String, C2SPacket<?>> REGISTRY = HashBiMap.create();

    static {
        REGISTRY.put("test", TestC2SPacket.PACKET);
        REGISTRY.put("keepAlive", KeepAliveC2SPacket.PACKET);

        REGISTRY.put("returnOffer", RTCPackets.RETURN_OFFER_C2S);
        REGISTRY.put("returnAnswer", RTCPackets.RETURN_ANSWER_C2S);
        REGISTRY.put("returnIce", RTCPackets.RETURN_ICE_C2S);
        REGISTRY.put("relayClientConnect", RelayC2SPackets.RELAY_CLIENT_CONNECTED_C2S_PACKET);
        REGISTRY.put("relayClientDisconnect", RelayC2SPackets.RELAY_CLIENT_DISCONNECTED_C2S_PACKET);
    }
    
    public static C2SPacket<?> get(String id) {
        return REGISTRY.get(id);
    }

    public static String getId(C2SPacket<?> packet) {
        return REGISTRY.inverse().get(packet);
    }
}
