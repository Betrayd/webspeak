package net.betrayd.webspeak.webrtc.ice4j;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public class RTCSignalingMessages {
    private RTCSignalingMessages() {};
    public interface RTCSignalingMessage {
        String getRTCIdentifier();
        String getType();
    }
    public record iceCandidate(String sdpMid, int sdpMLineIndex, String sdp, String identifier) implements RTCSignalingMessage{
        public static final String TYPE = "RTCiceCandidate";

        @Override
        public String getRTCIdentifier() { return identifier; }
        @Override
        public String getType() { return TYPE; }
    }

    public record sessionDescription(int RTCSdpType, String sdp, String identifier) implements  RTCSignalingMessage{
        public static final String TYPE = "RTCsessionDescription";

        /**
         * gets the RTCSdpType of this sessionDescription packet
         * @return the RTCSdpType of this packet, or null if we failed to parse
         */
        @Nullable
        public RTCSdpType getSdpType(){
            RTCSdpType value = null;
            if(net.betrayd.webspeak.webrtc.ice4j.RTCSdpType.values().length < RTCSdpType){
                return null;
            }
            return net.betrayd.webspeak.webrtc.ice4j.RTCSdpType.values()[RTCSdpType];
        }

        @Override
        public String getRTCIdentifier() { return identifier; }
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
