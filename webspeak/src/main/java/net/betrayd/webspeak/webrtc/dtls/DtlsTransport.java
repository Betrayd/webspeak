//Large portions of this class were translated from jitsi-videoBridge
package net.betrayd.webspeak.webrtc.dtls;

import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.webrtc.utils.TaskPools;
import org.ice4j.util.Buffer;
import org.jetbrains.annotations.Nullable;
import org.jitsi.utils.queue.PacketQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DtlsTransport {
    public static final Logger LOGGER = LoggerFactory.getLogger(DtlsTransport.class);

    private final PacketQueue<Buffer> dtlsQueue = new PacketQueue<Buffer>(128, null, "packet-queue", new PacketQueue.PacketHandler() {
        @Override
        public boolean handlePacket(Object o) {
            try {
                if (o instanceof Buffer buffer) {
                    dtlsDataRecieved(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
                    return true;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to handle DTLS data", e);
                return false;
            }
            LOGGER.warn("Packet buffer received data not of type Buffer!");
            return false;
        }
    }, TaskPools.IO_POOL);
    /*
        Missing a releasePacket method here.
    */

    /**
     * This could be made not final but it's never going to be in this impl
     */
    private final DtlsStack dtlsStack;
    private final Event.Invokable<DtlsServer.HandshakeCompleteData> handshakeCompleteEvent = Event.create();
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private boolean dtlsHandshakeComplete = false;

    public DtlsTransport() throws CertificateGenerationException {
        //TODO: this should be in a try catch as it can fail
        dtlsStack = new DtlsStack();
        dtlsStack.onHandshakeComplete().addListener((data) -> {
            dtlsHandshakeComplete = true;
            handshakeCompleteEvent.invoke(data);
        });

    }

    public Event<DtlsServer.HandshakeCompleteData> onDtlsHandshakeComplete(){
        return handshakeCompleteEvent;
    }

    public Event<net.betrayd.webspeak.webrtc.Buffer> onDtlsAppDataRecieved() {
        return dtlsStack.onIncomingProtocolDataRecieved();
    }

    public void setOutgoingDataHandler(Consumer<net.betrayd.webspeak.webrtc.Buffer> outgoingDataHandler){
        dtlsStack.setOutgoingDataHandler(outgoingDataHandler);
    }

    /**
     * Start a DTLS handshake.  The 'role' should have been set before calling this
     * (via [setSetupAttribute]
     */
    public void startDtlsHandshake() {
        if (dtlsStack.getRole() == null) {
            LOGGER.warn("Starting the DTLS stack before it knows its role");
        }
        try {
            dtlsStack.start();
        } catch (IOException | InterruptedException e) {
            // TODO: we're not doing anything here, we should somehow try again.
            LOGGER.error("Error during DTLS negotiation, The server likely has to be restarted", e);
        }
    }

    //TODO: make this not a string so that it's harder to make mistakes
    public void setSetupAttribute(@Nullable String setupAttr) {
        if(setupAttr == null || setupAttr.isEmpty()){
            return;
        }
        switch(setupAttr.toLowerCase()){
            case "active":
                LOGGER.debug("The remote side is acting as DTLS client, we'll act as server");
                dtlsStack.actAsServer();
                break;
            case "passive":
                LOGGER.debug("The remote side is acting as DTLS server, we'll act as client");
                dtlsStack.actAsClient();
                break;
            default:
                LOGGER.error("The remote side sent an unrecognized DTLS setup value: {}", setupAttr);
        }
    }

    public void setRemoteFingerprints(Map<String, List<String>> remoteFingerprints) {
        // Don't pass an empty list to the stack in order to avoid wiping
        // certificates that were contained in a previous request.
        if (remoteFingerprints.isEmpty()) {
            return;
        }

        dtlsStack.setRemoteFingerprints(remoteFingerprints);
    }

    /**
     * Describe the properties of this [DtlsTransport] into the given SDP String
     */
    public void describe(StringBuilder sdp) {
        String setupRole;
        DtlsRole dtlsRole = dtlsStack.getRole();
        if (dtlsRole == null) {
            setupRole = "actpass";
        }else if(dtlsRole instanceof DtlsServer){
            setupRole = "passive";
        }else if(dtlsRole instanceof DtlsClient){
            setupRole = "active";
        }
        else{
            LOGGER.error("bad DTLS role: {}. May have failed to initialize?", dtlsRole);
            throw new IllegalStateException("Cannot describe dtlsRole");
        }

        //add the setup
        sdp.append("a=setup:").append(setupRole).append("\r\n");

        sdp.append("a=fingerprint:")
                .append(dtlsStack.getLocalFingerprintHashFunction())
                .append(" ")
                .append(dtlsStack.getLocalFingerprint())
                .append("\r\n");
    }

    public void enqueueBuffer(net.betrayd.webspeak.webrtc.Buffer buffer) {
        dtlsQueue.add(new Buffer(buffer.data(), buffer.offset(), buffer.length()));
    }

    /**
     * Notify this layer that DTLS data has been received from the network
     */
    public void dtlsDataRecieved(byte[] data, int offset, int length) {
        dtlsStack.processIncomingProtocolData(data, offset, length);
    }

    /**
     * Send out DTLS data
     */
    public void sendDtlsData(byte[] data, int offset, int length) throws IOException {
        dtlsStack.sendApplicationData(data, offset, length);
    }

    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            LOGGER.info("Stopping");
            dtlsStack.close();
        }
    }
}
