package net.betrayd.webspeak.webrtc.dtls;

import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.webrtc.srtp.TlsRole;
import org.bouncycastle.tls.DTLSClientProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.DatagramTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DtlsClient implements DtlsRole{
    public static final Logger LOGGER = LoggerFactory.getLogger(DtlsClient.class);

    private final DatagramTransport transport;
    private final DTLSClientProtocol dtlsClientProtocol = new DTLSClientProtocol();

    private final Event.Invokable<DtlsServer.HandshakeCompleteData> handshakeCompleteEvent = Event.create();
    private final TlsClientImpl tlsClient;

    public DtlsClient(DatagramTransport transport, CertificateInfo certificateInfo, TlsImplVerifyCertificate verifyRemoteCandidates){
        this.transport = transport;

        this.tlsClient = new TlsClientImpl(certificateInfo, verifyRemoteCandidates);
    }

    @Override
    public DTLSTransport start() throws IOException {
        try {
            DTLSTransport returnValue = dtlsClientProtocol.connect(tlsClient, transport);

            handshakeCompleteEvent.invoke(new DtlsServer.HandshakeCompleteData(
                    TlsRole.CLIENT,
                    tlsClient.getChosenSrtpProtectionProfile(),
                    tlsClient.getSrtpKeyingMaterial()
            ));

            return returnValue;
        }
        catch (IOException e) {
            LOGGER.error("Error during DTLS connection", e);
            throw e;
        }
    }
}
