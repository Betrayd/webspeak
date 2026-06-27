package net.betrayd.webspeak.webrtc;

import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.webrtc.dtls.DtlsTransport;
import net.betrayd.webspeak.webrtc.ice.IceTransport;
import net.betrayd.webspeak.webrtc.ice.LocalCandidate;
import net.betrayd.webspeak.webrtc.signaling.BackendSignaling;
import net.betrayd.webspeak.webrtc.signaling.SignalingServer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.*;
import java.util.Collection;

public class RTCManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(RTCManager.class);
    public final ServerBackend serverBackend;
    public final WebSpeakServer server;

    public RTCManager(Collection<LocalCandidate> localCandidates, ServerBackend serverBackend, WebSpeakServer server, boolean useUniquePorts) {
        this.serverBackend = serverBackend;
        this.server = server;

        // 1. Explicitly register Bouncy Castle if it hasn't been done yet
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        serverBackend.getOnClientConnected().addListener(sessionID -> onClientConnected(sessionID, localCandidates, useUniquePorts));
    }

    private void onClientConnected(String sessionID, Collection<LocalCandidate> localCandidates, boolean useUniquePorts) {
        WebSpeakPlayer player = server.getPlayers().get(sessionID);

        if(player == null) {
            //TODO: Error recovery
            LOGGER.warn("Player not found for sessionID {}", sessionID);
            return;
        }


        IceTransport iceTransport = new IceTransport(localCandidates,useUniquePorts);
        try{
            iceTransport.init();
        }
        catch(IOException e){
            //TODO: Error recovery
            LOGGER.error("Failed to initialize ICE transport", e);
            return;
        }
        DtlsTransport dtlsTransport;
        try {
            dtlsTransport = new DtlsTransport();
        }
        catch(Exception e){
            //TODO: Error recovery
            LOGGER.error("Failed to initialize DTLS transport", e);
            return;
        }

        RTCConnection rtcConnection = new RTCConnection(iceTransport, dtlsTransport);
        BackendSignaling webSocketSignaling = new BackendSignaling(sessionID, serverBackend);
        SignalingServer signalingServer = new SignalingServer(rtcConnection, webSocketSignaling);

        PlayerRTCConnection playerRTCConnection = new PlayerRTCConnection(rtcConnection, signalingServer);
        player.setConnection(playerRTCConnection);

        rtcConnection.initiateSfuOffer(signalingServer);
    }
}
