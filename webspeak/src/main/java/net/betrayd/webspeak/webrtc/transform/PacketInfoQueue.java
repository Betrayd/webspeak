package net.betrayd.webspeak.webrtc.transform;

import org.jitsi.utils.queue.PacketQueue;

import java.util.concurrent.ExecutorService;

public class PacketInfoQueue extends PacketQueue<PacketInfo> {

    public PacketInfoQueue(ExecutorService executor, PacketHandler<PacketInfo> packetHandler, int capacity) {
        super(capacity, null, "PacketInfoQueue", packetHandler, executor);
    }
}
