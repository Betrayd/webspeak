package net.betrayd.webspeak;

import dev.onvoid.webrtc.media.MediaStreamTrack;
import lombok.Getter;
import lombok.Setter;
import net.betrayd.webspeak.webrtc.PlayerRTCDataChannels;
import net.betrayd.webspeak.webrtc.signaling.BackendSignaling;
import org.jetbrains.annotations.Nullable;

/**
 * A player that contains a connection, can obtain coordinates, etc.
 */
public abstract class WebSpeakPlayer implements AudioSource3D {

    /**
     * The server this player belongs to.
     */
    @Getter
    private final WebSpeakServer server;

    @Nullable
    @Getter
    @Setter
    private BackendSignaling signaling = null;

    @Nullable
    @Getter
    @Setter
    private PlayerRTCDataChannels rtcConnection = null;

   /* @Nullable
    @Getter
    @Setter
    private*/

    public WebSpeakPlayer(WebSpeakServer server)
    {
        this.server = server;
    }

    public final @Nullable String tryGetAudioId() {
        return server.getAudioSources().inverse().get(this);
    }

    public final String getAudioId() {
        var id = tryGetAudioId();
        if (id == null) {
            throw new IllegalStateException("This player is not in a server!");
        }
        return id;
    }

    public final @Nullable String tryGetSessionId() {
        return server.getPlayers().inverse().get(this);
    }

    public final String getSessionId() {
        var id = tryGetSessionId();
        if (id == null) {
            throw new IllegalStateException("This player is not in a server!");
        }
        return id;
    }

    @Override
    public MediaStreamTrack getAudioTrack(){
        //if(rtcConnection != null){
        //    return rtcConnection.micTrack;
        //}
        return null;
    }
}
