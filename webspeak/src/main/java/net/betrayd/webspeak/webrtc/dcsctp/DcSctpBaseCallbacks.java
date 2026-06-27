package net.betrayd.webspeak.webrtc.dcsctp;

import net.betrayd.webspeak.webrtc.utils.TaskPools;
import org.jetbrains.annotations.Nullable;
import org.jitsi.dcsctp4j.DcSctpSocketCallbacks;
import org.jitsi.dcsctp4j.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Contains methods we can usefully implement for every JVB socket
 */
public abstract class DcSctpBaseCallbacks implements DcSctpSocketCallbacks {
    public static final Logger LOGGER = LoggerFactory.getLogger(DcSctpBaseCallbacks.class);

    private final DcSctpTransport transport;
    public DcSctpBaseCallbacks(DcSctpTransport transport) {
        this.transport = transport;
    }

    public Timeout createTimeout(DcSctpSocketCallbacks.DelayPrecision p0) {
        return new ATimeout(transport);
    }

    public void OnConnectionRestarted() {
        LOGGER.info("Surprising SCTP callback: connection restarted");
    }

    public void OnStreamsResetFailed(short[] outgoingStreams, String reason) {
        LOGGER.info(
                "Surprising SCTP callback: streams ${outgoingStreams.joinToString()} reset failed: $reason"
        );
    }

    @Override
    public void OnStreamsResetPerformed(short[] outgoingStreams) {
        // This is normal following a call to close(), which is a hard-close (as opposed to shutdown() which is
        // soft-close)
        LOGGER.info("Outgoing streams ${outgoingStreams.joinToString()} reset");
    }

    @Override
    public void OnIncomingStreamsReset(short[] incomingStreams) {
        /* Does Chrome ever reset streams? */
        LOGGER.info("Surprising SCTP callback: incoming streams {} reset", incomingStreams);
    }

    private static class ATimeout implements Timeout {
        public final WeakReference<DcSctpTransport> transport;
        private long timeoutId;
        @Nullable
        private ScheduledFuture<?> scheduledFuture = null;
        @Nullable
        private Future<?> future = null;
        ATimeout(DcSctpTransport transport) {
            this.transport = new WeakReference<>(transport);
        }

        @Override
        public void start(long duration, long timeoutId) {
            try {
                this.timeoutId = timeoutId;

                scheduledFuture = TaskPools.SCHEDULED_POOL.schedule(() -> {
                    future = TaskPools.IO_POOL.submit(() -> {
                        DcSctpTransport transport = this.transport.get();
                        if(transport != null) {
                            transport.handleTimeout(timeoutId);
                        }
                    });
                }, duration, TimeUnit.MILLISECONDS);
            }
            catch(Exception e) {
                LOGGER.error("Exception scheduling DCSCTP timeout", e);
            }
        }

        @Override
        public void stop() {
            if(scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            if(future != null) {
                future.cancel(false);
            }
            scheduledFuture = null;
            future = null;
        }
    }
}
