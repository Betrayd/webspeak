package net.betrayd.webspeak.webrtc.ice4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record IceStartData(String ufrag, String password, List<IceCandidateParser.ParsedCandidateSDP> iceCandidates) {
    private static final Pattern ufragPattern = Pattern.compile("^a=ice-ufrag:(.+)$", Pattern.MULTILINE);
    private static final Pattern passwordPattern = Pattern.compile("^a=ice-pwd:(.+)$", Pattern.MULTILINE);
    private static final Pattern iceCandidatePattern = Pattern.compile("^a=candidate:(.+)$", Pattern.MULTILINE);
    public static IceStartData fromSdp(String Sdp) throws IllegalArgumentException {
        String ufrag;
        Matcher matcher = ufragPattern.matcher(Sdp);
        if (matcher.find()){
            ufrag = matcher.group(1).trim();
        }else{
            throw new IllegalArgumentException("Invalid session description SDP");
        }

        String password;
        matcher = passwordPattern.matcher(Sdp);
        if (matcher.find()){
            password = matcher.group(1).trim();
        }else{
            throw new IllegalArgumentException("Invalid session description SDP");
        }

        List<IceCandidateParser.ParsedCandidateSDP> candidates = new ArrayList<>();
        matcher = iceCandidatePattern.matcher(Sdp);

        while (matcher.find()){
            //the substring 2 is just to remove the a=
            String candidateLine = matcher.group().substring(2);

            IceCandidateParser.ParsedCandidateSDP parsed = IceCandidateParser.parse(candidateLine);

            candidates.add(parsed);
        }

        return new IceStartData(ufrag, password, candidates);
    }
}
