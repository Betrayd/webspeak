package net.betrayd.webspeak.webrtc;

//When using in iceTransport this may cause audio jitter. Passing Buffer directly from ice4j may be better
public record Buffer(byte[] data, int offset, int length) {

}
