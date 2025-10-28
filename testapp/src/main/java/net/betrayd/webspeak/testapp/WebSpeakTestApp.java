package net.betrayd.webspeak.testapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.testapp.ui.MainUIController;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class WebSpeakTestApp extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSpeakTestApp.class);

    @Getter
    private static WebSpeakTestApp instance;

    //TODO: this is stupid since we already have these events on the server, but I just want to get it running for now so I'm not screwing with it
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

    public void addPlayer(Player player) {
        if (players.add(player)) {
            if (isServerRunning()) {
                TestWebPlayer webPlayer = new TestWebPlayer(server.get().getServer(), player);
                server.get().getServer().addPlayer(webPlayer)
                        .thenAcceptAsync((a) -> addPlayerEvent.invoke(player), Platform::runLater);
            }
        }
    }

    public boolean removePlayer(Object player) {
        if (!(player instanceof Player p))
            return false;

        if (players.remove(p)) {
            if (isServerRunning()) {
                if(p.webPlayerProperty.get() != null){
                    server.get().getServer().removePlayer(p.webPlayerProperty.get(), "Removed from server");
                }
            }
            removePlayerEvent.invoke(p);
            return true;
        }
        return false;
    }

    public boolean isServerRunning() {
        return server.get() != null;// && server.get().getServer() != null; && server.get().getServer() != null && server.get().getServer().isRunning();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/mainUI.fxml"));
        Parent root = loader.load();
        mainUIController = loader.getController();

        mainUIController.initApp(this);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public CompletableFuture<?> stopServer() {
        LOGGER.info("Stopping server");
        if (!isServerRunning()) {
            LOGGER.error("Server is not running!");
        } else if (server.get().getServer() != null) {
            WebSpeakServer pastServer = server.get().getServer();
            server.set(null);
            return pastServer.stop();
        } else {
            LOGGER.error("No server in server container");
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<WebSpeakServer> startServer() {
        LOGGER.info("Starting server");
        if (isServerRunning()) {
            LOGGER.error("Server is already running!");
            return null;
        }
        server.set(new ServerContainer());
        CompletableFuture<WebSpeakServer> returnValue = server.get().start();
        returnValue.thenAccept(server -> {
            LOGGER.info("Started server successfully");
            mainUIController.onStartServer(server);
        }).exceptionally(e -> {
            LOGGER.error("Server could not start", e);
            server.set(null);
            return null;
        });
        return returnValue;
    }
}
