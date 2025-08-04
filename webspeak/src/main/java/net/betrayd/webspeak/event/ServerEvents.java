package net.betrayd.webspeak.event;

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

}
