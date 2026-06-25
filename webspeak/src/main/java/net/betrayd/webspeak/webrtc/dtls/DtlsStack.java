//again translated from jitsi-videobridge
package net.betrayd.webspeak.webrtc.dtls;

import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.webrtc.Buffer;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.DTLSTransport;
import org.jitsi.utils.concurrent.ArrayBlockingQueueWithShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DtlsStack {
    static {
        //this makes it so we only block the main thread when first starting (so we are guaranteed a return value)
        getGlobalCertificateInfo();
    }
    public static final Logger LOGGER = LoggerFactory.getLogger(DtlsStack.class);

    public static final int QUEUE_SIZE = 50;

    private static volatile CertificateInfo globalCertificateInfo = null;

    private static final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    /**
     * Because generating the certificateInfo can be expensive, we generate a single
     * one to be used everywhere which expires in 24 hours (when we'll generate
     * another one).
     */
    public static CertificateInfo getGlobalCertificateInfo() throws RuntimeException {
        if(globalCertificateInfo == null) {
            generateSetGlobalWithTryCatch();
            return globalCertificateInfo;
        }

        //all this isn't really needed anywhere where we are planning on using webspeak but might as well add it.
        long experationPeriodMs = Duration.ofDays(1).toMillis();
        if(globalCertificateInfo.creationTimestampMs() + experationPeriodMs < System.currentTimeMillis()) {
            //there is a to do here in jitsi since it's bad to spin a new independent thread like this. I added a patch so multiple quick calls don't all spin up new threads to do the same expensive computation.
            if(!refreshInProgress.get()){
                Thread thread = new Thread(() -> {
                    refreshInProgress.set(true);
                    generateSetGlobalWithTryCatch();
                    refreshInProgress.set(false);
                });

                thread.start();
            }
        }
        return globalCertificateInfo;
    }

    private static void generateSetGlobalWithTryCatch() throws RuntimeException {
        try{
            globalCertificateInfo = DtlsUtils.generateCertificateInfo();
        }catch (Exception e){
            throw new RuntimeException("Could not generate certificate info", e);
        }
    }

    private final ArrayBlockingQueueWithShutdown<ByteBuffer> incomingProtocolData = new ArrayBlockingQueueWithShutdown<>(QUEUE_SIZE, true);
    private final DatagramTransportImpl datagramTransport;
    private final CountDownLatch roleIsSet = new CountDownLatch(1);
    /**
     * A buffer we'll use to receive data from [dtlsTransport].
     */
    private final byte[] dtlsAppDataBuf = new byte[1500];
    private final Event.Invokable<DtlsServer.HandshakeCompleteData> handshakeCompleteDataEvent = Event.create();
    private final Event.Invokable<Buffer> incomingProtocolDataRecievedEvent = Event.create();

    private DTLSTransport dtlsTransport = null;

    private CertificateInfo certificateInfo;

    private DtlsRole role;

    private Map<String, List<String>> remoteFingerprints = Map.of();

    public DtlsStack() throws RuntimeException {
        certificateInfo = getGlobalCertificateInfo();
        datagramTransport = new DatagramTransportImpl(incomingProtocolData);
    }

    public Event<DtlsServer.HandshakeCompleteData> onHandshakeComplete(){
        return handshakeCompleteDataEvent;
    }

    public Event<Buffer> onIncomingProtocolDataRecieved(){return incomingProtocolDataRecievedEvent;}

    public DtlsRole getRole(){
        return role;
    }

    public String getLocalFingerprintHashFunction(){
        return certificateInfo.localFingerprintHashFunction();
    }

    public String getLocalFingerprint(){
        return certificateInfo.LocalFingerPrint();
    }

    public void setOutgoingDataHandler(Consumer<Buffer> outgoingDataHandler){
        this.datagramTransport.setOutgoingDataHandler(outgoingDataHandler);
    }

    public void setRemoteFingerprints(Map<String, List<String>> remoteFingerprints){
        this.remoteFingerprints = remoteFingerprints;
    }

    public void actAsServer(){
        LOGGER.info("DTLS role selected: server/passive");
        DtlsServer dtlsServer = new DtlsServer(
                datagramTransport,
                certificateInfo,
                certificate -> {
                    try {
                        verifyAndValidateRemoteCandidates(certificate);
                    } catch (DtlsException | OperatorCreationException e) {
                        throw new IOException(e);
                    }
                }
        );
        dtlsServer.onHandshakeComplete().addListener(handshakeCompleteDataEvent::invoke);
        role = dtlsServer;

        roleIsSet.countDown();
    }

    public void actAsClient(){
        LOGGER.debug("DTLS role selected: client/active");
        role = new DtlsClient(
                datagramTransport,
                certificateInfo,
                certificate -> {
                    try {
                        verifyAndValidateRemoteCandidates(certificate);
                    } catch (DtlsException | OperatorCreationException e) {
                        throw new IOException(e);
                    }
                }
        );
        roleIsSet.countDown();
    }

    /**
     * 'start' this stack, in whatever role it has been told to operate (client or server).  If a role
     * has not yet been yet (via {@link #actAsServer()} or [actAsClient (not yet implemented)]), then it will block until the role
     * has been set.
     */
    public void start() throws InterruptedException, IOException {
        LOGGER.debug("Starting DTLS stack with role={}", role != null ? role.getClass().getSimpleName() : "null");
        roleIsSet.await();

        if(role != null){
            dtlsTransport = role.start();
        }

        LOGGER.debug("DTLS transport established");

        // There is a bit of a race here: It's technically possible the
        // far side could finish the handshake and send a message before
        // this side assigns dtlsTransport here.  If so, that message
        // would be passed to #processIncomingProtocolData and put in
        // incomingProtocolData, but, since dtlsTransport won't be set
        // yet, we won't 'receive' it yet.  Check for any incoming packets
        // here, to handle this case.
        processIncomingProtocolData();
    }

    public void close() {
        try {
            datagramTransport.close();
        }
        catch (IOException e) {
            LOGGER.error("Error closing DatagramTransport", e);
        }

        incomingProtocolData.shutdown();
        /*incomingProtocolData.forEach {
            //again this is not implemented in Jitsi-videobidge yet anyway
            //BufferPool.returnBuffer(it.array());
        }*/
        incomingProtocolData.clear();
    }

    public void sendApplicationData(byte[] data, int offset, int length) throws IOException {
        if(dtlsTransport != null){
            dtlsTransport.send(data, offset, length);
        }
    }

    public void processIncomingProtocolData() {
        int bytesReceived = -1;
        byte[] bufferCopy = null;
        do {
            synchronized (dtlsAppDataBuf) {
                if(dtlsTransport != null){
                    try{
                        bytesReceived = dtlsTransport.receive(dtlsAppDataBuf, 0, 1500, 1);
                    }catch (IOException ignored){

                    }
                    //in jitsi this is done in the BufferPool. Moving on.
                    if (bytesReceived > 0) {
                        bufferCopy = new byte[bytesReceived];
                        System.arraycopy(dtlsAppDataBuf, 0, bufferCopy, 0, bytesReceived);
                    }
                }
            }
            if(bufferCopy != null){
                incomingProtocolDataRecievedEvent.invoke(new Buffer(bufferCopy, 0, bytesReceived));
            }
        } while(bytesReceived > 0);
    }

    /**
     * We get 'pushed' the data from a lower transport layer, but bouncycastle wants to 'pull' the data
     * itself.  To mimic this, we put the received data into a queue, and then 'pull' it through ourselves by
     * calling 'receive' on the negotiated [DTLSTransport].
     *
     * Note: the data we get here may be a DTLS protocol packet and therefore won't generate anything
     * to be received by [dtlsTransport] or a DTLS app packet (data sent over DTLS after the handshake has
     * completed) and will result in data being received through [dtlsTransport].  It's possible, though,
     * that the handshake has finished and the far end has sent application data but we've not yet set
     * [dtlsTransport], so we "miss" it.  We don't lose this data, but it will sit inside of [incomingProtocolData]
     * until the next packet comes through.  This means that we must make a copy of the buffer we receive, as we
     * won't necessarily be done with it by the time this method completes.
     */
    public void processIncomingProtocolData(byte[] data, int offset, int length) {
        byte[] bufferCopy = new byte[length];
        System.arraycopy(data, offset, bufferCopy, 0, length);

        if(!incomingProtocolData.offer(ByteBuffer.wrap(bufferCopy, 0, length))){
            if (!incomingProtocolData.isShutdown()) {
                LOGGER.warn("DTLS stack queue full, dropping packet");
            }
        }
    }

    private void verifyAndValidateRemoteCandidates(Certificate remoteCertificate) throws IOException, OperatorCreationException, DtlsException {
        if(remoteCertificate != null){
            DtlsUtils.verifyAndValidateCertificate(remoteCertificate, remoteFingerprints);
            LOGGER.debug("Remote DTLS certificate verified successfully");
            return;
        }

        throw new DtlsException("Remote certificate was null");
    }
}
