package net.betrayd.webspeak.webrtc;

import com.igrium.sfu.*;
import com.igrium.sfu.sdp.SdpUtils;
import lombok.Getter;
import net.betrayd.webspeak.event.Event;
import org.jitsi.nlj.format.OpusPayloadType;
import org.jitsi.nlj.format.PayloadType;
import org.jitsi.nlj.rtp.RtpExtension;
import org.jitsi.nlj.rtp.RtpExtensionType;
import org.jitsi.rtp.rtp.RtpPacket;
import org.jitsi.utils.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RTCConnectionWrapper {
    public static final Logger LOGGER = LoggerFactory.getLogger(RTCConnectionWrapper.class);

    //From the AI vibeSFU demo app
    private static final int EXT_AUDIO_LEVEL_ID = 1;
    private static final byte OPUS_PT = 111;
    private static final Set<String> AUDIO_RTCP_FB = Set.of("transport-cc");
    private static final int EXT_TRANSPORT_CC_ID = 3;

    private static List<PayloadType> audioPayloadTypes()
    {
        Map<String, String> fmtp = new LinkedHashMap<>();
        fmtp.put("minptime", "10");
        fmtp.put("useinbandfec", "1");

        OpusPayloadType opus = new OpusPayloadType(OPUS_PT, 48000, 1, fmtp);

        opus.getRtcpFeedbackSet().addAll(AUDIO_RTCP_FB);

        return List.of(opus);
    }

    private static List<RtpExtension> audioExtensions()
    {
        return List.of(
                new RtpExtension((byte) EXT_AUDIO_LEVEL_ID, RtpExtensionType.SSRC_AUDIO_LEVEL),
                new RtpExtension((byte) EXT_TRANSPORT_CC_ID, RtpExtensionType.TRANSPORT_CC));
    }

    /**
     * Maps one transceiver onto the plain SDP data {@code SdpUtils} deals in. The
     * {@code PayloadType} &rarr; SDP codec mapping lives here, in the host app, and not in
     * {@code SdpUtils} — the library core stays SDP-free and {@code SdpUtils} stays free of
     * {@code org.jitsi.nlj} (the same rule {@code DemoSfu.toPayloadType} follows in the other
     * direction).
     */
    private static SdpUtils.MediaSection toSection(MediaTransceiver transceiver)
    {
        String kind = transceiver.getKind() == MediaKind.AUDIO ? "audio" : "video";
        SdpUtils.MediaSection section = new SdpUtils.MediaSection(transceiver.getMid(), kind);
        section.rejected = transceiver.isStopped();
        section.direction = transceiver.getDirection().getSdpToken();
        section.cname = transceiver.getCname();
        section.msidStream = transceiver.getStreamId();
        section.msidTrack = transceiver.getTrackId();
        if (transceiver.getDirection().isSending())
        {
            section.ssrcs.add(transceiver.getSendSsrc());
        }
        for (PayloadType payloadType : transceiver.getPayloadTypes())
        {
            section.codecs.add(toCodec(payloadType));
        }
        for (RtpExtension extension : transceiver.getRtpExtensions())
        {
            section.extmaps.add(new SdpUtils.Extmap(extension.getId(), extension.getType().getUri()));
        }
        return section;
    }

    private static SdpUtils.Codec toCodec(PayloadType payloadType)
    {
        SdpUtils.Codec codec = new SdpUtils.Codec(payloadType.getPt() & 0xFF);
        // SDP spells these VP8/opus; PayloadTypeEncoding is upper-case.
        codec.name = payloadType.getEncoding() == org.jitsi.nlj.format.PayloadTypeEncoding.OPUS
                ? "opus" : payloadType.getEncoding().name();
        codec.clockRate = payloadType.getClockRate();
        codec.channels = payloadType.getMediaType() == MediaType.AUDIO ? 2 : 1;
        codec.fmtp.putAll(payloadType.getParameters());
        codec.rtcpFb.addAll(payloadType.getRtcpFeedbackSet());
        return codec;
    }

    private String aiCreateOffer(){
        List<SdpUtils.MediaSection> sections = new ArrayList<>();
        sections.add(SdpUtils.MediaSection.dataChannel(dataMid));
        for (MediaTransceiver t : peerConnection.getTransceivers()) sections.add(toSection(t));
        String offerSdp = SdpUtils.buildOffer(peerConnection.getLocalDescription(), sections);
        return offerSdp;
    }
    //End From the AI vibeSFU demo app

    private final Event.Invokable<Void> readyEvent = Event.create();
    private final Event.Invokable<RtpPacket> micDataReceivedEvent = Event.create();

    private boolean hasReadied = false;

    private final SfuPeerConnection peerConnection = new SfuPeerConnection(
            SfuPeerConnection.Role.OFFERER,
            new SfuPeerConnectionObserver() {
                @Override
                public void onConnected() {

                }
                @Override
                public void onDataChannelOpen(DataChannelTrack track) {
                    System.out.println("opened: " + track.getLabel());
                }
                @Override
                public void onDataChannelStringMessage(DataChannelTrack track, String msg) {
                    System.out.println("got: " + msg);
                }
            });

    private final String dataMid = peerConnection.getDataChannelMid();

    private final MediaTransceiver micTransceiver;
    @Getter
    private final DataChannelTrack reliable;
    @Getter
    private final DataChannelTrack unreliable;

    public RTCConnectionWrapper() {
        DataChannelOptions reliableChannelOptions = new DataChannelOptions();
        reliableChannelOptions.ordered(true);
        reliableChannelOptions.priority(512);
        reliable = peerConnection.createDataChannel("reliable", reliableChannelOptions);
        DataChannelOptions unreliableChannelOptions = new DataChannelOptions();
        unreliableChannelOptions.ordered(false);
        unreliableChannelOptions.priority(128);
        unreliableChannelOptions.maxRetransmits(0);
        unreliable = peerConnection.createDataChannel("unreliable", unreliableChannelOptions);
        micTransceiver = peerConnection.addTransceiver(MediaKind.AUDIO, MediaDirection.RECVONLY, audioPayloadTypes(), audioExtensions());
    }

    public Event<Void> onReady() {
        return readyEvent;
    }

    public Event<RtpPacket> onMicDataReceived() {
        return micDataReceivedEvent;
    }

    public String createOffer()
    {
        return aiCreateOffer();
    }

    /**
     * Passes the answer to the underlying RTC connection and adds the mic data.
     * @param sdp the sdp of the sent answer
     * @return a future which completes normally if we succeed, but fails if the answer didn't have a mic track
     */
    public CompletableFuture<Void> passAnswer(String sdp){
        SdpUtils.ParsedSdp answer = SdpUtils.parse(sdp);

        peerConnection.setRemoteDescription(answer.transport);
        SdpUtils.MediaDescription section = answer.getMediaByMid(micTransceiver.getMid());
        if (section == null || section.isRejected() || section.ssrcs.isEmpty())
        {
            LOGGER.warn("No audio in answer (mid {})", micTransceiver.getMid());
            return CompletableFuture.failedFuture(new IllegalArgumentException("No audio in answer (mid {})"));
        }

        long ssrc = section.ssrcs.get(0);
        //If we have a new ssrc in the connection for our mic receiver replace the ssrc listener with this one
        if(micTransceiver.getRemoteSsrc() == ssrc){
            MediaTrack receiveTrack = micTransceiver.setRemoteSsrc(ssrc);
            //Starts listening to data comming from the mic track in order
            receiveTrack.onRtpPacket(micDataReceivedEvent::invoke);
        }

        if(!hasReadied){
            this.hasReadied = true;
            readyEvent.invoke(null);
        }

        return CompletableFuture.completedFuture(null);
    }
}
