package net.betrayd.webspeak.webrtc.dtls;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDefaultDigestProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tls.SecurityParameters;
import org.bouncycastle.tls.TlsContext;
import org.bouncycastle.tls.TlsUtils;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.TlsSecret;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.time.Duration;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DtlsUtils {
    public static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    public static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final BcTlsCrypto BC_TLS_CRYPTO = new BcTlsCrypto(SECURE_RANDOM);

    public static CertificateInfo generateCertificateInfo() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException, IOException {
        X500Name cn = generateCN("TODO-APP-NAME", "TODO-APP-VERSION");
        KeyPair keyPair = generateEcKeyPair();
        Certificate x509certificate = generateCertificate(cn, keyPair);
        String localFingerprintHashFunction = DtlsConfig.FINGERPRINT_HASH_FUNCTION;
        String localFingerprint = getFingerprint(x509certificate, localFingerprintHashFunction);

        org.bouncycastle.tls.Certificate certificate = new org.bouncycastle.tls.Certificate(
                new BcTlsCertificate[]{new BcTlsCertificate(BC_TLS_CRYPTO, x509certificate)}
        );
        return new CertificateInfo(
                keyPair,
                certificate,
                localFingerprintHashFunction,
                localFingerprint,
                System.currentTimeMillis()
        );
    }

    /**
     * A helper which finds an SRTP protection profile present in both
     * [ours] and [theirs].  Throws {@link DtlsException} if no common profile is found.
     */
    public static int chooseSrtpProtectionProfile(Iterator<Integer> ours, Iterator<Integer> theirs) throws DtlsException {
        while (ours.hasNext()) {
            int our = ours.next();
            while (theirs.hasNext()) {
                int their = theirs.next();
                if(our == their){
                    return our;
                }
            }
        }
        //should add a log message here
        throw new DtlsException("No common SRTP protection profile found.");
    }

    /*
     * Copied from TlsContext#exportKeyingMaterial and modified to work with
     * an externally provided masterSecret value.
     */
    public static byte[] exportKeyingMaterial(TlsContext context, String asciiLabel, byte[] context_value, int length, TlsSecret masterSecret){
        if (context_value != null && !TlsUtils.isValidUint16(context_value.length)) {
            throw new IllegalArgumentException("'context_value' must have a length less than 2^16 (or be null)");
        }
        SecurityParameters securityParameters = context.getSecurityParameters();
        byte[] clientRandom = securityParameters.getClientRandom();
        byte[] serverRandom = securityParameters.getServerRandom();

        int seedLength = clientRandom.length + serverRandom.length;
        if (context_value != null) {
            seedLength += (2 + context_value.length);
        }

        byte[] seed = new byte[seedLength];
        int seedPos = 0;

        System.arraycopy(clientRandom, 0, seed, seedPos, clientRandom.length);
        seedPos += clientRandom.length;
        System.arraycopy(serverRandom, 0, seed, seedPos, serverRandom.length);
        seedPos += serverRandom.length;

        if (context_value != null) {
            TlsUtils.writeUint16(context_value.length, seed, seedPos);
            seedPos += 2;
            System.arraycopy(context_value, 0, seed, seedPos, context_value.length);
            seedPos += context_value.length;
        }

        if (seedPos != seedLength) {
            throw new IllegalStateException("error in calculation of seed for export");
        }

        return TlsUtils.PRF(securityParameters, masterSecret, asciiLabel, seed, length).extract();
    }

    /**
     * Verifies and validates a specific certificate against the fingerprints
     * presented by the remote endpoint via the signaling path.
     *
     * @param certificate the certificate to be verified and validated against
     * the fingerprints presented by the remote endpoint via the signaling path
     * @throws DtlsException (should be a new DtlsException) if [certificateInfo] fails validation
     */
    public static void verifyAndValidateCertificate(org.bouncycastle.tls.Certificate certificate, Map<String, List<String>> remoteFingerprints) throws IOException, OperatorCreationException, DtlsException {
        if (certificate.isEmpty()){
            throw new IOException("No remote fingerprints.");
        }
        for(TlsCertificate currentCertificate : certificate.getCertificateList()){
            Certificate x509Cert = Certificate.getInstance(currentCertificate.getEncoded());
            verifyAndValidateCertificate(x509Cert, remoteFingerprints);
        }
    }

    /**
     * Verifies and validates a specific certificate against the fingerprints
     * presented by the remote endpoint via the signaling path.
     *
     * @param certificate the certificate to be verified and validated against
     * the fingerprints presented by the remote endpoint via the signaling path.
     * @throws DtlsException if the specified [certificate] failed to verify
     * and validate against the fingerprints presented by the remote endpoint
     * via the signaling path.
     */
    private static void verifyAndValidateCertificate(Certificate certificate, Map<String, List<String>> remoteFingerprints) throws IOException, OperatorCreationException, DtlsException {
        for(String hashFunction : DtlsConfig.getAcceptedFingerprintHashFunctions()){
            List<String> fingerPrints = remoteFingerprints.get(hashFunction);
            if(fingerPrints != null){
                String certificateFingerprint = getFingerprint(certificate, hashFunction);

                if(!fingerPrints.contains(certificateFingerprint)){
                    throw new DtlsException("Certificate fingerprint does not match remote fingerprint.");
                }

                return;
            }
        }
        throw new DtlsException("No fingerprint declared over the signaling path with any of the accepted hash functions: [Certificate]:" + certificate + ", [Fingerprint]: Remote: " + remoteFingerprints + ", Accepted: " + DtlsConfig.getAcceptedFingerprintHashFunctions());
    }

    /**
     * Generate an x509 certificate valid from 1 day ago until 7 days from now.
     *
     * <p>This was a Jitsi-to do: make the algorithm dynamic (passed in) to support older dtls versions/clients</p>
     */
    private static Certificate generateCertificate(X500Name subject, KeyPair keyPair) throws OperatorCreationException {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now - Duration.ofDays(1).toMillis());
        Date expiryDate = new Date(now + Duration.ofDays(7).toMillis());
        BigInteger serialNumber = BigInteger.valueOf(now);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                serialNumber,
                startDate,
                expiryDate,
                subject,
                keyPair.getPublic()
            );
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate());

        return certBuilder.build(signer).toASN1Structure();
    }

    /**
     * Generate an eliptic-curve keypair using the secp256r1 named curve:
     * "All Implementations MUST implement DTLS 1.2 with the
     * TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 cipher suite and the P-256
     * curve"
     *
     * --https://tools.ietf.org/html/draft-ietf-rtcweb-security-arch-18#section-6.5
     *
     * NOTE(brian): I used 'secp256r1' specifically because it's what I saw in wireshark traces from chrome
     */
    private static KeyPair generateEcKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        ECNamedCurveParameterSpec ecCurveSpec = ECNamedCurveTable.getParameterSpec("secp256r1");

        keyGen.initialize(ecCurveSpec);

        return keyGen.generateKeyPair();
    }

    /**
     * Generate an [X500Name] using the given [appName] and [appVersion]
     */
    private static X500Name generateCN(String appName, String appVersion) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        String rdn = "$appName $appVersion";
        builder.addRDN(BCStyle.CN, rdn);
        return builder.build();
    }

    /**
     * Computes the fingerprint of a [org.bouncycastle.asn1.x509.Certificate] using [hashFunction] and returns it
     * as a [String]
     */
    private static String getFingerprint(Certificate certificate, String hashFunction) throws OperatorCreationException, IOException {
        AlgorithmIdentifier digAlgId = DefaultDigestAlgorithmIdentifierFinder.INSTANCE.find(hashFunction.toUpperCase());
        ExtendedDigest digest = BcDefaultDigestProvider.INSTANCE.get(digAlgId);
        byte[] input = certificate.getEncoded(ASN1Encoding.DER);
        byte[] output = new byte[digest.getDigestSize()];

        digest.update(input, 0, input.length);
        digest.doFinal(output, 0);

        return byteArrayToFingerprint(output);
    }

    private static String byteArrayToFingerprint(byte[] input) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < input.length; i++) {
            int octet = (int)input[i];
            int firstIndex = (octet & 0xF0) >>> 4;
            int secondIndex = (octet & 0x0F);
            buf.append(HEX_CHARS[firstIndex]);
            buf.append(HEX_CHARS[secondIndex]);
            if (i < input.length - 1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }
}
