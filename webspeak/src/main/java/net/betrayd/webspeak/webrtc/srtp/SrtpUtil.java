package net.betrayd.webspeak.webrtc.srtp;

import org.bouncycastle.tls.SRTPProtectionProfile;

public class SrtpUtil {
    public static SrtpProfileInformation getSrtpProfileInformationFromSrtpProtectionProfile(int srtpProtectionProfile) {
        switch (srtpProtectionProfile) {
            case SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32:
                return new SrtpProfileInformation(
                        128 / 8,
                        112 / 8,
                        SrtpPolicy.AESCM_ENCRYPTION,
                        SrtpPolicy.HMACSHA1_AUTHENTICATION,
                        160 / 8,
                        80 / 8,
                        32 / 8
                );
            case SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80:
                return new SrtpProfileInformation(
                        128 / 8,
                        112 / 8,
                        SrtpPolicy.AESCM_ENCRYPTION,
                        SrtpPolicy.HMACSHA1_AUTHENTICATION,
                        160 / 8,
                        80 / 8,
                        80 / 8
                );
            case SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32:
                return new SrtpProfileInformation(
                        0,
                        0,
                        SrtpPolicy.NULL_ENCRYPTION,
                        SrtpPolicy.HMACSHA1_AUTHENTICATION,
                        160 / 8,
                        80 / 8,
                        32 / 8
                );
            case SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80:
                return new SrtpProfileInformation(
                        0,
                        0,
                        SrtpPolicy.NULL_ENCRYPTION,
                        SrtpPolicy.HMACSHA1_AUTHENTICATION,
                        160 / 8,
                        80 / 8,
                        80 / 8
                );
            case SRTPProtectionProfile.SRTP_AEAD_AES_128_GCM:
                return new SrtpProfileInformation(
                        128 / 8,
                        96 / 8,
                        SrtpPolicy.AESGCM_ENCRYPTION,
                        SrtpPolicy.NULL_AUTHENTICATION,
                        0,
                        128 / 8,
                        128 / 8
                );
            case SRTPProtectionProfile.SRTP_AEAD_AES_256_GCM:
                return new SrtpProfileInformation(
                        256 / 8,
                        96 / 8,
                        SrtpPolicy.AESGCM_ENCRYPTION,
                        SrtpPolicy.NULL_AUTHENTICATION,
                        0,
                        128 / 8,
                        128 / 8
                );
            default:
                throw new IllegalArgumentException("Unsupported SRTP protection profile");
        }
    }
}
