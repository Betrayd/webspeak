package net.betrayd.webspeak.webrtc.srtp;

public record SrtpProfileInformation (
        int cipherKeyLength,
        int cipherSaltLength,
        int cipherName,
        int authFunctionName,
        int authKeyLength,
        int rtcpAuthTagLength,
        int rtpAuthTagLength
) {}
