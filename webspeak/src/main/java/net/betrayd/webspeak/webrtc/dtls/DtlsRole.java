package net.betrayd.webspeak.webrtc.dtls;

import org.bouncycastle.tls.DTLSTransport;

import java.io.IOException;

public interface DtlsRole {
    /**
     * 'Starts' a DTLS connection.  Blocks until the connection
     * has been successfully established (an exception is thrown
     * upon failure).  Returns the resulting [DTLSTransport].
     *
     * The definition of 'start' varies between roles.  For a client
     * this means 'connect' to a remote server, for a server it means
     * 'accept' and wait for an incoming connection.
     */
    DTLSTransport start() throws IOException;
}
