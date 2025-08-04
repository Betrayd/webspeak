package net.betrayd.webspeak.event;

import net.betrayd.webspeak.AudioSource3D;
import net.betrayd.webspeak.PlayerConnection;
import net.betrayd.webspeak.WebSpeakPlayer;

public final class ServerEvents {

    public interface OnPlayerConnected {
        /**
         * Called when a client has connected to a player in the server.
         *
         * @param player     Player that was connected to.
         * @param connection New player's connection. <code>null</code> if the connection was removed without being replaced.
         */
        void onPlayerConnected(WebSpeakPlayer player, PlayerConnection connection);
    }

    public interface OnPlayerDisconnected {
        /**
         * Called when a player client has disconnected.
         *
         * @param player     The player that disconnected.
         * @param connection The (now dead) client connection.
         * @param reason     The reason for disconnect
         */
        void onPlayerDisconnected(WebSpeakPlayer player, PlayerConnection connection, PlayerConnection.DisconnectReason reason);
    }

    public interface OnPlayerAdded {
        /**
         * Called when a player has been added to the server.
         *
         * @param player    Player that was added.
         * @param sessionId The player's session ID.
         * @param audioId   The player's audio ID.
         */
        void onPlayerAdded(WebSpeakPlayer player, String sessionId, String audioId);
    }

    public interface OnPlayerRemoved {
        /**
         * Called when a player has been removed from the server.
         *
         * @param player    Player that was removed.
         * @param sessionId The player's session ID.
         * @param audioId   The player's audio ID.
         */
        void onPlayerRemoved(WebSpeakPlayer player, String sessionId, String audioId);
    }

    public interface OnAudioSourceAdded {
        /**
         * Called when an audio source (including a player) is added to the server.
         *
         * @param audioSource Audio source that was added.
         * @param audioId     The source's audio ID.
         */
        void onAudioSourceAdded(AudioSource3D audioSource, String audioId);
    }

    public interface OnAudioSourceRemoved {
        /**
         * Called when an audio source (including a player) is removed from the server.
         *
         * @param audioSource Audio source that was removed.
         * @param audioId     The source's audio ID.
         */
        void onAudioSourceRemoved(AudioSource3D audioSource, String audioId);
    }

    public final WebSpeakEvent<Runnable> ON_START_TICK = WebSpeakEvent.createNoArg();
    public final WebSpeakEvent<Runnable> ON_END_TICK = WebSpeakEvent.createNoArg();

    public final WebSpeakEvent<OnPlayerConnected> ON_PLAYER_CONNECTED = WebSpeakEvent.createArrayBacked(
            listeners -> (player, connection) -> {
                for (var l : listeners) {
                    l.onPlayerConnected(player, connection);
                }
            }
    );

    public final WebSpeakEvent<OnPlayerDisconnected> ON_PLAYER_DISCONNECTED = WebSpeakEvent.createArrayBacked(
            listeners -> (player, connection, reason) -> {
                for (var l : listeners) {
                    l.onPlayerDisconnected(player, connection, reason);
                }
            }
    );

    public final WebSpeakEvent<OnPlayerAdded> ON_PLAYER_ADDED = WebSpeakEvent.createArrayBacked(
            listeners -> (player, sid, aid) -> {
                for (var l : listeners) {
                    l.onPlayerAdded(player, sid, aid);
                }
            }
    );

    public final WebSpeakEvent<OnPlayerRemoved> ON_PLAYER_REMOVED = WebSpeakEvent.createArrayBacked(
            listeners -> (player, sid, aid) -> {
                for (var l : listeners) {
                    l.onPlayerRemoved(player, sid, aid);
                }
            }
    );

    public final WebSpeakEvent<OnAudioSourceAdded> ON_AUDIO_SOURCE_ADDED = WebSpeakEvent.createArrayBacked(
            listeners -> (source, aid) -> {
                for (var l : listeners) {
                    l.onAudioSourceAdded(source, aid);
                }
            }
    );

    public final WebSpeakEvent<OnAudioSourceRemoved> ON_AUDIO_SOURCE_REMOVED = WebSpeakEvent.createArrayBacked(
            listeners -> (source, aid) -> {
                for (var l : listeners) {
                    l.onAudioSourceRemoved(source, aid);
                }
            }
    );
}
