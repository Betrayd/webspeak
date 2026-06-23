package net.betrayd.webspeak.webrtc;

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
import net.betrayd.webspeak.webrtc.utils.RawPacketUtils;
import net.betrayd.webspeak.webrtc.utils.TaskPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Doesn't currently compile as the pipeline is being changed
 */
public class RTCConnection {
    public static final Logger LOGGER = LoggerFactory.getLogger(RTCConnection.class);

    private final IceTransport iceTransport;
    private final DtlsTransport dtlsTransport;

    private final Map<String, RTCTrack> tracks = new ConcurrentHashMap<>();
    // Fast lookup map for routing incoming audio packets by SSRC
    //private final Map<Long, AudioTrack> audioTracksBySsrc = new ConcurrentHashMap<>();
    int sdpVersion = 1;

    private final long sdpSessionId = new SecureRandom().nextLong() & 0x7FFFFFFFFFL;

    public RTCConnection(IceTransport iceTransport, DtlsTransport dtlsTransport) {
        this.iceTransport = iceTransport;
        this.dtlsTransport = dtlsTransport;

        iceTransport.oneIceReady().addListener(v -> onIceReady());

        iceTransport.onRawPacketReceived().addListener(this::handleRawPacket);

        dtlsTransport.onDtlsHandshakeComplete().addListener(this::onDtlsHandshakeComplete);

        dtlsTransport.onDtlsAppDataRecieved().addListener(this::dtlsAppPacketReceived);

        dtlsTransport.setOutgoingDataHandler(buffer -> {
            try {
                iceTransport.send(buffer.data(), buffer.offset(), buffer.length());
            } catch (IOException e) {
                LOGGER.warn("Error sending DTLS data through ICE", e);
            }
        });

        //add a default track since webRTC doesn't work without one
        DataChannelTrack defaultTrack = new DataChannelTrack("default", "Default");
        addTrack(defaultTrack);
    }

    public void receivedSessionDescription(RTCSignalingMessages.sessionDescription sessionDescription) {
        if(sessionDescription.getSdpType() != RTCSdpType.ANSWER){
            //Non answers are currently unhandled...
            return;
        }

        String sdp = sessionDescription.sdp();

        // Extract the setup role from the remote SDP so the DTLS stack knows how to act
        if (sdp.contains("a=setup:active")) {
            dtlsTransport.setSetupAttribute("active");
        } else if (sdp.contains("a=setup:passive")) {
            dtlsTransport.setSetupAttribute("passive");
        } else if (sdp.contains("a=setup:actpass")) {
            // If the remote answers with actpass, we should default to acting as the client (active)
            dtlsTransport.setSetupAttribute("passive");
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
        LOGGER.warn("dtlsAppPacketReceived!!!! length: {}", buffer.length());
    }

    private void onIceReady(){
        LOGGER.info("ICE connected");

        TaskPools.IO_POOL.execute(dtlsTransport::startDtlsHandshake);
    }

    private void handleRawPacket(Buffer buffer){
        if (buffer.length() > 0) {
            int firstByte = buffer.data()[buffer.offset()] & 0xFF;

            if(RawPacketUtils.isDTLSPacket(buffer)){
                byte[] copiedData = new byte[buffer.length()];
                System.arraycopy(buffer.data(), buffer.offset(), copiedData, 0, buffer.length());
                Buffer clonedBuffer = new Buffer(copiedData, 0, buffer.length());

                dtlsTransport.enqueueBuffer(clonedBuffer);
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
