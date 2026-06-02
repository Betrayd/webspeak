package net.betrayd.webspeak.webrtc.rtc_java.signaling;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.webrtc.ice4j.RTCSignalingMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
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
    public CompletableFuture<?> sendIceCandidate(RTCSignalingMessages.iceCandidate iceCandidate) {
        return backend.sendMessage(sessionID, RTCSignalingMessages.write(iceCandidate));
    }

    @Override
    public CompletableFuture<?> sendDescription(RTCSignalingMessages.sessionDescription offer) {
        return backend.sendMessage(sessionID, RTCSignalingMessages.write(offer));
    }
}
