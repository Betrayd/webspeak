package net.betrayd.webspeak.webrtc.rtc_java.signaling;

import dev.onvoid.webrtc.*;
import lombok.Getter;
import lombok.Setter;
import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.webrtc.ice4j.RTCSignalingMessages;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public abstract class RTCConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(RTCConnection.class);
    //Yes this starts on 1, that's on purpose so that if we get 0 it's an error
    //this exposes the number of data channels the server has made to the client, but I really don't think that's a big issue
    public static String getNextIdentifier(){
        return UUID.randomUUID().toString();
    }

    private final RTCPeerConnection peerConnection;
    @Getter @Nullable @Setter
    private SignalingChannel signaling;
    @Getter
    private final String identifier;

    private final Event.Invokable<String> onEstablishmentError = Event.create();

    public RTCConnection(PeerConnectionFactory factory, RTCConfiguration config, String RTCIdentifier){
        this.identifier = RTCIdentifier;
        this.peerConnection = factory.createPeerConnection(config,
            this.getPeerConnectionObserver()
        );
    }

    public HandledPeerConnectionObserver getPeerConnectionObserver() {
        return new HandledPeerConnectionObserver(this);
    }

    public HandledSessionDescriptionOfferObserver getSessionDescriptionOfferObserver() {
        return new HandledSessionDescriptionOfferObserver(this);
    }

    public HandledSetSessionDescriptionEmptyObserver getSessionDescriptionCompleteObserver() {
        return new HandledSetSessionDescriptionEmptyObserver(this);
    }

    public HandledSessionDescriptionAnswerObserver getSessionDescriptionAnswerObserver() {
        return new HandledSessionDescriptionAnswerObserver(this);
    }

    protected RTCPeerConnection getPeerConnection(){
        return peerConnection;
    }

    public Event<String> onEstablishmentError(){
        return onEstablishmentError;
    }

    public void createOffer(){
        RTCOfferOptions options = new RTCOfferOptions();

        peerConnection.createOffer(options, getSessionDescriptionOfferObserver());
    }

    public void close(){
        peerConnection.close();
    }

    public void receivedIceCandidate(RTCSignalingMessages.iceCandidate message){
        if(message.sdpMid() == null && message.sdp() == null){
            return;
        }
        try{
            RTCIceCandidate candidate = new RTCIceCandidate(message.sdpMid(), message.sdpMLineIndex(), message.sdp());
            peerConnection.addIceCandidate(candidate);
        }
        catch (Throwable e){
            LOGGER.error("Failed to handle received ice iceCandidates", e);
        }
    }

    public void receivedSessionDescription(RTCSignalingMessages.sessionDescription message){
        RTCSdpType value = message.getSdpType();
        if(value == null){
            return;
        }
        if(value.equals(RTCSdpType.ANSWER)){
            RTCSessionDescription remoteDescription = new RTCSessionDescription(value, message.sdp());
            peerConnection.setRemoteDescription(remoteDescription, getSessionDescriptionCompleteObserver());
            return;
        }
        if(value.equals(RTCSdpType.OFFER)){
            RTCAnswerOptions options = new RTCAnswerOptions();

            peerConnection.createAnswer(options, getSessionDescriptionAnswerObserver());
        }
    }

    //end class

    public static class HandledSetSessionDescriptionEmptyObserver implements SetSessionDescriptionObserver{

        private final RTCConnection con;

        public HandledSetSessionDescriptionEmptyObserver(RTCConnection con){
            this.con = con;
        }

        @Override
        public void onSuccess() {

        }

        @Override
        public void onFailure(String error) {
            LOGGER.error("Failed to set remote description: {}", error);
            con.onEstablishmentError.invoke(error);
        }
    }

    public static class HandledSetSessionDescriptionAnswerObserver implements SetSessionDescriptionObserver{

        private final RTCConnection con;
        private final RTCSessionDescription description;

        public HandledSetSessionDescriptionAnswerObserver(RTCConnection con, RTCSessionDescription description){
            this.con = con;
            this.description = description;
        }

        @Override
        public void onSuccess() {
            RTCSignalingMessages.sessionDescription contents = new RTCSignalingMessages.sessionDescription(description.sdpType.ordinal(), description.sdp, con.getIdentifier());
            if(con.signaling != null){
                try {
                    con.signaling.sendDescription(contents).whenComplete((s, e)->{
                        if(e!=null){
                            LOGGER.error("Failed to send answer", e);
                            //send error here so the implementation of RTCConnection may try to reestablish
                            con.onEstablishmentError.invoke("Failed to send answer");
                        }
                    });
                }
                catch(Exception e){
                    LOGGER.error("Failed to send answer", e);
                    //send error here so the implementation of RTCConnection may try to reestablish
                    con.onEstablishmentError.invoke("Failed to send answer");
                }
            }
            else{
                LOGGER.error("Failed to set local description: {}", "No signaling backend was found");
                con.onEstablishmentError.invoke("No signaling backend was found");
            }
        }

        @Override
        public void onFailure(String error) {
            LOGGER.error("Failed to set local description: {}", error);
            con.onEstablishmentError.invoke(error);
        }
    }

    public static class HandledSetSessionDescriptionOfferObserver implements SetSessionDescriptionObserver{

        private final RTCConnection con;
        private final RTCSessionDescription description;

        public HandledSetSessionDescriptionOfferObserver(RTCConnection con, RTCSessionDescription description){
            this.con = con;
            this.description = description;
        }

        @Override
        public void onSuccess() {
            RTCSignalingMessages.sessionDescription contents = new RTCSignalingMessages.sessionDescription(description.sdpType.ordinal(), description.sdp, con.getIdentifier());
            if(con.signaling != null){
                try {
                    con.signaling.sendDescription(contents).whenComplete((s, e)->{
                        if(e!=null){
                            LOGGER.error("Failed to send local description", e);
                            //send error here so the implementation of RTCConnection may try to reestablish
                            con.onEstablishmentError.invoke("Failed to send local description");
                        }
                    });
                }
                catch(Exception e){
                    LOGGER.error("Failed to send local description", e);
                    //send error here so the implementation of RTCConnection may try to reestablish
                    con.onEstablishmentError.invoke("Failed to send local description");
                }
            }
            else{
                LOGGER.error("Failed to set local description: {}", "No signaling backend was found");
                con.onEstablishmentError.invoke("No signaling backend was found");
            }
        }

        @Override
        public void onFailure(String error) {
            LOGGER.error("Failed to set local description: {}", error);
            con.onEstablishmentError.invoke(error);
        }
    }

    public static class HandledSessionDescriptionAnswerObserver implements CreateSessionDescriptionObserver{
        private final RTCConnection con;

        public HandledSessionDescriptionAnswerObserver(RTCConnection con){
            this.con = con;
        }

        @Override
        public void onSuccess(RTCSessionDescription description) {
            if(con.peerConnection != null){
                con.peerConnection.setLocalDescription(description, getHandledSetSessionDescriptionObserver(description));
            }
        }

        @Override
        public void onFailure(String error) {
            LOGGER.error("Failed to create answer: {}", error);
            con.onEstablishmentError.invoke(error);
        }

        public HandledSetSessionDescriptionAnswerObserver getHandledSetSessionDescriptionObserver(RTCSessionDescription description){
            return new HandledSetSessionDescriptionAnswerObserver(con, description);
        }
    }

    public static class HandledSessionDescriptionOfferObserver implements CreateSessionDescriptionObserver{
        private final RTCConnection con;

        public HandledSessionDescriptionOfferObserver(RTCConnection con){
            this.con = con;
        }

        @Override
        public void onSuccess(RTCSessionDescription description) {
            if(con.peerConnection != null){
                con.peerConnection.setLocalDescription(description, getHandledSetSessionDescriptionObserver(description));
            }
        }

        @Override
        public void onFailure(String error) {
            LOGGER.error("Failed to create offer: {}", error);
            con.onEstablishmentError.invoke(error);
        }

        public HandledSetSessionDescriptionOfferObserver getHandledSetSessionDescriptionObserver(RTCSessionDescription description){
            return new HandledSetSessionDescriptionOfferObserver(con, description);
        }
    }

    //class for handling ice iceCandidates externally
    public static class HandledPeerConnectionObserver implements PeerConnectionObserver{

        private final RTCConnection con;

        public HandledPeerConnectionObserver(RTCConnection con){
            this.con = con;
        }

        @Override
        public void onIceCandidate(RTCIceCandidate candidate) {
            RTCSignalingMessages.iceCandidate contents = new RTCSignalingMessages.iceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp, con.getIdentifier());
            if(con.signaling != null){
                try {
                    con.signaling.sendIceCandidate(contents).whenComplete((s, e)->{
                        if(e!=null){
                            LOGGER.error("Failed to send iceCandidates", e);
                            //send error here so the implementation of RTCConnection may try to reestablish
                            con.onEstablishmentError.invoke("Failed to send ice candidate");
                        }
                    });
                }
                catch(Exception e){
                    LOGGER.error("Failed to send iceCandidates", e);
                    //send error here so the implementation of RTCConnection may try to reestablish
                    con.onEstablishmentError.invoke("Failed to send ice candidate");
                }
            }
            else{
                LOGGER.error("Failed to send ice candidate: {}", "No signaling backend was found");
                con.onEstablishmentError.invoke("No signaling backend was found");
            }
        }
    }
}
