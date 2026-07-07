package net.betrayd.webspeak.webrtc.utils;

import net.betrayd.webspeak.webrtc.Buffer;

public class RawPacketUtils {
    public static int DTLS_LOWER_BOUND = 20;
    public static int DTLS_UPPER_BOUND = 63;

    public static boolean isDTLSPacket(Buffer buffer){
        if(buffer.getLength() > 0){
            int firstByte = buffer.getData()[buffer.getOffset()] & 0xFF;

            return firstByte >= DTLS_LOWER_BOUND && firstByte <= DTLS_UPPER_BOUND;
        }
        return false;
    }

    public static boolean isRtpRtcp(Buffer buffer){
        return false;
    }
}
