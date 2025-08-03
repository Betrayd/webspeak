package net.betrayd.webspeaktest;

import java.io.IOException;
import java.net.URL;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import net.betrayd.webspeak.v1.WebSpeakChannel;
import net.betrayd.webspeak.v1.WebSpeakPlayer;
import net.betrayd.webspeak.v1.util.WebSpeakVector;
import net.betrayd.webspeaktest.ui.PlayerAvatarController;

public class Player {

    private static final URL AVATAR_URL = Player.class.getResource("/ui/avatar.fxml");

    public static PlayerAvatarController loadAvatar() {
        FXMLLoader fxmlLoader = new FXMLLoader(AVATAR_URL);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fxmlLoader.getController();
        
    }

    public static final Player create(Color color) {
        PlayerAvatarController controller = loadAvatar();
        Player player = new Player(controller, color);
        controller.initPlayer(player);
        controller.fillProperty().bind(player.colorProperty());
        return player;
    }
    

    private final PlayerAvatarController avatar;

    
    public Player(PlayerAvatarController avatar, Color color) {
        this.avatar = avatar;
        this.colorProperty.set(color);
    }
    
    public PlayerAvatarController getAvatar() {
        return avatar;
    }

    public Node getNode() {
        return avatar.getRoot();
    }

    private final ObjectProperty<Color> colorProperty = new SimpleObjectProperty<>(Color.RED);

    public Color getColor() {
        return colorProperty.get();
    }

    public void setColor(Color color) {
        colorProperty.set(color);
    }

    public ObjectProperty<Color> colorProperty() {
        return colorProperty;
    }

    private final StringProperty nameProperty = new SimpleStringProperty("player");

    public String getName() {
        return nameProperty.get();
    }

    public void setName(String name) {
        this.nameProperty.set(name);
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    {
        nameProperty.addListener((prop, oldVal, newVal) -> onUpdateName(newVal));
    }

    private void onUpdateName(String name) {
        WebSpeakPlayer webPlayer = getWebPlayer();
        if (webPlayer != null) {
            webPlayer.setPlayerListEntry(webPlayer.getPlayerListEntry().withName(name));
        }
    }

    private final ObjectProperty<TestWebPlayer> webPlayerProperty = new SimpleObjectProperty<>();

    public TestWebPlayer getWebPlayer() {
        return webPlayerProperty.get();
    }

    public void setWebPlayer(TestWebPlayer webPlayer) {
        if (webPlayer != null && webPlayer.getPlayer() != this)
            throw new IllegalArgumentException("Web player must point to this player.");
        webPlayerProperty.set(webPlayer);
        if (webPlayer != null) {
            webPlayer.setPlayerListEntry(webPlayer.getPlayerListEntry().withName(getName()));
        }
    }

    public ReadOnlyObjectProperty<TestWebPlayer> webPlayerProperty() {
        return webPlayerProperty;
    }

    private final SimpleObjectProperty<WebSpeakChannel> channelProperty = new SimpleObjectProperty<>(WebSpeakTestApp.DEFAULT_CHANNEL);

    public WebSpeakChannel getChannel() {
        return channelProperty.get();
    }

    public void setChannel(WebSpeakChannel channel) {
        channelProperty.set(channel);
    }

    public SimpleObjectProperty<WebSpeakChannel> channelProperty() {
        return channelProperty;
    }

    {
        channelProperty.addListener((prop, oldVal, newVal) -> {
            var webPlayer = getWebPlayer();
            if (webPlayer != null) {
                webPlayer.getServer().execute(() -> webPlayer.setChannel(newVal));
            }
        });
        webPlayerProperty.addListener((prop, oldVal, newVal) -> {
            if (newVal != null) {
                var channel = getChannel();
                newVal.getServer().execute(() -> newVal.setChannel(channel));
            }
        });
    }

    public WebSpeakVector getLocation() {
        double x = getNode().getLayoutX();
        double y = getNode().getLayoutY();

        double graphScale = WebSpeakTestApp.getInstance().getGraphScale();
        x /= graphScale;
        y /= graphScale;

        // By default WebSpeak uses a right-hand Cartesian coordinate space. Therefore, a top-down view needs to be converted.
        // (this is why Z-up makes more sense IMO)
        return new WebSpeakVector(x, 0, y);
    }

    public double getRotation() {
        // For some reason, JavaFX decided clockwise is positive, although that's mathematically incorrect.
        return -avatar.getRotation();
    }
}
