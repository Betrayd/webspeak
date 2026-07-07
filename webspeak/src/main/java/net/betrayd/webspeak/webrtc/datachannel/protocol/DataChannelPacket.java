package net.betrayd.webspeak.webrtc.datachannel.protocol;

import net.betrayd.webspeak.webrtc.Packet;
import org.jitsi.dcsctp4j.DcSctpMessage;

public class DataChannelPacket extends Packet {
    private final int sid;
    private final int ppid;
    public DataChannelPacket(byte[] data, int offset, int length, int sid, int ppid) {
        super(data, offset, length);
        this.sid = sid;
        this.ppid = ppid;
    }

    public DataChannelPacket(DcSctpMessage message)
    {
        super(message.getPayload(), 0, message.getPayload().length);
        this.sid = message.getStreamID();
        this.ppid = message.getPpid();
    }

    public int getSid() {
        return sid;
    }

    public int getPpid() {
        return ppid;
    }


    @Override
    public Packet clone() {
        return new DataChannelPacket(getData().clone(), getOffset(), getLength(), sid, ppid);
    }
}
