package net.betrayd.webspeak.testapp;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import lombok.Getter;
import lombok.Setter;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.math.Vec3d;
import net.betrayd.webspeak.testapp.ui.PlayerAvatarController;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;

public class Player {
    private static final URL AVATAR_URL = Player.class.getResource("/ui/avatar.fxml");

    public static Player create(Color color) {
        FXMLLoader loader = new FXMLLoader(AVATAR_URL);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PlayerAvatarController controller = loader.getController();
        Player returnPlayer = new Player(controller);
        returnPlayer.colorProperty.set(color);
        controller.initialize();
        controller.fillProperty().bind(returnPlayer.colorProperty);
        return returnPlayer;
    }

    public final ObjectProperty<Color> colorProperty = new SimpleObjectProperty<>(Color.RED);
    public final StringProperty nameProperty = new SimpleStringProperty("player");

    public final ObjectProperty<TestWebPlayer> webPlayerProperty = new SimpleObjectProperty<>();

    @Getter
    private final PlayerAvatarController avatarController;

    public Player(PlayerAvatarController avatarController) {
        this.avatarController = avatarController;
    }

    public final PlayerAvatarController getAvatar() {
        return avatarController;
    }

    public final Node getNode() {return avatarController.getRoot();}

    public Vec3d getLocation() {
        double x = getNode().getLayoutX();
        double y = getNode().getLayoutY();

        double graphScale = WebSpeakTestApp.getInstance().getGraphScale();
        x /= graphScale;
        y /= graphScale;

        // By default WebSpeak uses a right-hand Cartesian coordinate space. Therefore, a top-down view needs to be converted.
        // (this is why Z-up makes more sense IMO)
        return new Vec3d(x, 0, y);
    }

    public double getRotation() {
        // For some reason, JavaFX decided clockwise is positive, although that's mathematically incorrect.
        return -getAvatarController().getRotation();
    }
}
