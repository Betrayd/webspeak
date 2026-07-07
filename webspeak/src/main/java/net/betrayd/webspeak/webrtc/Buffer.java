package net.betrayd.webspeak.webrtc;

//When using in iceTransport this may cause audio jitter. Passing Buffer directly from ice4j may be better
public class Buffer {
    public static final int BYTES_TO_LEAVE_AT_END_OF_PACKET = 20;

    private final byte[] data;
    private final int offset;
    private final int length;
    public Buffer(byte[] data, int offset, int length){
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    public byte[] getData(){
        return data;
    }
    public int getOffset(){
        return offset;
    }
    public int getLength(){
        return length;
    }

    protected byte[] cloneBuffer(int bytesToLeaveAtStart){
        byte[] clone = new byte[bytesToLeaveAtStart + length + BYTES_TO_LEAVE_AT_END_OF_PACKET];
        System.arraycopy(
                data,
                offset,
                clone,
                bytesToLeaveAtStart,
                length
        );
        return clone;
    }
}
