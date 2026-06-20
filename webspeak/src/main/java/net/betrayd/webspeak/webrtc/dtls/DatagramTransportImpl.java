package net.betrayd.webspeak.webrtc.dtls;

import net.betrayd.webspeak.webrtc.Buffer;
import org.bouncycastle.tls.DatagramTransport;
import org.jitsi.utils.concurrent.ArrayBlockingQueueWithShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DatagramTransportImpl implements DatagramTransport {
    public static final Logger LOGGER = LoggerFactory.getLogger(DatagramTransportImpl.class);
    private final ArrayBlockingQueueWithShutdown<ByteBuffer> incomingProtocolData;

    private Consumer<Buffer> outgoingDataHandler = null;

    public DatagramTransportImpl(ArrayBlockingQueueWithShutdown<ByteBuffer> incomingProtocolData) {
        this.incomingProtocolData = incomingProtocolData;
    }

    public void setOutgoingDataHandler(Consumer<Buffer> outgoingDataHandler) {
        this.outgoingDataHandler = outgoingDataHandler;
    }

    /**
     * Receive limit computation copied from [org.bouncycastle.tls.UDPTransport]
     */
    @Override
    public int getReceiveLimit() throws IOException {
        return 1500 - 20 - 8;
    }

    @Override
    public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
        ByteBuffer data;
        try {
            data = incomingProtocolData.poll((long) waitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return -1;
        }
        if (data == null) {
            return -1;
        }
        int length = Math.min(len, data.limit());
        if (length < data.limit()) {
            LOGGER.warn("assed buffer size {} was too small to hold incoming data size ({}); data was truncated", len, data.limit());
        }
        System.arraycopy(data.array(), data.arrayOffset(), buf, off, length);
        //There is no actual impl of this in Jitsi. If we decide to add a buffer pool then re-implement
        //BufferPool.returnBuffer(data.array());
        return length;
    }

    /**
     * Send limit computation copied from [org.bouncycastle.tls.UDPTransport]
     */
    @Override
    public int getSendLimit() throws IOException {
        return 1500 - 84 - 8;
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        if(outgoingDataHandler != null) {
            outgoingDataHandler.accept(new Buffer(buf, off, len));
        }
    }

    @Override
    public void close() throws IOException {

    }
}
