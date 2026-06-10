package net.betrayd.webspeak.webrtc.ice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Data taken from remote session description. Used to start the IceTransport
 * @param ufrag
 * @param password
 * @param iceCandidates
 */
public record IceStartData(String ufrag, String password, List<IceCandidateParser.ParsedCandidateSDP> iceCandidates) {
    public static IceStartData fromSdp(String sdp) throws IllegalArgumentException {
        if (sdp == null || sdp.isBlank()) {
            throw new IllegalArgumentException("SDP cannot be null or empty");
        }

        String ufrag = null;
        String password = null;
        List<IceCandidateParser.ParsedCandidateSDP> candidates = new ArrayList<>();

        // StringReader wraps the raw string, BufferedReader handles the CRLF splits
        try (BufferedReader reader = new BufferedReader(new StringReader(sdp))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Strips trailing \r or whitespace

                if (line.trim().startsWith("a=ice-ufrag:")) {
                    ufrag = line.substring(12).trim();
                }
                else if(line.trim().startsWith("a=ice-pwd:")) {
                    password = line.substring(10).trim();
                }
                else if (line.startsWith("a=candidate:")){
                    String candidate = line.substring(2);
                    IceCandidateParser.ParsedCandidateSDP parsedCandidate = IceCandidateParser.parse(candidate);
                    candidates.add(parsedCandidate);
                }
                //parse other data here, move out of this record. Create a record that holds an IceStartData and the other data
            }
        }
        catch(IOException e){
            throw new IllegalArgumentException("Error processing SDP string", e);
        }

        if (ufrag == null) {
            throw new IllegalArgumentException("Invalid session description SDP: missing ufrag");
        }
        if (password == null) {
            throw new IllegalArgumentException("Invalid session description SDP: missing password");
        }

        return new IceStartData(ufrag, password, candidates);
    }
}
