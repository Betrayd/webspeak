package net.betrayd.webspeak.webrtc.signaling;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.onvoid.webrtc.RTCSdpType;
import org.jetbrains.annotations.Nullable;

public class RTCSignalingMessages {
    private RTCSignalingMessages() {};
    public interface RTCSignalingMessage {
        int getRTCIdentifier();
        String getType();
    }
    public record iceCandidate(String sdpMid, int sdpMLineIndex, String sdp, int identifier) implements RTCSignalingMessage{
        public static final String TYPE = "RTCiceCandidate";

        @Override
        public int getRTCIdentifier() { return identifier; }
        @Override
        public String getType() { return TYPE; }
    }

    public record sessionDescription(int RTCSdpType, String sdp, int identifier) implements  RTCSignalingMessage{
        public static final String TYPE = "RTCsessionDescription";

        /**
         * gets the RTCSdpType of this sessionDescription packet
         * @return the RTCSdpType of this packet, or null if we failed to parse
         */
        @Nullable
        public RTCSdpType getSdpType(){
            RTCSdpType value = null;
            if(dev.onvoid.webrtc.RTCSdpType.values().length < RTCSdpType){
                return null;
            }
            return dev.onvoid.webrtc.RTCSdpType.values()[RTCSdpType];
        }

        @Override
        public int getRTCIdentifier() { return identifier; }
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
