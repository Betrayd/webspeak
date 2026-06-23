package net.betrayd.webspeak.webrtc.dtls;

import net.betrayd.webspeak.webrtc.srtp.SrtpConfig;
import net.betrayd.webspeak.webrtc.srtp.SrtpProfileInformation;
import net.betrayd.webspeak.webrtc.srtp.SrtpUtil;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.HexFormat;
import java.util.List;

public class TlsClientImpl extends DefaultTlsClient {
    public static final Logger LOGGER = LoggerFactory.getLogger(TlsClientImpl.class);
    private final CertificateInfo certificateInfo;
    private final TlsImplVerifyCertificate verifyRemoteCertificate;

    @Nullable
    private TlsCredentials clientCredentials = null;

    @Nullable
    private TlsSession session = null;
    private byte[] srtpKeyingMaterial = null;
    private int chosenSrtpProtectionProfile = 0;

    public TlsClientImpl(CertificateInfo certificateInfo, TlsImplVerifyCertificate verifyRemoteCertificate) {
        super(DtlsUtils.BC_TLS_CRYPTO);
        this.certificateInfo = certificateInfo;
        this.verifyRemoteCertificate = verifyRemoteCertificate;
    }

    public int getChosenSrtpProtectionProfile() {
        return chosenSrtpProtectionProfile;
    }

    public byte[] getSrtpKeyingMaterial() {
        return srtpKeyingMaterial;
    }

    @Override
    public TlsAuthentication getAuthentication() throws IOException {
        return new TlsAuthentication() {

            @Override
            public void notifyServerCertificate(TlsServerCertificate tlsServerCertificate) throws IOException {
                verifyRemoteCertificate.accept(tlsServerCertificate.getCertificate());
            }

            @Override
            public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
                // NOTE: can't set clientCredentials when it is declared because 'context' won't be set yet
                if(clientCredentials == null) {
                    SignatureAndHashAlgorithm toUse = null;
                    if(TlsUtils.isSignatureAlgorithmsExtensionAllowed(context.getServerVersion())){
                        toUse = new SignatureAndHashAlgorithm(
                                HashAlgorithm.sha256,
                                SignatureAlgorithm.ecdsa
                        );
                    }
                    clientCredentials = new BcDefaultTlsCredentialedSigner(
                            new TlsCryptoParameters(context),
                            (BcTlsCrypto) context,
                            PrivateKeyFactory.createKey(certificateInfo.keyPair().getPrivate().getEncoded()),
                            certificateInfo.certificate(),
                            toUse
                    );
                }
                return clientCredentials;
            }
        };
    }

    @Override
    public Hashtable getClientExtensions() throws IOException {
        Hashtable extensions = super.getClientExtensions();
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
    public void processServerExtensions(Hashtable serverExtensions) throws IOException{
        super.processServerExtensions(serverExtensions);

        UseSRTPData useSRTPData = TlsSRTPUtils.getUseSRTPExtension(serverExtensions);
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
    public int getHandshakeTimeoutMillis(){
        return (int)DtlsConfig.getTimeout().toMillis();
    }

    @Override
    public void notifyHandshakeComplete() throws IOException {
        super.notifyHandshakeComplete();
        LOGGER.info("Negotiated DTLS version {}", context.getSecurityParameters().getNegotiatedVersion());
        TlsSession newSession = context.getResumableSession();
        //logging
        if (newSession != null) {
            byte[] newSessionBytes = newSession.getSessionID();
            if (newSessionBytes != null) {
                String newSessionID = HexFormat.of().formatHex(newSessionBytes);

                if (session != null) {
                    byte[] oldSessionBytes = session.getSessionID();
                    if (oldSessionBytes != null && Arrays.equals(oldSessionBytes, newSessionBytes)) {
                        LOGGER.info("Resumed DTLS session {}", newSessionID);
                    } else {
                        LOGGER.info("Established DTLS session {}", newSessionID);
                    }
                }
            }
        }
        SrtpProfileInformation srtpProfileInformation = SrtpUtil.getSrtpProfileInformationFromSrtpProtectionProfile(chosenSrtpProtectionProfile);
        srtpKeyingMaterial = context.exportKeyingMaterial(ExporterLabel.dtls_srtp,
                null,
                2 * (srtpProfileInformation.cipherKeyLength() + srtpProfileInformation.cipherSaltLength())
        );
    }

    @Override
    public ProtocolVersion[] getSupportedVersions() {
        return new ProtocolVersion[]{ProtocolVersion.DTLSv12};
    }

    @Override
    public void notifyAlertRaised(short alertLevel, short alertDescription, String message, Throwable var4) {
        LOGGER.warn("DTLS alert raised: level={}, description={}, message={}", alertLevel, alertDescription, message, var4);
    }

    @Override
    public void notifyAlertReceived(short alertLevel, short alertDescription){
        LOGGER.warn("DTLS alert raised: level={}, description={}", alertLevel, alertDescription);
    }
}
