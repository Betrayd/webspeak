package net.betrayd.webspeak.webrtc.dtls;

import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.webrtc.srtp.TlsRole;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.DTLSServerProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.DatagramTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class DtlsServer implements DtlsRole {
    public static final Logger LOGGER = LoggerFactory.getLogger(DtlsServer.class);

    private final DatagramTransport transport;
    private final DTLSServerProtocol dtlsServerProtocol = new DTLSServerProtocol();

    private final Event.Invokable<HandshakeCompleteData> handshakeCompleteEvent = Event.create();
    private final TlsServerImpl tlsServer;

    public DtlsServer(DatagramTransport transport, CertificateInfo certificateInfo, TlsImplVerifyCertificate verifyRemoteCandidates) {
        this.transport = transport;

        tlsServer = new TlsServerImpl(certificateInfo, verifyRemoteCandidates);
    }

    public Event<HandshakeCompleteData> onHandshakeComplete(){
        return handshakeCompleteEvent;
    }

    @Override
    public DTLSTransport start() throws IOException {
        try {
            DTLSTransport returnValue = dtlsServerProtocol.accept(tlsServer, transport);

            handshakeCompleteEvent.invoke(new HandshakeCompleteData(
                    TlsRole.SERVER,
                    tlsServer.getChosenSrtpProtectionProfile(),
                    tlsServer.getSrtpKeyingMaterial()
            ));

            return returnValue;
        }catch (IOException e) {
            LOGGER.error("Error during DTLS connection", e);
            throw e;
        }
    }

    public static record HandshakeCompleteData(TlsRole tlsRole, int profileID, byte[] keyingMaterial){

    }
}
