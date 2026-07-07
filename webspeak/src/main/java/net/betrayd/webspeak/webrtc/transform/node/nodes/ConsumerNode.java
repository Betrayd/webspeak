package net.betrayd.webspeak.webrtc.transform.node.nodes;

import net.betrayd.webspeak.webrtc.transform.PacketInfo;

public abstract class ConsumerNode extends Node {
    protected abstract void Consume(PacketInfo packetInfo);

    @Override
    public void processPacket(PacketInfo packetInfo){
        super.processPacket(packetInfo);
        Consume(packetInfo);
    }

    @Override
    public Node attach(Node node){
        throw new UnsupportedOperationException("ConsumerNode must be a terminal and should not have child nodes attached.");
    }
}
