package net.betrayd.webspeak.webrtc.dcsctp;

import net.betrayd.webspeak.webrtc.transform.PacketInfo;
import net.betrayd.webspeak.webrtc.transform.node.nodes.ConsumerNode;
import net.betrayd.webspeak.webrtc.utils.TaskPools;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.LinkedBlockingQueue;

public class DcStcpHandler extends ConsumerNode {
    private final Object sctpTransportLock = new Object();
    @Nullable
    private DcSctpTransport sctpTransport = null;
    private final LinkedBlockingQueue<PacketInfo> cachedSctpPackets = new LinkedBlockingQueue<>(100);

    @Override
    protected void Consume(PacketInfo packetInfo) {
        synchronized(sctpTransportLock) {
            if(sctpTransport != null){
                sctpTransport.handleIncomingSctp(packetInfo.getPacket());
            }
        }
    }

    public void setSctpTransport(DcSctpTransport sctpTransport){
        // Submit this to the pool since we wait on the lock and process any
        // cached packets here as well
        TaskPools.IO_POOL.execute(() -> {
            // We grab the lock here so that we can set the SCTP transport and
            // process any previously-cached packets as an atomic operation.
            // It also prevents another thread from coming in via
            // #doProcessPackets and processing packets at the same time in
            // another thread, which would be a problem.
            synchronized(sctpTransportLock) {
                this.sctpTransport = sctpTransport;
                for(PacketInfo packetInfo : cachedSctpPackets){
                    sctpTransport.handleIncomingSctp(packetInfo.getPacket());
                }
                cachedSctpPackets.clear();
            }
        });
    }
}
