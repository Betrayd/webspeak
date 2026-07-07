package net.betrayd.webspeak.webrtc.transform;

import net.betrayd.webspeak.webrtc.Buffer;
import net.betrayd.webspeak.webrtc.transform.format.PayloadType;
import net.betrayd.webspeak.webrtc.transform.timeline.EventTimeline;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/**
 * [PacketInfo] is a wrapper around a [Packet] instance to be passed through
 * a pipeline.  Since the [Packet] can change as it moves through the pipeline
 * (as it is parsed into different types), the wrapping [PacketInfo] stays consistent
 * and allows for metadata to be passed along with a packet.
 */
public class PacketInfo {
    private final Buffer buffer;
    @Nullable
    private final EventTimeline timeline;

    private Instant recievedTime;
    /**
     * Whether the packet originally had cryptex RTP header extensions.
     */
    private boolean originalHadCryptex;
    /**
     * Whether this packet has been recognized to contain only shouldDiscard.
     */
     private boolean shouldDiscard = false;

    /**
     * The ID of the endpoint associated with this packet (i.e. the source endpoint).
     */
    @Nullable
    private String endpointId = null;

    /**
     * Whether this packet indicates a point in which its stream's layering changed, in
     * a way that indicates that bitrate allocation may need to be recomputed.
     */
    boolean layeringChanged = false;

    @Nullable
    PayloadType payloadType;


    /**
     * @param buffer The original length of the packet, i.e. before decryption.  Stays unchanged even if the packet is updated.
     * @param timeline debugging feature. Set to null to not use it.
     */
    public PacketInfo(Buffer buffer, @Nullable EventTimeline timeline) {
        this.buffer = buffer;
        this.timeline = timeline;
    }

    public void setRecievedTime(Instant recievedTime) {
        this.recievedTime = recievedTime;
        if (timeline != null && timeline.getReferenceTime() == null) {
            timeline.setReferenceTime(recievedTime);
        }
    }

    public Buffer getPacket(){
        return buffer;
    }
}
