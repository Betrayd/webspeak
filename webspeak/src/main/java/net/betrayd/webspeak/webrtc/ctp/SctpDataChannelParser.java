package net.betrayd.webspeak.webrtc.ctp;

import org.bouncycastle.tls.DTLSTransport;
import org.jetbrains.annotations.NotNull;
import org.jitsi.dcsctp4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class SctpDataChannelParser implements DcSctpSocketCallbacks {
    public static final Logger LOGGER = LoggerFactory.getLogger(SctpDataChannelParser.class);

    private final DTLSTransport transport;
    private final DcSctpSocketInterface socket;

    public SctpDataChannelParser(DTLSTransport dtlsTransport){
        this.transport = dtlsTransport;

        DcSctpOptions options = new DcSctpOptions();

        DcSctpSocketFactory factory = new DcSctpSocketFactory();

        this.socket = factory.create("SFU-SCTP", this, null, options);

        this.socket.connect();
    }

    /**
     * Call when DTLS decrypts a packet
     * @param data
     * @param offset
     * @param length
     */
    public void onDtlsPacketDecrypted(byte[] data, int offset, int length) {
        //byte[] reSizedData = new byte[length];
        //System.arraycopy(data, offset, reSizedData, 0, length);

        socket.receivePacket(data, offset, length);
    }

    public void sendString(short streamId, String string) {
        byte[] payload = string.getBytes(StandardCharsets.UTF_8);

        // PPID 51 indicates a WebRTC String. PPID 53 indicates Binary.
        DcSctpMessage message = new DcSctpMessage(streamId, 51, payload);
        //This is where we specify reliability
        SendOptions sendOptions = new SendOptions();
        socket.send(message, sendOptions);
    }

    @Override
    public SendPacketStatus sendPacketWithStatus(byte @NotNull [] bytes) {
        return null;
    }

    @Override
    public Timeout createTimeout(@NotNull DelayPrecision delayPrecision) {
        return null;
    }

    @Override
    public @NotNull Instant Now() {
        return null;
    }

    @Override
    public long getRandomInt(long l, long l1) {
        return 0;
    }

    @Override
    public void OnMessageReceived(@NotNull DcSctpMessage dcSctpMessage) {
        short streamId = dcSctpMessage.getStreamID();
        int PPid = dcSctpMessage.getPpid();
        byte[] payload = dcSctpMessage.getPayload();

        if(PPid == 51) {
            String text = new String(payload, StandardCharsets.UTF_8);
            System.out.println("DataChannel[" + streamId + "] Received: " + text);

            //Route back to data channel track
        }
        else if(PPid == 53) {
            LOGGER.warn("Data channel received binary data, which is not supported");
        }
    }

    @Override
    public void OnError(@NotNull ErrorKind errorKind, @NotNull String s) {

    }

    @Override
    public void OnAborted(@NotNull ErrorKind errorKind, @NotNull String s) {

    }

    @Override
    public void OnConnected() {

    }

    @Override
    public void OnClosed() {

    }

    @Override
    public void OnConnectionRestarted() {

    }

    @Override
    public void OnStreamsResetFailed(short @NotNull [] shorts, @NotNull String s) {

    }

    @Override
    public void OnStreamsResetPerformed(short @NotNull [] shorts) {

    }

    @Override
    public void OnIncomingStreamsReset(short @NotNull [] shorts) {

    }
}
