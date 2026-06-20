package net.betrayd.webspeak.webrtc;

import net.betrayd.webspeak.webrtc.dtls.old.DatagramTransportImpl;
import net.betrayd.webspeak.webrtc.dtls.old.DtlsServerImpl;
import net.betrayd.webspeak.webrtc.ice.IceCandidateParser;
import net.betrayd.webspeak.webrtc.ice.IceStartData;
import net.betrayd.webspeak.webrtc.ice.IceTransport;
import net.betrayd.webspeak.webrtc.signaling.RTCSdpType;
import net.betrayd.webspeak.webrtc.signaling.RTCSignalingMessages;
import net.betrayd.webspeak.webrtc.signaling.SignalingServer;
import net.betrayd.webspeak.webrtc.tracks.DataChannelTrack;
import net.betrayd.webspeak.webrtc.tracks.RTCTrack;
import org.bouncycastle.tls.DTLSServerProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCryptoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class RTCConnection {
    public static final Logger LOGGER = LoggerFactory.getLogger(RTCConnection.class);

    private final IceTransport iceTransport;

    private final X509Certificate localCert;
    private final PrivateKey localPrivateKey;

    private final Map<String, RTCTrack> tracks = new ConcurrentHashMap<>();
    // Fast lookup map for routing incoming audio packets by SSRC
    //private final Map<Long, AudioTrack> audioTracksBySsrc = new ConcurrentHashMap<>();

    private final long sdpSessionId = new SecureRandom().nextLong() & 0x7FFFFFFFFFL;
    private final ExecutorService handshakeExecutor;

    private final TlsCrypto crypto;

    private int sdpVersion = 1; // incrememented every time we send a new offer

    private boolean startedDtlsHandshake = false;
    private boolean srtpNegotiated = false;

    private DatagramTransportImpl datagramTransport;
    private DTLSTransport dtlsTransport;

    private String localFingerprint;

    public RTCConnection(IceTransport iceTransport, ExecutorService handshakeExecutor, X509Certificate localCert, PrivateKey localPrivateKey) {
        this.iceTransport = iceTransport;
        this.handshakeExecutor = handshakeExecutor;
        this.localCert = localCert;
        this.localPrivateKey = localPrivateKey;
        this.crypto = new JcaTlsCryptoProvider().create(new SecureRandom());

        //Instantiate early to catch aggressive WebRTC ClientHello packets. Probably won't work
        this.datagramTransport = new DatagramTransportImpl(iceTransport, 2048, 1200);

        iceTransport.oneIceReady().addListener(v -> negotiateDtlsHandshake());

        iceTransport.onRawPacketReceived().addListener(this::handleRawPacket);

        //add a default track since webRTC doesn't work without one
        DataChannelTrack defaultTrack = new DataChannelTrack("default", "Default");
        addTrack(defaultTrack);
    }

    public void receivedSessionDescription(RTCSignalingMessages.sessionDescription sessionDescription) {
        if(sessionDescription.getSdpType() != RTCSdpType.ANSWER){
            //Non answers are currently unhandled...
            return;
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
        //Don't re-initiate the fingerprint
        if (this.localFingerprint == null) {
            DtlsServerImpl dtlsServer = new DtlsServerImpl(crypto, localCert, localPrivateKey);
            this.localFingerprint = dtlsServer.getLocalFingerprint();
        }

        // 1. Extract the local parameters that ice4j gathered during IceTransport.init()
        String localUfrag = iceTransport.getLocalUfrag();
        String localPwd = iceTransport.getLocalPassword();
        String localCandidates = iceTransport.getLocalCandidatesAsSdp();

        // 2. Assemble the full WebRTC-compliant SDP Offer string
        String generatedSdpOffer = buildLocalSdpOffer(localUfrag, localPwd, localFingerprint, localCandidates);

        // 3. Dispatch the Offer to the client (assuming 0 represents RTCSdpType.OFFER)
        RTCSignalingMessages.sessionDescription offerPacket =
                new RTCSignalingMessages.sessionDescription(0, generatedSdpOffer, "sfu-stream");

        signalingServer.sendSessionDescription(offerPacket);

        sdpVersion++;
    }

    // Add this helper to construct the SDP string using the variables
    private String buildLocalSdpOffer(String ufrag, String password, String fingerprint, String candidatesSdp) {
        StringBuilder sdp = new StringBuilder();

        // Base Session Description Protocol Headers
        sdp.append("v=0\r\n");
        // The version field (3rd parameter) increments on renegotiation
        sdp.append("o=- ").append(sdpSessionId).append(" ").append(sdpVersion).append(" IN IP4 0.0.0.0\r\n");
        sdp.append("s=-\r\n");
        sdp.append("t=0 0\r\n");
        sdp.append("a=rtcp-mux\r\n");

        int midCounter = 0;
        boolean hasDataChannel = false;

        // Append Media Blocks dynamically for each registered track
        for (RTCTrack track : getTracks()) {
            //No audio tracks yet
            /*if (track instanceof AudioTrack) {
                AudioTrack audioTrack = (AudioTrack) track;
                sdp.append("m=audio 9 UDP/TLS/RTP/SAVPF ").append(audioTrack.getPayloadType()).append("\r\n");
                sdp.append("c=IN IP4 0.0.0.0\r\n");
                sdp.append("a=setup:actpass\r\n"); // SFU provides actpass, client answers active
                sdp.append("a=mid:").append(midCounter).append("\r\n");
                sdp.append("a=rtpmap:").append(audioTrack.getPayloadType()).append(" opus/48000/2\r\n");
                sdp.append("a=ice-ufrag:").append(ufrag).append("\r\n");
                sdp.append("a=ice-pwd:").append(password).append("\r\n");
                sdp.append("a=fingerprint:sha-256 ").append(fingerprint).append("\r\n");
                sdp.append("a=msid:sfu-session ").append(track.getId()).append("\r\n");
                sdp.append("a=ssrc:").append(audioTrack.getSsrc()).append(" cname:sfu-audio\r\n");
                sdp.append("a=sendonly\r\n"); // SFU is sending this audio track to the client

                midCounter++;
            } else*/
            if (track instanceof DataChannelTrack) {
                hasDataChannel = true;
            }
        }

        // Standard WebRTC aggregates all individual Data Channels into a single SCTP m-line block
        if (hasDataChannel) {
            sdp.append("m=application 9 UDP/DTLS/SCTP webrtc-datachannel\r\n");
            sdp.append("c=IN IP4 0.0.0.0\r\n");
            sdp.append("a=setup:actpass\r\n");
            sdp.append("a=mid:").append(midCounter).append("\r\n");
            sdp.append("a=sctp-port:5000\r\n"); // Default WebRTC data channel port mapping
            sdp.append("a=ice-ufrag:").append(ufrag).append("\r\n");
            sdp.append("a=ice-pwd:").append(password).append("\r\n");
            sdp.append("a=fingerprint:sha-256 ").append(fingerprint).append("\r\n");

            //update in case we add more for some reason (we won't)
            midCounter++;
        }
        else{
            LOGGER.error("Something went catastrophically wrong. You have reached an unreachable state. Good job. (No data channel on establish RTC connection)");
        }

        //ice candidates added to the end
        sdp.append(candidatesSdp);

        return sdp.toString();
    }

    public void close() {
        if (dtlsTransport != null) {
            try {
                dtlsTransport.close();
            } catch (IOException ignored) {}
        }
        iceTransport.stop();
    }

    private void negotiateDtlsHandshake(){
        // accept() blocks waiting for queues. We MUST offload it to a background thread
        if(startedDtlsHandshake){
            return;
        }
        startedDtlsHandshake = true;

        handshakeExecutor.submit(() -> {
            try {
                LOGGER.info("Ice Connection ready state. Establishing DTLS handshake...");

                DtlsServerImpl dtlsServer = new DtlsServerImpl(crypto, localCert, localPrivateKey);
                DTLSServerProtocol protocol = new DTLSServerProtocol();

                this.dtlsTransport = protocol.accept(dtlsServer, datagramTransport);

                LOGGER.info("DTLS handshake successful");

            }catch(Exception e){
                LOGGER.error("Negotiate DTLS handshake error",e);
            }
        });
    }

    private void handleRawPacket(Buffer rawPacket) {
        if (rawPacket.length() > 0) {
            int firstByte = rawPacket.data()[rawPacket.offset()] & 0xFF;
            // 0 to 3 is STUN (Handled by ICE4J)
            // WebRTC Multiplexing rules: 20 to 63 is DTLS

            if (firstByte >= 20 && firstByte <= 63) {
                //implement this somewhere
                if(datagramTransport != null) {
                    datagramTransport.offer(rawPacket.data(), rawPacket.offset(), rawPacket.length());
                }else{
                    LOGGER.warn("DTLS packet sent without first creating");
                }
            }
            // 128 to 191 is RTP/RTCP (Forward directly to audio pipeline)
            else if (firstByte > 127 && firstByte < 192) {
                boolean isRtcp = (rawPacket.data()[rawPacket.offset() + 1] & 0xFF) >= 192 && (rawPacket.data()[rawPacket.offset() + 1] & 0xFF) <= 223;

                if (isRtcp) {
                    // Handle SRTP decryption for RTCP
                } else {
                    // 1. Decrypt the SRTP packet using keys from getSrtpKeyingMaterial()
                    // byte[] decryptedRtp = srtpContext.unprotect(rawPacket);

                    // 2. Read the SSRC from bytes 8-11 of the decrypted RTP header
                    // long ssrc = readSsrc(decryptedRtp);

                    // 3. Route to the correct audio track
                    // AudioTrack targetTrack = audioTracksBySsrc.get(ssrc);
                    // if (targetTrack != null) {
                    //     targetTrack.onRtpPacketReceived(decryptedRtp);
                    // }
                }
            }
        }
    }

    private void handSRTPKey(){
        LOGGER.info("Handing SRTP Key");
    }
}
