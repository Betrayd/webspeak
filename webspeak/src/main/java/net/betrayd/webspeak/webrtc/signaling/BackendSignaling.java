package net.betrayd.webspeak.webrtc.signaling;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.betrayd.webspeak.ServerBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Supplier;

public class BackendSignaling implements SignalingChannel {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendSignaling.class);
    private static final Gson GSON = new Gson();

    private final String sessionID;
    private final ServerBackend backend;

    public BackendSignaling(String sessionID, ServerBackend backend, Supplier<Collection<RTCConnection>> rtcConnections){
        this.sessionID = sessionID;
        this.backend = backend;
        backend.getOnMessageReceived().addListener((event) -> {
            if(event.sessionId().equals(sessionID)){
                JsonObject obj = GSON.fromJson(event.message(), JsonObject.class);

                if(obj != null){
                    String type = obj.get("type").getAsString();
                    switch (type) {
                        case RTCSignalingMessages.iceCandidate.TYPE ->
                                handleReceivedIceCandidate(rtcConnections.get(), GSON.fromJson(obj, RTCSignalingMessages.iceCandidate.class));
                        case RTCSignalingMessages.sessionDescription.TYPE ->
                                handleReceivedSessionDescription(rtcConnections.get(), GSON.fromJson(obj, RTCSignalingMessages.sessionDescription.class));
                    }
                }
            }
        });
    }

    @Override
    public void sendIceCandidate(RTCSignalingMessages.iceCandidate iceCandidate) {
        try {
            backend.sendMessage(sessionID, RTCSignalingMessages.write(iceCandidate)).whenComplete((s, e)->{
                if(e!=null){
                    LOGGER.error("{} - Failed to send iceCandidates", sessionID, e);
                    //send error here so the implementation of RTCConnection may try to reestablish
                }
            });
        }
        catch(Exception e){
            LOGGER.error("{} - Failed to send iceCandidates", sessionID, e);
            //send error here so the implementation of RTCConnection may try to reestablish
        }
    }

    @Override
    public void sendDescription(RTCSignalingMessages.sessionDescription offer) {
        try {
            backend.sendMessage(sessionID, RTCSignalingMessages.write(offer)).whenComplete((s, e)->{
                if(e!=null){
                    LOGGER.error("{} - Failed to send sessionDescription", sessionID, e);
                    //send error here so the implementation of RTCConnection may try to reestablish
                }
            });
        }
        catch(Exception e){
            LOGGER.error("{} - Failed to send sessionDescription", sessionID, e);
            //send error here so the implementation of RTCConnection may try to reestablish
        }
    }
}
