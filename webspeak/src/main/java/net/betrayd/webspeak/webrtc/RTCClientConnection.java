package net.betrayd.webspeak.webrtc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.onvoid.webrtc.*;
import net.betrayd.webspeak.ServerBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the RTC connection for a single client
 */
public class RTCClientConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(RTCClientConnection.class);

    private final String sessionID;
    private final RTCPeerConnection peerConnection;
    private final ServerBackend serverBackend;

    private static final Gson GSON = new Gson();

    protected RTCClientConnection(String sessionID, PeerConnectionFactory factory, RTCConfiguration config, ServerBackend serverBackend){
        this.sessionID = sessionID;
        this.serverBackend = serverBackend;
        this.peerConnection = factory.createPeerConnection(config,
                new PeerConnectionObserver() {
                    @Override
                    public void onIceCandidate(RTCIceCandidate rtcIceCandidate) {
                        // Send the ICE candidate to the remote peer via your signaling channel
                        try {
                            RTCSignalingMessages.iceCandidate contents = new RTCSignalingMessages.iceCandidate(rtcIceCandidate.sdpMid, rtcIceCandidate.sdpMLineIndex, rtcIceCandidate.sdp, rtcIceCandidate.serverUrl);
                            serverBackend.sendMessage(RTCClientConnection.this.sessionID, RTCSignalingMessages.write(contents)).whenComplete((s, e)->{
                                if(e!=null){
                                    LOGGER.error("{} - Failed to send iceCandidates", RTCClientConnection.this.sessionID, e);
                                }
                            });
                        }
                        catch(Exception e){
                            LOGGER.error("{} - Failed to send iceCandidates", RTCClientConnection.this.sessionID, e);
                        }
                    }
                }
        );

        serverBackend.getOnMessageReceived().addListener(messageEvent -> {
            if(messageEvent.sessionId().equals(this.sessionID)){
                this.onSignalingMessageReceived(messageEvent.message());
            }
        });

        init();
    }

    protected void init(){
        RTCOfferOptions options = new RTCOfferOptions();

        peerConnection.createOffer(options, new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                // Set local description
                peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
                    @Override
                    public void onSuccess() {
                        // Send the offer to the remote peer via your signaling channel
                        try {
                            RTCSignalingMessages.sessionDescription contents = new RTCSignalingMessages.sessionDescription(description.sdpType.ordinal(), description.sdp);
                            serverBackend.sendMessage(sessionID, RTCSignalingMessages.write(contents)).whenComplete((s, e)->{
                                if(e!=null){
                                    LOGGER.error("Failed to send sessionDescription", e);
                                }
                            });
                        }
                        catch(Exception e){
                            LOGGER.error("Failed to send sessionDescription", e);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        LOGGER.error("Failed to set local description: {}", error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                LOGGER.error("Failed to create offer: {}", error);
            }
        });
    }

    protected void onSignalingMessageReceived(String message){
        handleSignalingMessage(message);
    }

    private void handleSignalingMessage(String payload) {
        JsonObject obj = GSON.fromJson(payload, JsonObject.class);

        String type = obj.get("type").getAsString();

        switch (type) {
            case RTCSignalingMessages.iceCandidate.TYPE ->
                    handleReceivedIceCandidate(GSON.fromJson(obj, RTCSignalingMessages.iceCandidate.class));
            case RTCSignalingMessages.sessionDescription.TYPE ->
                    handleReceivedSessionDescription(GSON.fromJson(obj, RTCSignalingMessages.sessionDescription.class));
            default -> LOGGER.warn("{} - Unknown signaling message type: {}", sessionID, type);
        }
    }

    private void handleReceivedIceCandidate(RTCSignalingMessages.iceCandidate message){
        RTCIceCandidate candidate = new RTCIceCandidate(message.sdpMid(), message.sdpMLineIndex(), message.sdp(), message.serverUrl());
        peerConnection.addIceCandidate(candidate);
    }

    private void handleReceivedSessionDescription(RTCSignalingMessages.sessionDescription message){
        RTCSdpType value = null;
        if(RTCSdpType.values().length < message.RTCSdpType()){
            LOGGER.warn("{} - Received bad RTCSdpType for session description", sessionID);
            return;
        }
        value = RTCSdpType.values()[message.RTCSdpType()];
        RTCSessionDescription remoteDescription = new RTCSessionDescription(value, message.sdp());
        peerConnection.setRemoteDescription(remoteDescription, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                LOGGER.info("{} - Remote description set successfully", sessionID);
            }

            @Override
            public void onFailure(String error) {
                LOGGER.error("{} - Failed to set remote description: {}", sessionID, error);
            }
        });
    }
}