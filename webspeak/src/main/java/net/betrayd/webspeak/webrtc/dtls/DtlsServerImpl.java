package net.betrayd.webspeak.webrtc.dtls;

import net.betrayd.webspeak.event.Event;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCrypto;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Hashtable;

public class DtlsServerImpl extends DefaultTlsServer {

    /**
     * I wish this could be volatile but in case the library updates just using an int makes life easier.
     * Gotten from {@link org.bouncycastle.tls.SRTPProtectionProfile}
     */
    private int selectedSrtpProtectionProtocol;

    private final X509Certificate localCertificate;
    private final PrivateKey localPrivateKey;
    private final String localFingerprint;

    private Event.Invokable<Void> handshakeCompeteEvent;

    public DtlsServerImpl(TlsCrypto tlsCrypto, X509Certificate localCertificate, PrivateKey localPrivateKey) {
        super(tlsCrypto);
        this.localCertificate = localCertificate;
        this.localPrivateKey = localPrivateKey;
        this.localFingerprint = DltsUtils.computeFingerprint(localCertificate);
    }

    public Event<Void> onHandshakeComplete(){
        return handshakeCompeteEvent;
    }

    public String getLocalFingerprint() {
        return localFingerprint;
    }

    public byte[] getSrtpKeyingMaterial() {
        // "EXTRACTOR-dtls_srtp" is the magic string defined by RFC 5764
        return this.context.exportKeyingMaterial("EXTRACTOR-dtls_srtp", null, 60);
    }

    @Override
    public TlsCredentials getCredentials() throws IOException {
        try {
            // 1. Verify we are using the JCA bridge
            if (!(getCrypto() instanceof JcaTlsCrypto)) {
                throw new IllegalStateException("TlsCrypto must be JcaTlsCrypto to map java.security keys.");
            }
            JcaTlsCrypto jcaCrypto = (JcaTlsCrypto) getCrypto();

            // 2. Convert standard Java X509Certificate to Bouncy Castle's internal format
            TlsCertificate bcCert = jcaCrypto.createCertificate(localCertificate.getEncoded());

            // 3. Create the Certificate chain. The short (0) specifies CertificateType.x509
            Certificate bcCertificateChain = new Certificate((short) 0, new TlsCertificate[]{bcCert});

            // 4. Specify the signature algorithm.
            // NOTE: WebRTC strongly favors ECDSA. Ensure this matches your generated key type.
            // For standard P-256 WebRTC keys, use ecdsa_secp256r1_sha256.
            // If you generated an RSA key instead, you would need an RSA algorithm.
            SignatureAndHashAlgorithm signatureAlgorithm = new SignatureAndHashAlgorithm(
                    HashAlgorithm.sha256,
                    SignatureAlgorithm.ecdsa
            );;

            // 5. Use the JCA-specific Credentialed Signer which directly accepts java.security.PrivateKey
            return new JcaDefaultTlsCredentialedSigner(
                    new TlsCryptoParameters(context),
                    jcaCrypto,
                    localPrivateKey,
                    bcCertificateChain,
                    signatureAlgorithm
            );

        } catch (Exception e) {
            throw new IOException("Failed to load and format DTLS credentials", e);
        }
    }

    //Read what SRTP profiles the browser supports
    @Override
    public void processClientExtensions(Hashtable clientExtensions) throws IOException {
        super.processClientExtensions(clientExtensions);

        UseSRTPData useSrtp = TlsSRTPUtils.getUseSRTPExtension(clientExtensions);
        if (useSrtp != null) {
            this.selectedSrtpProtectionProtocol = useSrtp.getProtectionProfiles()[0];
        }
    }

    @Override
    public Hashtable getServerExtensions() throws IOException {
        Hashtable extensions = super.getServerExtensions();
        if(extensions == null){
            extensions = new Hashtable();
        }

        int[] profile = new int[] { this.selectedSrtpProtectionProtocol };
        UseSRTPData serverSrtpData = new UseSRTPData(profile, new byte[0]);
        TlsSRTPUtils.addUseSRTPExtension(extensions, serverSrtpData);

        return extensions;
    }

    @Override
    public void notifyHandshakeComplete() throws IOException {
        super.notifyHandshakeComplete();

        handshakeCompeteEvent.invoke(null);
    }
}
