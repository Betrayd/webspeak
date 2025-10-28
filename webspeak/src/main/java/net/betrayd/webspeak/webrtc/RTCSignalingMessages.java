package net.betrayd.webspeak.webrtc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class RTCSignalingMessages {
    private RTCSignalingMessages() {};
    public interface RTCSignalingMessage {
        String getType();
    }
    public record iceCandidate(String sdpMid, int sdpMLineIndex, String sdp) implements RTCSignalingMessage{
        public static final String TYPE = "RTCiceCandidate";

        @Override
        public String getType() { return TYPE; }
    }

    public record sessionDescription(int RTCSdpType, String sdp) implements  RTCSignalingMessage{
        public static final String TYPE = "RTCsessionDescription";

        @Override
        public String getType() {
            return TYPE;
        }
    }

    //public record C2SrequestRTC() implements RTCSignalingMessage{
    //    public static final String TYPE = "RTCrequest";
    //
    //    @Override
    //    public String getType() {
    //        return TYPE;
    //    }
    //}

    private static final Gson GSON = new Gson();

    public static String write(RTCSignalingMessage message){
        JsonObject obj = GSON.toJsonTree(message).getAsJsonObject();
        obj.addProperty("type", message.getType());
        return GSON.toJson(obj);
    }
}
