package net.betrayd.webspeak.webrtc;

import net.betrayd.webspeak.webrtc.dtls.DatagramTransportImpl;
import net.betrayd.webspeak.webrtc.dtls.DtlsServerImpl;
import net.betrayd.webspeak.webrtc.ice.IceCandidateParser;
import net.betrayd.webspeak.webrtc.ice.IceStartData;
import net.betrayd.webspeak.webrtc.ice.IceTransport;
import net.betrayd.webspeak.webrtc.signaling.RTCSignalingMessages;
import net.betrayd.webspeak.webrtc.signaling.SignalingServer;
import org.bouncycastle.tls.DTLSServerProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class RTCConnection {
    public static final Logger LOGGER = LoggerFactory.getLogger(RTCConnection.class);

    private final IceTransport iceTransport;

    private final X509Certificate localCert;
    private final PrivateKey localPrivateKey;

    private boolean startedDtlsHandshake = false;
    private boolean srtpNegotiated = false;

    private DatagramTransportImpl datagramTransport;
    private DTLSTransport dtlsTransport;

    private String localFingerprint;

    public RTCConnection(IceTransport iceTransport, X509Certificate localCert, PrivateKey localPrivateKey) {
        this.iceTransport = iceTransport;
        this.localCert = localCert;
        this.localPrivateKey = localPrivateKey;

        iceTransport.oneIceReady().addListener(v -> negotiateDtlsHandshake());

        iceTransport.onRawPacketReceived().addListener(this::handleRawPacket);
    }

    public void receivedSessionDescription(RTCSignalingMessages.sessionDescription sessionDescription) {
        IceStartData data = null;
        try {
            data = IceStartData.fromSdp(sessionDescription.sdp());
        }
        catch (IllegalArgumentException e) {
            //TODO: is it safe to pass user input straight through console? IDK, Log4Shell?
            LOGGER.warn("received bad ice packet {}", sessionDescription.sdp());
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
            LOGGER.warn("received bad session description packet");
        }

        if(data != null){
            iceTransport.addIceCandidate(IceCandidateParser.parse(iceCandidate.sdp()));
        }
    }

    public void initiateSfuOffer(SignalingServer signalingServer) {
        TlsCrypto crypto = new BcTlsCrypto(new SecureRandom());
        DtlsServerImpl dtlsServer = new DtlsServerImpl(crypto, localCert, localPrivateKey);
        this.localFingerprint = dtlsServer.getLocalFingerprint();

        // 1. Extract the local parameters that ice4j gathered during IceTransport.init()
        String localUfrag = iceTransport.getLocalUfrag();
        String localPwd = iceTransport.getLocalPassword();
        String localCandidates = iceTransport.getLocalCandidatesAsSdp();

        // 2. Assemble the full WebRTC-compliant SDP Offer string
        String generatedSdpOffer = buildLocalSdpOffer(localUfrag, localPwd, localFingerprint, localCandidates);

        // 3. Dispatch the Offer to the client (assuming 0 represents RTCSdpType.OFFER)
        RTCSignalingMessages.sessionDescription offerPacket =
                new RTCSignalingMessages.sessionDescription(0, generatedSdpOffer, "sfu-audio-stream");

        signalingServer.sendSessionDescription(offerPacket);
    }

    // Add this helper to construct the SDP string using the variables
    private String buildLocalSdpOffer(String ufrag, String password, String fingerprint, String candidatesSdp) {
        return "v=0\r\n" +
                "o=- 1234567890123456 1 IN IP4 0.0.0.0\r\n" +
                "s=-\r\n" +
                "t=0 0\r\n" +
                "m=audio 9 UDP/TLS/RTP/SAVPF 111\r\n" +
                "c=IN IP4 0.0.0.0\r\n" +
                "a=rtcp-mux\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "a=setup:actpass\r\n" +
                "a=ice-ufrag:" + ufrag + "\r\n" +
                "a=ice-pwd:" + password + "\r\n" +
                "a=fingerprint:sha-256 " + fingerprint + "\r\n" +
                candidatesSdp;
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
        Thread dtlsThread = new Thread(() -> {

            try {
                LOGGER.info("Ice Connection ready state. Establishing DTLS handshake...");

                this.datagramTransport = new DatagramTransportImpl(iceTransport, 2048, 2048);

                TlsCrypto crypto = new BcTlsCrypto(new SecureRandom());
                DtlsServerImpl dtlsServer = new DtlsServerImpl(crypto, localCert, localPrivateKey);

                DTLSServerProtocol protocol = new DTLSServerProtocol();

                this.dtlsTransport = protocol.accept(dtlsServer, datagramTransport);

                LOGGER.info("DTLS handshake successful");

            }catch(IOException e){
                LOGGER.error("Negotiate DTLS handshake error",e);
            }
        });

        dtlsThread.setDaemon(true);
        dtlsThread.start();
    }

    private void handleRawPacket(IceTransport.Buffer rawPacket) {
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
                // Handle SRTP decryption/forwarding
            }
        }
    }

    private void handSRTPKey(){
        LOGGER.info("Handing SRTP Key");
    }
}
