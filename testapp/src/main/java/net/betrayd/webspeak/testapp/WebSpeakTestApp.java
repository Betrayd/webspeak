package net.betrayd.webspeak.testapp;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.testapp.ui.MainUIController;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WebSpeakTestApp extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSpeakTestApp.class);

    @Getter
    private static WebSpeakTestApp instance;

    private final Event.Invokable<Player> addPlayerEvent = Event.create();
    private final Event.Invokable<Player> removePlayerEvent = Event.create();

    public Event<Player> getAddPlayerEvent() {
        return addPlayerEvent;
    }

    public Event<Player> getRemovePlayerEvent() {
        return removePlayerEvent;
    }

    private final ObjectProperty<ServerContainer> server = new SimpleObjectProperty<>();

    @Nullable
    public ServerContainer getServer() {
        return server.get();
    }

    public ReadOnlyObjectProperty<ServerContainer> serverProperty() {
        return server;
    }

    @Getter
    private MainUIController mainUIController;

    private final DoubleProperty graphScaleProperty = new SimpleDoubleProperty(32);

    public double getGraphScale() {
        return graphScaleProperty.get();
    }

    public void setGraphScale(double value) {
        graphScaleProperty.set(value);
    }

    public DoubleProperty graphScaleProperty() {
        return graphScaleProperty;
    }

    private final Set<Player> players = new HashSet<>();
    private final Set<Player> playersUnmod = Collections.unmodifiableSet(players);

    public Set<Player> getPlayers() {
        return playersUnmod;
    }

    public boolean addPlayer(Player player) {
        if (players.add(player)) {
            if (isServerRunning()) {
                // TODO: add to server
            }
            addPlayerEvent.invoke(player);
            return true;
        }
        return false;
    }

    public boolean removePlayer(Object player) {
        if (!(player instanceof Player p))
            return false;

        if (players.remove(p)) {
            if (isServerRunning()) {
                // TODO: remove from server
            }
            removePlayerEvent.invoke(p);
            return true;
        }
        return false;
    }

    public boolean isServerRunning() {
        return server.get() != null;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/mainUI.fxml"));
        Parent root = loader.load();
        mainUIController = loader.getController();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
