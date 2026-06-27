package net.betrayd.webspeak.webrtc.dcsctp;

import net.betrayd.webspeak.webrtc.Buffer;
import org.jetbrains.annotations.Nullable;
import org.jitsi.dcsctp4j.*;

public class DcSctpTransport {
    public static int DEFAULT_SCTP_PORT = 5000;
    private static final long DEFAULT_MAX_TIMER_DURATION = 3000L;
    private static DcSctpOptions options;
    private static SendOptions sendOptions;
    public static DcSctpOptions getDefaultOptions(){
        if (options != null){
            return options;
        }
        options = new DcSctpOptions();
        options.setMaxTimerBackoffDuration(DEFAULT_MAX_TIMER_DURATION);
        options.setMaxRetransmissions(null);
        options.setMaxInitRetransmits(null);
        return options;
    }
    public static SendOptions getSendOptions(){
        if (sendOptions != null){
            return sendOptions;
        }
        sendOptions = new SendOptions();
        return sendOptions;
    }

    private static DcSctpSocketFactory socketFactory = new DcSctpSocketFactory();
    private final Object lock = new Object();
    @Nullable
    private DcSctpSocketInterface socket;

    public void start(DcSctpSocketCallbacks callbacks, DcSctpOptions options) {
        synchronized (lock) {
            socket = socketFactory.create("Scoket: ", callbacks, null, options);
        }
    }

    public void stop() {
        synchronized (lock) {
            if (socket != null) {
                socket.close();
            }
            socket = null;
        }
    }

    public void handleIncomingSctp(Buffer buffer) {
        synchronized (lock) {
            if (socket != null) {
                socket.receivePacket(buffer.data(), buffer.offset(), buffer.length());
            }
        }
    }

    public void connect() {
        synchronized (lock) {
            if (socket != null) {
                socket.connect();
            }
        }
    }

    public SendStatus send(DcSctpMessage message, SendOptions sendOptions) {
        synchronized (lock) {
            if (socket != null) {
                SendStatus status = socket.send(message, sendOptions);
                if (status == null) {
                    return status;
                }
            }
        }
        return SendStatus.kErrorShuttingDown;
    }

    public void handleTimeout(long timeoutId) {
        synchronized (lock) {
            if (socket != null) {
                socket.handleTimeout(timeoutId);
            }
        }
    }
}
