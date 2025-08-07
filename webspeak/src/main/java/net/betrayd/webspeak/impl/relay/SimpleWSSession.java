package net.betrayd.webspeak.impl.relay;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;

import java.io.Closeable;

/**
 * A simplified implementation of Session designed for use with MultiConnection
 */
public interface SimpleWSSession extends Closeable {

    /**
     * <p>Initiates the asynchronous send of a TEXT message, notifying
     * the given callback when the message send is completed, either
     * successfully or with a failure.</p>
     *
     * @param text     the message text to send
     * @param callback callback to notify when the send operation is complete
     */
    void sendText(String text, Callback callback);


    /**
     * <p>Equivalent to {@code close(StatusCode.NORMAL, null, Callback.NOOP)}.</p>
     *
     * @see #close(int, String, Callback)
     * @see #disconnect()
     */
    @Override
    default void close() {
        close(StatusCode.NORMAL, null, Callback.NOOP);
    }

    /**
     * <p>Sends a websocket CLOSE frame, with status code and reason, notifying
     * the given callback when the frame send is completed, either successfully
     * or with a failure.</p>
     *
     * @param statusCode the status code
     * @param reason     the (optional) reason
     * @param callback   callback to notify when the send operation is complete
     * @see StatusCode
     * @see #close()
     * @see #disconnect()
     */
    void close(int statusCode, String reason, Callback callback);

    /**
     * <p>Abruptly closes the WebSocket connection without sending a CLOSE frame.</p>
     *
     * @see #close(int, String, Callback)
     */
    void disconnect();

    /**
     * @return whether the session is open
     */
    boolean isOpen();

    /**
     * @return whether the underlying socket is using a secure transport
     */
    boolean isSecure();

    interface Listener {

        /**
         * <p>A WebSocket TEXT message has been received.</p>
         *
         * @param message the text payload
         */
        default void onWebSocketText(String message) {
        }

        /**
         * <p>A WebSocket error has occurred during the processing of WebSocket frames.</p>
         * <p>Usually errors occurs from bad or malformed incoming packets, for example
         * text frames that do not contain UTF-8 bytes, frames that are too big, or other
         * violations of the WebSocket specification.</p>
         * <p>The WebSocket {@link Session} will be closed, but applications may
         * explicitly {@link Session#close(int, String, Callback) close} the
         * {@link Session} providing a different status code or reason.</p>
         *
         * @param cause the error that occurred
         */
        default void onWebSocketError(Throwable cause) {
        }

        /**
         * <p>The WebSocket {@link Session} has been closed.</p>
         *
         * @param statusCode the close {@link StatusCode status code}
         * @param reason     the optional reason for the close
         */
        default void onWebSocketClose(int statusCode, String reason) {
        }

    }
}
