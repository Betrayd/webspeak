package net.betrayd.webspeaktest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.betrayd.webspeak.v1.WebSpeakChannel;
import net.betrayd.webspeak.v1.WebSpeakGroup;
import net.betrayd.webspeak.v1.util.AudioModifier;
import net.betrayd.webspeaktest.ui.MainUIController;

public class WebSpeakTestApp extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSpeakTestApp.class);

    private static WebSpeakTestApp instance;

    public static WebSpeakTestApp getInstance() {
        return instance;
    }
    
    private final PannerOptionsManager pannerOptionsManager = new PannerOptionsManager(this);

    public PannerOptionsManager getPannerOptionsManager() {
        return pannerOptionsManager;
    }

    private final ObjectProperty<WebSpeakTestServer> server = new SimpleObjectProperty<>(null);

    public WebSpeakTestServer getServer() {
        return server.get();
    }

    public ReadOnlyObjectProperty<WebSpeakTestServer> serverProperty() {
        return server;
    }

    private final DoubleProperty graphScaleProperty = new SimpleDoubleProperty(32);

    public double getGraphScale() {
        return graphScaleProperty.get();
    }

    public void setGraphScale(double graphScale) {
        graphScaleProperty.set(graphScale);
    }

    public DoubleProperty graphScaleProperty() {
        return graphScaleProperty;
    }


    private final ObservableMap<Player, String> connectionIps = FXCollections.observableHashMap();

    public ObservableMap<Player, String> getConnectionIps() {
        return connectionIps;
    }


    private MainUIController mainUIController;

    private final Set<Player> players = new HashSet<>();

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public boolean addPlayer(Player player) {
        if (players.add(player)) {
            if (isServerRunning()) {
                addPlayerToServer(getServer(), player);
            }
            mainUIController.onAddPlayer(player);
            return true;
        } else {
            return false;
        }
    }

    public boolean removePlayer(Object player) {
        if (players.remove(player)) {
            if (player instanceof Player castPlayer) {
                if (isServerRunning()) {
                    TestWebPlayer webPlayer = castPlayer.getWebPlayer();
                    if (webPlayer != null) {
                        // Technically removePlayer is already thread-safe, but no reason to wait on a
                        // lock here.
                        getServer().execute(() -> {
                            getServer().getWebSpeakServer().removePlayer(webPlayer);
                        });
                    }
                }
                mainUIController.onRemovePlayer(castPlayer);
            }
            return true;
        } else {
            return false;
        }
    }

    private final ObservableList<WebSpeakChannel> channels = FXCollections.observableArrayList();

    public ObservableList<WebSpeakChannel> getChannels() {
        return channels;
    }
    

    private final List<WebSpeakGroup> globalGroups = new ArrayList<>();
    
    /**
     * Get a list of groups that will appear in the players' info panels. Shouldn't
     * be modified after app starts.
     */
    public List<WebSpeakGroup> getGlobalGroups() {
        return globalGroups;
    }

    private void setupGroups() {
        var survival = new WebSpeakGroup("Survival Mode");
        var spectator = new WebSpeakGroup("Spectator");
        var admins = new WebSpeakGroup("Admins");

        survival.setAudioModifier(spectator, new AudioModifier(true, null));
        spectator.setAudioModifier(spectator, new AudioModifier(null, false));
        admins.setAudioModifier(spectator, new AudioModifier(false, null));

        globalGroups.add(survival);
        globalGroups.add(spectator);
        globalGroups.add(admins);
    }

    public static final WebSpeakChannel DEFAULT_CHANNEL = new WebSpeakChannel("Default");

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        
        channels.add(DEFAULT_CHANNEL);
        channels.add(new WebSpeakChannel("Channel 1"));
        channels.add(new WebSpeakChannel("Channel 2"));

        setupGroups();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/mainUI.fxml"));
        Parent root = loader.load();
        mainUIController = loader.getController();

        mainUIController.initApp(this);

        Scene scene = new Scene(root);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public boolean startServer(int port) {
        LOGGER.info("Starting server");
        if (server.get() != null) {
            LOGGER.error("Server is already running!");
            return false;
        }
        connectionIps.clear();
        try {
            //TODO: make this also not hardcoded
            WebSpeakTestServer webServer = new WebSpeakTestServer(port, false);
            server.set(webServer);
            
            // Add all players to server
            for (var player : players) {
                addPlayerToServer(webServer, player);
            }
            
            var future = webServer.awaitStart();

            future.thenAcceptAsync(server -> {
                for (var channel : getChannels()) {
                    server.addChannel(channel);
                }
            }, Platform::runLater);
            
            future.thenAccept(server -> {
                // server.getPannerOptions().maxDistance = scopeRadiusProperty.get();
                getPannerOptionsManager().applyAllPannerOptions(server);
                server.onSessionConnected(player -> {
                    if (player instanceof TestWebPlayer testPlayer) {
                        String connectionIp = testPlayer.getConnection().getRemoteAddress();
                        Platform.runLater(() -> connectionIps.put(testPlayer.getPlayer(), connectionIp));
                    }
                });
                server.onSessionDisconnected(player -> Platform.runLater(() -> {
                    if (player instanceof TestWebPlayer testPlayer) {
                        connectionIps.remove(testPlayer.getPlayer());
                    }
                }));
            });
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Exception starting server: ", e);
            return false;
        }
    }

    private void addPlayerToServer(WebSpeakTestServer server, Player player) {
        CompletableFuture.supplyAsync(() -> server.getWebSpeakServer()
                .createPlayer((s, id, session) -> new TestWebPlayer(s, player, id, session)), server)
                .thenAcceptAsync(p -> player.setWebPlayer(p), Platform::runLater);
    }


    public CompletableFuture<Void> stopServer() {
        if (!isServerRunning()) {
            LOGGER.error("Server is not running!");
            return CompletableFuture.completedFuture(null);
        }
        
        return server.get().shutdownAsync().thenRunAsync(() -> {
            connectionIps.clear();
            for (var player : players) {
                player.setWebPlayer(null);
            }
            server.set(null);
        }, Platform::runLater).exceptionally(e -> {
            LOGGER.error("Error clearing players: ", e);
            return null;
        });
    }

    public boolean isServerRunning() {
        return server.get() != null;
    }

    public int getServerPort() {
        WebSpeakTestServer server = this.server.get();
        if (server == null) {
            return 0;
        }
        return server.getWebSpeakServer().getPort();
    }

    @Override
    public void stop() throws Exception {
        if (server.get() != null) {
            try {
                server.get().shutdownAndWait();
            } catch (Exception e) {
                LOGGER.error("Error stopping server", e);
            }
        }

        super.stop();
    }
}
