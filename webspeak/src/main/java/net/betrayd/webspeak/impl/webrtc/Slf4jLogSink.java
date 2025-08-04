package net.betrayd.webspeak.impl.webrtc;

import dev.onvoid.webrtc.logging.LogSink;
import dev.onvoid.webrtc.logging.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLogSink implements LogSink {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebRTC");


    @Override
    public void onLogMessage(Logging.Severity severity, String s) {
        switch (severity) {
            case VERBOSE -> LOGGER.trace(s);
            case INFO -> LOGGER.info(s);
            case WARNING -> LOGGER.warn(s);
            case ERROR -> LOGGER.error(s);
        }
    }
}
