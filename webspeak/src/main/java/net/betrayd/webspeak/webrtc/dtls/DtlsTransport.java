//Large portions of this class were translated from jitsi-videoBridge
package net.betrayd.webspeak.webrtc.dtls;

import net.betrayd.webspeak.webrtc.ice.IceTransport;
import net.betrayd.webspeak.webrtc.utils.TaskPools;
import org.jitsi.utils.queue.PacketQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DtlsTransport {
    public static final Logger LOGGER = LoggerFactory.getLogger(DtlsTransport.class);
    /**
     * Used to Forward data from this DTLS transport.
     * Jitsi instead uses an interface class with a send method.
     */
    private final IceTransport iceTransport;

    private final PacketQueue packetQueue = new PacketQueue(128, null, "packet-queue", new PacketQueue.PacketHandler() {
        @Override
        public boolean handlePacket(Object o) {
            try {

                return true;
            } catch (Exception e) {
                LOGGER.warn("Failed to handle DTLS data", e);
                return false;
            }
        }
    }, TaskPools.IO_POOL);/*
    Missing a releasePacket method here.
    */

    private boolean dtlsHandshakeComplete = false;

    private DtlsStack dtlsStack = new DtlsStack();

    private DtlsTransport(IceTransport iceTransport) {
        this.iceTransport = iceTransport;
    }


}
