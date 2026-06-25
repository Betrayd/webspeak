//Also translated from jitsi. Everything in DTLS is basically
package net.betrayd.webspeak.webrtc.dtls;

import net.betrayd.webspeak.webrtc.srtp.SrtpConfig;
import net.betrayd.webspeak.webrtc.srtp.SrtpProfileInformation;
import net.betrayd.webspeak.webrtc.srtp.SrtpUtil;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.TlsSecret;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedDecryptor;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class TlsServerImpl extends DefaultTlsServer {
    public static final Logger LOGGER = LoggerFactory.getLogger(TlsServerImpl.class);
    private final CertificateInfo certificateInfo;
    private final TlsImplVerifyCertificate verifyRemoteCertificate;

    @Nullable
    private TlsSession session = null;
    private byte[] srtpKeyingMaterial = null;
    private int chosenSrtpProtectionProfile = 0;

    public TlsServerImpl(CertificateInfo certificateInfo, TlsImplVerifyCertificate verifyRemoteCertificate) {
        super(DtlsUtils.BC_TLS_CRYPTO);
        this.certificateInfo = certificateInfo;
        this.verifyRemoteCertificate = verifyRemoteCertificate;
        LOGGER.debug("DTLS server handshake beginning");
    }

    public int getChosenSrtpProtectionProfile() {
        return chosenSrtpProtectionProfile;
    }

    public byte[] getSrtpKeyingMaterial() {
        return srtpKeyingMaterial;
    }

    @Override
    public Hashtable getServerExtensions() throws IOException {
        Hashtable extensions = super.getServerExtensions();
        if(extensions == null){
            extensions = new Hashtable<>();
        }

        if (TlsSRTPUtils.getUseSRTPExtension(extensions) == null) {
            TlsSRTPUtils.addUseSRTPExtension(
                    extensions,
                    new UseSRTPData(
                            new int[]{chosenSrtpProtectionProfile},
                            TlsUtils.EMPTY_BYTES
                    )
            );
        }

        return extensions;
    }

    @Override
    public void processClientExtensions(Hashtable clientExtensions) throws IOException {
        super.processClientExtensions(clientExtensions);

        UseSRTPData useSRTPData = TlsSRTPUtils.getUseSRTPExtension(clientExtensions);
        int[] protectionProfiles = useSRTPData.getProtectionProfiles();

        try {
            chosenSrtpProtectionProfile = DtlsUtils.chooseSrtpProtectionProfile(SrtpConfig.getProtectionProfiles().iterator(), Arrays.stream(protectionProfiles).boxed().iterator());
        } catch (DtlsException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int[] getCipherSuites(){
        List<Integer> listCiphers = DtlsConfig.getSupportedCipherSuites();
        int[] arrayCiphers = new int[listCiphers.size()];
        for(int i = 0; i < listCiphers.size(); i++){
            arrayCiphers[i] = listCiphers.get(i);
        }

        return arrayCiphers;
    }

    @Override
    public TlsCredentialedDecryptor getRSAEncryptionCredentials() throws IOException {
        return new BcDefaultTlsCredentialedDecryptor(
                (BcTlsCrypto) context.getCrypto(),
                certificateInfo.certificate(),
                PrivateKeyFactory.createKey(certificateInfo.keyPair().getPrivate().getEncoded())
        );
    }

    @Override
    public TlsCredentialedSigner getECDSASignerCredentials() throws IOException {
        return new BcDefaultTlsCredentialedSigner(
                new TlsCryptoParameters(context),
                (BcTlsCrypto) context.getCrypto(),
                PrivateKeyFactory.createKey(certificateInfo.keyPair().getPrivate().getEncoded()),
                certificateInfo.certificate(),
                new SignatureAndHashAlgorithm(HashAlgorithm.sha256, SignatureAlgorithm.ecdsa)
        );
    }

    @Override
    public CertificateRequest getCertificateRequest() throws IOException {
        Vector<SignatureAndHashAlgorithm> signatureAlgorithms = new Vector<>(1);
        signatureAlgorithms.add(new SignatureAndHashAlgorithm(HashAlgorithm.sha256, SignatureAlgorithm.ecdsa));
        return new CertificateRequest(new short[]{ClientCertificateType.ecdsa_sign}, signatureAlgorithms, null);
    }

    @Override
    public int getHandshakeTimeoutMillis(){
        return (int)DtlsConfig.getTimeout().toMillis();
    }

    @Override
    public void notifyHandshakeComplete() throws IOException {
        super.notifyHandshakeComplete();
        LOGGER.info("Negotiated DTLS version {}", context.getSecurityParameters().getNegotiatedVersion());
        TlsSession newSession = context.getResumableSession();
        //logging
        if(newSession != null){
            byte[] newSessionBytes = newSession.getSessionID();
            if(newSessionBytes != null){
                String newSessionID = HexFormat.of().formatHex(newSessionBytes);

                if(session != null){
                    byte[] oldSessionBytes = session.getSessionID();
                    if(oldSessionBytes != null && Arrays.equals(oldSessionBytes, newSessionBytes)){
                        LOGGER.info("Resumed DTLS session {}", newSessionID);
                    }else{
                        LOGGER.info("Established DTLS session {}", newSessionID);
                    }
                }
            }
        }

        SrtpProfileInformation srtpProfileInformation = SrtpUtil.getSrtpProfileInformationFromSrtpProtectionProfile(chosenSrtpProtectionProfile);
        if (!context.getSecurityParameters().isExtendedMasterSecret()) {
            TlsSession session = context.getSession();
            if(session != null){
                SessionParameters sessionParameters = session.exportSessionParameters();
                if(sessionParameters != null){
                    TlsSecret secret = sessionParameters.getMasterSecret();
                    if(secret != null){
                        srtpKeyingMaterial = DtlsUtils.exportKeyingMaterial(
                                context,
                                ExporterLabel.dtls_srtp,
                                null,
                                2 * (srtpProfileInformation.cipherKeyLength() + srtpProfileInformation.cipherSaltLength()),
                                secret
                        );
                    }
                }
            }
        } else {
            srtpKeyingMaterial = context.exportKeyingMaterial(
                    ExporterLabel.dtls_srtp,
                    null,
                    2 * (srtpProfileInformation.cipherKeyLength() + srtpProfileInformation.cipherSaltLength())
            );
        }
    }

    @Override
    public void notifyClientCertificate(Certificate clientCertificate) throws IOException {
        verifyRemoteCertificate.accept(clientCertificate);
    }

    @Override
    public void notifyAlertRaised(short alertLevel, short alertDescription, String message, Throwable var4) {
        LOGGER.warn("DTLS alert raised: level={}, description={}, message={}", alertLevel, alertDescription, message, var4);
    }

    @Override
    public void notifyAlertReceived(short alertLevel, short alertDescription){
        LOGGER.warn("DTLS alert raised: level={}, description={}", alertLevel, alertDescription);
    }

    @Override
    public ProtocolVersion[] getSupportedVersions() {
        return new ProtocolVersion[]{ProtocolVersion.DTLSv12};
    }
}
