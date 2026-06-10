package net.betrayd.webspeak.webrtc.signaling;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.webrtc.RTCConnection;
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

    private final Event.Invokable<RTCSignalingMessages.sessionDescription> sessionDescriptionReceivedEvent = Event.create();
    private final Event.Invokable<RTCSignalingMessages.iceCandidate> iceCandidateReceivedEvent = Event.create();

    public BackendSignaling(String sessionID, ServerBackend backend){
        this.sessionID = sessionID;
        this.backend = backend;
        backend.getOnMessageReceived().addListener((event) -> {
            if(event.sessionId().equals(sessionID)){
                JsonObject obj = GSON.fromJson(event.message(), JsonObject.class);

                if(obj != null){
                    String type = obj.get("type").getAsString();
                    switch (type) {
                        case RTCSignalingMessages.sessionDescription.TYPE ->
                                sessionDescriptionReceivedEvent.invoke(GSON.fromJson(obj, RTCSignalingMessages.sessionDescription.class));
                        case RTCSignalingMessages.iceCandidate.TYPE ->
                                iceCandidateReceivedEvent.invoke(GSON.fromJson(obj, RTCSignalingMessages.iceCandidate.class));
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<?> sendDescription(RTCSignalingMessages.sessionDescription offer) {
        return backend.sendMessage(sessionID, RTCSignalingMessages.write(offer));
    }

    @Override
    public CompletableFuture<?> sendIceCandidate(RTCSignalingMessages.iceCandidate iceCandidate) {
        return backend.sendMessage(sessionID, RTCSignalingMessages.write(iceCandidate));
    }

    @Override
    public Event<RTCSignalingMessages.sessionDescription> onReceivedSessionDescription() {
        return sessionDescriptionReceivedEvent;
    }

    @Override
    public Event<RTCSignalingMessages.iceCandidate> onReceivedIceCandidate() {
        return iceCandidateReceivedEvent;
    }
}
