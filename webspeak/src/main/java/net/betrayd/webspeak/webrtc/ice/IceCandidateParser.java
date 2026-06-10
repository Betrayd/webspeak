package net.betrayd.webspeak.webrtc.ice;

public class IceCandidateParser {

    public static ParsedCandidateSDP parse(String sdp) throws IllegalArgumentException {
        if (sdp == null || !sdp.startsWith("candidate:")) {
            throw new IllegalArgumentException("Invalid ICE candidate SDP");
        }

        String[] tokens = sdp.split(" ");

        if(tokens.length < 8){
            throw new IllegalArgumentException("Invalid ICE candidate SDP");
        }
        String foundation = tokens[0].substring("candidate:".length());

        int component = Integer.parseInt(tokens[1]);
        String protocol = tokens[2];
        long priority = Long.parseLong(tokens[3]);
        String ip = tokens[4];
        int port = Integer.parseInt(tokens[5]);
        String type = tokens[7];

        return new ParsedCandidateSDP(foundation, component, protocol, priority, ip, port, type);
    }

    /**
     * A parsed ice candidate
     * @param foundation
     * @param component
     * @param protocol
     * @param priority
     * @param ip
     * @param port
     * @param type
     */
    public static record ParsedCandidateSDP(String foundation, int component, String protocol, long priority, String ip, int port, String type) {}
}
