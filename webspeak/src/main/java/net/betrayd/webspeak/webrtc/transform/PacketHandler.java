package net.betrayd.webspeak.webrtc.transform;

public interface PacketHandler {
    /**
     * Process the given packets
     */
    public void processPacket(PacketInfo packetInfo);
}
