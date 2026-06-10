package net.betrayd.webspeak.webrtc.ice;

import java.net.InetAddress;

/**
 * A record that stores potential candidates.
 * <p>These should always be UDP as nothing else is currently supported</p>
 * @param address the address of the stun ex. stun.l.google.com
 * @param port the port of the stun ex. 19302
 */
public record LocalCandidate(InetAddress address, int port) {
}
