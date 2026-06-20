package net.betrayd.webspeak.webrtc.dtls.old;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;

public class DltsUtils {
    /**
     * Converts a Java X509Certificate into a WebRTC SDP fingerprint.
     * Example output: 0A:1B:2C:3D:4E:5F...
     * @throws RuntimeException if the fingerprint cannot be computed
     */
    public static String computeFingerprint(X509Certificate certificate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(certificate.getEncoded());

            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xFF & hash[i]).toUpperCase();
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
                if (i < hash.length - 1) {
                    hexString.append(":");
                }
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute DTLS certificate fingerprint", e);
        }
    }
}
