package net.betrayd.webspeak.v1;

public class WebSpeakFlags {
    public final record WebSpeakFlag<T>(String name, T defaultValue) {
    };

    /**
     * Log to console whenever a client sends an RTC offer.
     */
    public static final WebSpeakFlag<Boolean> DEBUG_RTC_OFFERS = new WebSpeakFlag<>("debugRTCOffers", false);

    /**
     * Log to console when the server asks a client to connect to another client.
     */
    public static final WebSpeakFlag<Boolean> DEBUG_CONNECTION_REQUESTS = new WebSpeakFlag<>("debugConnectionRequests", false);

    /**
     * Log to console whenever a client sends a keepalive packet.
     */
    public static final WebSpeakFlag<Boolean> DEBUG_KEEPALIVE = new WebSpeakFlag<>("deugKeepAlive", false);

    /**
     * Log to console whenever a player swaps channels.
     */
    public static final WebSpeakFlag<Boolean> DEBUG_CHANNEL_SWAPS = new WebSpeakFlag<>("debugChannelSwaps", false);
}
