package net.betrayd.webspeak.webrtc.tracks;

public class DataChannelTrack implements RTCTrack {

    private final String id;
    private final String label;

    public DataChannelTrack(String id, String label) {
        this.id = id;
        this.label = label;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public TrackType getType() {
        return TrackType.DATA;
    }

    // Method to handle incoming messages from the client
    public void onMessageReceived(String message) {
        // Handle incoming data channel string
    }

    // Method to handle incoming binary data
    public void onMessageReceived(byte[] data) {
        // Handle incoming data channel binary
    }
}
