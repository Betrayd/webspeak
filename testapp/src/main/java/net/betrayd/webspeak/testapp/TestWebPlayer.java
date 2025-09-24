package net.betrayd.webspeak.testapp;

import javafx.geometry.Point2D;
import lombok.Getter;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.math.Vec3d;
import net.betrayd.webspeak.testapp.util.MathUtils;

public class TestWebPlayer extends WebSpeakPlayer {

    @Getter
    private final Player player;

    public TestWebPlayer(WebSpeakServer server, Player player) {
        super(server);
        this.player = player;
        player.webPlayerProperty.set(this);
    }

    @Override
    public Vec3d getLocation() {
        return player.getLocation();
    }

    @Override
    public Vec3d getForward() {
        Point2D point = MathUtils.rotatePoint(0, 1, Math.toRadians(player.getRotation() + 180d));
        return new Vec3d(point.getX(), 0, point.getY());
    }
}
