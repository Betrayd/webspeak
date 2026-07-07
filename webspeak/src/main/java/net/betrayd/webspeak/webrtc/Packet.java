package net.betrayd.webspeak.webrtc;

public abstract class Packet extends Buffer implements Cloneable{
    public Packet(byte[] data, int offset, int length) {
        super(data, offset, length);
    }

    @Override
    public abstract Packet clone();
}
