package net.betrayd.webspeak.webrtc.transform.format;

import lombok.Getter;
import org.jitsi.utils.MediaType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class PayloadType {
    /**
     * The 7-bit RTP payload type number.
     */
    @Getter
    private final byte pt;
    /**
     * The encoding name.
     */
    @Getter
    private final PayloadTypeEncoding encoding;
    /**
     * The media type (audio or video).
     */
    @Getter
    private final MediaType mediaType;
    /**
     * The RTP clock rate.
     */
    @Getter
    private final int clockRate;
    /**
     * Additional parameters associated with the payload type (e.g. the "apt" used for RTX).
     */
    @Getter
    private final Map<String,String> parameters;
    /**
     * The rtcp feedback messages associated with the payload type (e.g. nack, nack pli, transport-cc, goog-remb, ccm fir, etc).
     */
    @Getter
    private final Set<String> rtcpFeedbackSet;
    /**
     *
     * @param pt The 7-bit RTP payload type number.
     * @param encoding The encoding name.
     * @param mediaType The media type (audio or video).
     * @param clockRate The RTP clock rate.
     * @param parameters Additional parameters associated with the payload type (e.g. the "apt" used for RTX).
     * @param rtcpFeedbackSet The rtcp feedback messages associated with the payload type (e.g. nack, nack pli, transport-cc, goog-remb, ccm fir, etc).
     */
    public PayloadType(byte pt, PayloadTypeEncoding encoding, MediaType mediaType, int clockRate, Map<String, String> parameters, Set<String> rtcpFeedbackSet){
        this.pt = pt;
        this.encoding = encoding;
        this.mediaType = mediaType;
        this.clockRate = clockRate;
        this.parameters = parameters;
        this.rtcpFeedbackSet = new CopyOnWriteArraySet<>(rtcpFeedbackSet);
    }

    public String encodingName() {
        return encoding.name().toLowerCase();
    }

    public String channelsString(){
        return "";
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append(pt).append(" -> ").append(encodingName())
                .append(" (").append(clockRate)
                .append(channelsString())
                .append("): ").append(parameters);

        return builder.toString();
    }

    public static abstract class AudioPayloadType extends PayloadType{

        /**
         * The number of channels
         */
        private final int channels;

        public AudioPayloadType(byte pt, PayloadTypeEncoding encoding, int clockRate, int channels, Map<String, String> parameters) {
            super(pt, encoding, MediaType.AUDIO, clockRate, parameters, new HashSet<>());
            this.channels = channels;
        }

        @Override
        public String channelsString(){
            if(channels > 1){
                return "/"+channels;
            }
            return "";
        }
    }

    public static class OpusPayloadType extends AudioPayloadType{

        public OpusPayloadType(byte pt, int clockRate, int channels, Map<String, String> parameters) {
            super(pt, PayloadTypeEncoding.OPUS, clockRate, channels, parameters);
        }

        public OpusPayloadType(byte pt, int clockRate, int channels) {
            this(pt, clockRate, channels, new ConcurrentHashMap<>());
        }

        public OpusPayloadType(byte pt, int clockRate) {
            this(pt, clockRate, 2);
        }

        public OpusPayloadType(byte pt) {
            this(pt, 48000);
        }
    }

    public static class TelephoneEventPayloadType extends AudioPayloadType{
        public TelephoneEventPayloadType(byte pt, int clockRate, int channels, Map<String, String> parameters) {
            super(pt, PayloadTypeEncoding.TELEPHONE_EVENT, clockRate, channels, parameters);
        }

        public TelephoneEventPayloadType(byte pt, int clockRate, int channels) {
            this(pt, clockRate, channels, new ConcurrentHashMap<>());
        }

        public TelephoneEventPayloadType(byte pt, int clockRate) {
            this(pt, clockRate, 1);
        }
    }

    public static class AudioRedPayloadType extends AudioPayloadType{
        public AudioRedPayloadType(byte pt, int clockRate, int channels, Map<String, String> parameters) {
            super(pt, PayloadTypeEncoding.RED, clockRate, channels, parameters);
        }

        public AudioRedPayloadType(byte pt, int clockRate, int channels) {
            this(pt, clockRate, channels, new ConcurrentHashMap<>());
        }

        public AudioRedPayloadType(byte pt, int clockRate) {
            this(pt, clockRate, 1);
        }

        public AudioRedPayloadType(byte pt) {
            this(pt, 48000);
        }

        public static class OtherAudioPayloadType extends AudioPayloadType{
            private final String encodingName;
            public OtherAudioPayloadType(byte pt, String encodingName, int clockRate, int channels, Map<String, String> parameters) {
                super(pt, PayloadTypeEncoding.OTHER, clockRate, channels, parameters);
                this.encodingName = encodingName;
            }

            public OtherAudioPayloadType(byte pt, String encodingName, int clockRate, int channels) {
                this(pt, encodingName, clockRate, channels, new ConcurrentHashMap<>());
            }

            public OtherAudioPayloadType(byte pt, String encodingName, int clockRate) {
                this(pt, encodingName, clockRate, 1);
            }

            @Override
            public String encodingName() {
                return encodingName.toLowerCase();
            }
        }
    }
}
