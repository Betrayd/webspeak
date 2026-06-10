package net.betrayd.webspeak.webrtc;

import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.webrtc.ice.IceTransport;
import net.betrayd.webspeak.webrtc.ice.LocalCandidate;
import net.betrayd.webspeak.webrtc.signaling.BackendSignaling;
import net.betrayd.webspeak.webrtc.signaling.SignalingServer;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

public class RTCManager {

    public final ServerBackend serverBackend;
    public final WebSpeakServer server;

    // Permanent cryptographic identities for this server session
    private final PrivateKey localPrivateKey;
    private final X509Certificate localCertificate;

    public RTCManager(Collection<LocalCandidate> localCandidates, ServerBackend serverBackend, WebSpeakServer server) {
        this.serverBackend = serverBackend;
        this.server = server;

        // 1. Explicitly register Bouncy Castle if it hasn't been done yet
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // 2. Generate the credentials once at server startup
        try {
            KeyPair keyPair = generateECDSAKeyPair();
            this.localPrivateKey = keyPair.getPrivate();
            this.localCertificate = generateSelfSignedCertificate(keyPair);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize WebRTC DTLS cryptographic components", e);
        }

        serverBackend.getOnClientConnected().addListener(sessionID -> onClientConnected(sessionID, localCandidates));
    }

    private void onClientConnected(String sessionID, Collection<LocalCandidate> localCandidates) {
        WebSpeakPlayer player = server.getPlayers().get(sessionID);

        if(player == null) {
            //TODO: Error
            return;
        }


        IceTransport transport = new IceTransport(localCandidates);
        try{
            transport.init();
        }
        catch(IOException e){
            //TODO: Error
            return;
        }

        RTCConnection rtcConnection = new RTCConnection(transport, localCertificate, localPrivateKey);
        BackendSignaling webSocketSignaling = new BackendSignaling(sessionID, serverBackend);
        SignalingServer signalingServer = new SignalingServer(rtcConnection, webSocketSignaling);

        PlayerRTCConnection playerRTCConnection = new PlayerRTCConnection(rtcConnection, signalingServer);
        player.setConnection(playerRTCConnection);

        rtcConnection.initiateSfuOffer(signalingServer);
    }

    /**
     * Generates a standard elliptic curve keypair matching WebRTC standard specifications (P-256 curve)
     */
    private KeyPair generateECDSAKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        keyPairGenerator.initialize(256, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Builds a self-signed version 3 X509 Certificate valid for 1 year
     */
    private X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 24 * 60 * 60 * 1000); // 1 day ago to avoid strict clock mismatches
        Date notAfter = new Date(now + 365L * 24 * 60 * 60 * 1000); // 1 year expiration

        X500Name dnName = new X500Name("CN=WebSpeakSFU");
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dnName,
                serialNumber,
                notBefore,
                notAfter,
                dnName,
                keyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);

        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certHolder);
    }
}
