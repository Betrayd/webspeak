package net.betrayd.webspeak.webrtc.dtls.old;

import net.betrayd.webspeak.webrtc.ice.IceTransport;
import org.bouncycastle.tls.DatagramTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatagramTransportImpl implements DatagramTransport {
    public static final Logger LOGGER = LoggerFactory.getLogger(DatagramTransportImpl.class);

    private final IceTransport iceTransport;
    private final int receiveLimit;
    private final int sendLimit;

    volatile boolean closed = false;

    //TODO: fix potential exploit and memory leak crash by adding some kind of limit here.
    //^ I don't know I'm learning as I go.
    private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>(100);

    public DatagramTransportImpl(IceTransport transport, int receiveLimit, int sendLimit) {
        this.iceTransport = transport;
        this.receiveLimit = receiveLimit;
        this.sendLimit = sendLimit;
    }

    public void offer(byte[] data, int offset, int length) {
        if(!closed){
            byte[] copy = new byte[length];
            System.arraycopy(data, offset, copy, 0, length);
            if (!queue.offer(copy)){
                LOGGER.error("DTLS queue full. Dropping packet");
            }
        }
    }

    @Override
    public int getReceiveLimit() throws IOException {
        return receiveLimit;
    }

    @Override
    public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
        try {
            byte[] dtlsPacket;
            if(waitMillis > 0){
                dtlsPacket = queue.poll(waitMillis, TimeUnit.MILLISECONDS);

                // FIX: Throw a timeout exception so Bouncy Castle knows to re-transmit
                // missing packets, rather than treating a null packet as a dead socket.
                if (dtlsPacket == null) {
                    throw new java.net.SocketTimeoutException("DTLS receive timeout");
                }
            }
            else{
                dtlsPacket = queue.take();
            }

            // Cleanly handle the poison pill for actual shutdowns
            if (closed && dtlsPacket.length == 0) {
                return -1;
            }

            int copyLen = Math.min(len, dtlsPacket.length);
            System.arraycopy(dtlsPacket, 0, buf, off, copyLen);
            return copyLen;

        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for packet", e);
        }
    }

    @Override
    public int getSendLimit() throws IOException {
        return sendLimit;
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        if (!closed){
            iceTransport.send(buf, off, len);
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            queue.clear();
            queue.offer(new byte[0]); // this is apparently a thing called a poison pill. It allows to kill the blocking thread early
        }
    }
}
