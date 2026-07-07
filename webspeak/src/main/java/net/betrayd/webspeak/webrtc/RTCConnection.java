package net.betrayd.webspeak.webrtc;

import net.betrayd.webspeak.webrtc.datachannel.DataChannelStack;
import net.betrayd.webspeak.webrtc.datachannel.protocol.DataChannelPacket;
import net.betrayd.webspeak.webrtc.dcsctp.DcSctpTransport;
import net.betrayd.webspeak.webrtc.dtls.DtlsServer;
import net.betrayd.webspeak.webrtc.dtls.DtlsTransport;
import net.betrayd.webspeak.webrtc.ice.IceCandidateParser;
import net.betrayd.webspeak.webrtc.ice.IceStartData;
import net.betrayd.webspeak.webrtc.ice.IceTransport;
import net.betrayd.webspeak.webrtc.signaling.RTCSdpType;
import net.betrayd.webspeak.webrtc.signaling.RTCSignalingMessages;
import net.betrayd.webspeak.webrtc.signaling.SignalingServer;
import net.betrayd.webspeak.webrtc.tracks.DataChannelTrack;
import net.betrayd.webspeak.webrtc.tracks.RTCTrack;
import net.betrayd.webspeak.webrtc.transform.PacketInfoQueue;
import net.betrayd.webspeak.webrtc.transform.node.nodes.Node;
import net.betrayd.webspeak.webrtc.transform.pipeline.PipelineBuilder;
import net.betrayd.webspeak.webrtc.utils.RawPacketUtils;
import net.betrayd.webspeak.webrtc.utils.TaskPools;
import org.jetbrains.annotations.Nullable;
import org.jitsi.dcsctp4j.DcSctpMessage;
import org.jitsi.dcsctp4j.SendStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Doesn't currently compile as the pipeline is being changed
 */
public class RTCConnection {
    public static final Logger LOGGER = LoggerFactory.getLogger(RTCConnection.class);
    public static final int QUEUE_SIZE = 1024;

    private final IceTransport iceTransport;
    private final DtlsTransport dtlsTransport;

    private final Map<String, RTCTrack> tracks = new ConcurrentHashMap<>();
    // Fast lookup map for routing incoming audio packets by SSRC
    //private final Map<Long, AudioTrack> audioTracksBySsrc = new ConcurrentHashMap<>();
    int sdpVersion = 1;

    private final long sdpSessionId = new SecureRandom().nextLong() & 0x7FFFFFFFFFL;

    public void DcSctpHandler() =
    /** The [DcSctpTransport] instance we'll use to manage the SCTP connection */
    @Nullable
    private DcSctpTransport sctpTransport = null;
    private final DataChannelStack dataChannelStack = new DataChannelStack((data, sid, ppid) -> {
        DcSctpMessage message = new DcSctpMessage((short) sid, ppid, data.array());

        if(sctpTransport != null){
            SendStatus stauts = sctpTransport.send(message, DcSctpTransport.getDefaultSendOptions());

            if(stauts == SendStatus.kSuccess){
                return 0;
            }else{
                LOGGER.error("Error sending to SCTP: {}", stauts);
            }
        }
        return -1;
    });

    private final PacketInfoQueue incomingDataChannelMessageQueue = new PacketInfoQueue(TaskPools.IO_POOL, packetInfo -> {
        //logic guarentees that type safety. If not something in the logic was horribly wrong and so we want to runtime error and crash anyway
        DataChannelPacket dataChannelPacket = (DataChannelPacket) packetInfo.getPacket();

        dataChannelStack.onIncomingDataChannelPacket(ByteBuffer.wrap(dataChannelPacket.getData()), dataChannelPacket.getSid(), dataChannelPacket.getPpid());

        return true;
    }, QUEUE_SIZE);

    public Node sctpPipeline = PipelineBuilder.pipeline(builder -> {
        if (sctpHandler != null) {
            builder.node(sctpHandler);
        }
    });

    public RTCConnection(IceTransport iceTransport, DtlsTransport dtlsTransport) {
        this.iceTransport = iceTransport;
        this.dtlsTransport = dtlsTransport;

        iceTransport.oneIceReady().addListener(v -> onIceReady());

        iceTransport.onRawPacketReceived().addListener(this::handleRawPacket);

        dtlsTransport.onDtlsHandshakeComplete().addListener(this::onDtlsHandshakeComplete);

        dtlsTransport.onDtlsAppDataRecieved().addListener(this::dtlsAppPacketReceived);

        dtlsTransport.setOutgoingDataHandler(buffer -> {
            try {
                iceTransport.send(buffer.getData(), buffer.getOffset(), buffer.getLength());
            } catch (IOException e) {
                LOGGER.warn("Error sending DTLS data through ICE", e);
            }
        });

        //add a default track since webRTC doesn't work without one
        DataChannelTrack defaultTrack = new DataChannelTrack("default", "Default");
        addTrack(defaultTrack);
    }

    public void receivedSessionDescription(RTCSignalingMessages.sessionDescription sessionDescription) {
        LOGGER.debug("Received remote SDP type={}", sessionDescription.getSdpType());
        if(sessionDescription.getSdpType() != RTCSdpType.ANSWER){
            //Non answers are currently unhandled...
            LOGGER.warn("Received SDP answer type which is currently unsupported");
            return;
        }

        String sdp = sessionDescription.sdp();

        // Extract the setup role from the remote SDP so the DTLS stack knows how to act
        if (sdp.contains("a=setup:active")) {
            dtlsTransport.setSetupAttribute("active");
        } else if (sdp.contains("a=setup:passive")) {
            dtlsTransport.setSetupAttribute("passive");
        } else if (sdp.contains("a=setup:actpass")) {
            LOGGER.warn("Remote SDP answer used setup:actpass; defaulting local DTLS role to active/client");
            dtlsTransport.setSetupAttribute("active");
        } else{
            LOGGER.error("Received bad SDP type for a=setup: {}", sdp);
            return;
        }

        //TODO: clean this up to also use a string reader
        Map<String, java.util.List<String>> remoteFingerprints = new java.util.HashMap<>();
        for (String line : sdp.split("\r?\n")) {
            if (line.startsWith("a=fingerprint:")) {
                // Splits "sha-256 97:1D:89..." into ["sha-256", "97:1D:89..."]
                String[] parts = line.substring(14).trim().split("\\s+");
                if (parts.length == 2) {
                    String hashFunction = parts[0].toLowerCase();
                    String hashValue = parts[1];
                    remoteFingerprints.computeIfAbsent(hashFunction, k -> new java.util.ArrayList<>()).add(hashValue);
                }
            }
        }
        if (!remoteFingerprints.isEmpty()) {
            dtlsTransport.setRemoteFingerprints(remoteFingerprints);
            LOGGER.info("Remote SDP fingerprints parsed: algorithms={}", remoteFingerprints.keySet());
            LOGGER.debug("Remote SDP fingerprints={}", remoteFingerprints);
        } else {
            LOGGER.warn("No fingerprint found in remote SDP! DTLS will likely fail.");
        }

        IceStartData data = null;
        try {
            data = IceStartData.fromSdp(sessionDescription.sdp());
        }
        catch (IllegalArgumentException e) {
            LOGGER.warn("failed to parse SDP: {}.\nsdp: {}", e, sessionDescription.sdp());
        }

        if(data != null){
            iceTransport.startRemote(data);
        }
    }

    //We don't use mid or MlineIndex because we only have the one channel / ice media stream
    public void receivedIceCandidate(RTCSignalingMessages.iceCandidate iceCandidate) {
        IceCandidateParser.ParsedCandidateSDP data = null;
        try {
            data = IceCandidateParser.parse(iceCandidate.sdp());
            LOGGER.debug(
                    "Parsed remote ICE candidate: foundation={}, type={}, ip={}, port={}, priority={}",
                    data.foundation(),
                    data.type(),
                    data.ip(),
                    data.port(),
                    data.priority()
            );
        }
        catch (IllegalArgumentException e) {
            LOGGER.warn("failed to parse ice candidate: {}.\nsdp: {}", e, iceCandidate.sdp());
        }

        if(data != null){
            iceTransport.addIceCandidate(data);
        }
    }

    public void addTrack(RTCTrack track) {
        tracks.put(track.getId(), track);

        /*if (track instanceof AudioTrack) {
            audioTracksBySsrc.put(((AudioTrack) track).getSsrc(), (AudioTrack) track);
        }*/

        // If you add a track AFTER the connection is established, you must trigger renegotiation
        // triggerRenegotiation();
    }

    public RTCTrack getTrack(String id) {
        return tracks.get(id);
    }

    public Collection<RTCTrack> getTracks() {
        return tracks.values();
    }

    public void removeTrack(String id) {
        RTCTrack removed = tracks.remove(id);
        //don't worry about audio right now
        /*if (removed instanceof AudioTrack) {
            audioTracksBySsrc.remove(((AudioTrack) removed).getSsrc());
        }*/
    }

    public void initiateSfuOffer(SignalingServer signalingServer) {
        String localUfrag = iceTransport.getLocalUfrag();
        String localPwd = iceTransport.getLocalPassword();
        String localCandidates = iceTransport.getLocalCandidatesAsSdp();

        String generatedSdpOffer = buildLocalSdpOffer(localUfrag, localPwd, localCandidates);

        RTCSignalingMessages.sessionDescription offerPacket =
                new RTCSignalingMessages.sessionDescription(0, generatedSdpOffer, "sfu-stream");

        signalingServer.sendSessionDescription(offerPacket);

        sdpVersion++;
    }

    public void close() {
        dtlsTransport.stop();
        iceTransport.stop();
    }

    private void onDtlsHandshakeComplete(DtlsServer.HandshakeCompleteData data) {
        LOGGER.info("DTLS handshake complete");


    }

    private void dtlsAppPacketReceived(Buffer buffer){
        sctpPipeLine()
        dataChannelPacket = new DataChannelPacket(message);
    }

    private void onIceReady(){
        LOGGER.info("ICE connected");

        //TODO: add a test to not call multiple times if ice hits ready state repeatedly
        TaskPools.IO_POOL.execute(dtlsTransport::startDtlsHandshake);
    }

    private void handleRawPacket(Buffer buffer){
        if (buffer.getLength() > 0) {
            int firstByte = buffer.getData()[buffer.getOffset()] & 0xFF;

            if(RawPacketUtils.isDTLSPacket(buffer)){
                byte[] copiedData = new byte[buffer.getLength()];
                System.arraycopy(buffer.getData(), buffer.getOffset(), copiedData, 0, buffer.getLength());
                Buffer clonedBuffer = new Buffer(copiedData, 0, buffer.getLength());

                dtlsTransport.enqueueBuffer(clonedBuffer);
            }
            else if (RawPacketUtils.isRtpRtcp(buffer)) {
                LOGGER.error("RECEIVED RTP NOT IMPLEMENTED YET");
            }
        }


    }

    // Add this helper to construct the SDP string using the variables
    private String buildLocalSdpOffer(String ufrag, String password, String candidatesSdp) {
        StringBuilder sdp = new StringBuilder();

        sdp.append("v=0\r\n");
        sdp.append("o=- ").append(sdpSessionId).append(" ").append(sdpVersion).append(" IN IP4 0.0.0.0\r\n");
        sdp.append("s=-\r\n");
        sdp.append("t=0 0\r\n");
        sdp.append("a=rtcp-mux\r\n");

        int midCounter = 0;
        boolean hasDataChannel = false;

        for (RTCTrack track : getTracks()) {
            if (track instanceof DataChannelTrack) {
                hasDataChannel = true;
            }
        }

        if (hasDataChannel) {
            sdp.append("m=application 9 UDP/DTLS/SCTP webrtc-datachannel\r\n");
            sdp.append("c=IN IP4 0.0.0.0\r\n");
            sdp.append("a=mid:").append(midCounter).append("\r\n");
            sdp.append("a=sctp-port:5000\r\n");

            // DtlsTransport dynamically injects "a=setup:" and "a=fingerprint:" lines
            dtlsTransport.describe(sdp);

            sdp.append("a=ice-ufrag:").append(ufrag).append("\r\n");
            sdp.append("a=ice-pwd:").append(password).append("\r\n");

            midCounter++;
        } else {
            LOGGER.error("Unreachable state: No data channel found on establish RTC connection");
        }

        sdp.append(candidatesSdp);

        return sdp.toString();
    }
}
