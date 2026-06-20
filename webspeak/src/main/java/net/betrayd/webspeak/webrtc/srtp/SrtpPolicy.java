package net.betrayd.webspeak.webrtc.srtp;

public class SrtpPolicy {
    /**
     * Null Cipher, does not change the content of RTP payload
     */
    public final static int NULL_ENCRYPTION = 0;

    /**
     * Counter Mode AES Cipher, defined in Section 4.1.1, RFC3711
     */
    public final static int AESCM_ENCRYPTION = 1;

    /**
     * Galois/Counter Mode AES Cipher, defined in RFC 7714
     */
    public final static int AESGCM_ENCRYPTION = 5;

    /**
     * Counter Mode TwoFish Cipher
     */
    public final static int TWOFISH_ENCRYPTION = 3;

    /**
     * F8 mode AES Cipher, defined in Section 4.1.2, RFC 3711
     */
    public final static int AESF8_ENCRYPTION = 2;

    /**
     * F8 Mode TwoFish Cipher
     */
    public final static int TWOFISHF8_ENCRYPTION = 4;

    /**
     * Null Authentication, no authentication
     *
     * This should be set if GCM or other AEAD encryption is used.
     */
    public final static int NULL_AUTHENTICATION = 0;

    /**
     * HMAC SHA1 Authentication, defined in Section 4.2.1, RFC3711
     */
    public final static int HMACSHA1_AUTHENTICATION = 1;

    /**
     * Skein Authentication
     */
    public final static int SKEIN_AUTHENTICATION = 2;
}
