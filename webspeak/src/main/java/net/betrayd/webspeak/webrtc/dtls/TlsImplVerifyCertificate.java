package net.betrayd.webspeak.webrtc.dtls;

import org.bouncycastle.tls.Certificate;

import java.io.IOException;

public interface TlsImplVerifyCertificate {
    void accept(Certificate certificate) throws IOException;
}
