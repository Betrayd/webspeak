package net.betrayd.webspeak.testapp.ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.testapp.Player;
import net.betrayd.webspeak.testapp.WebSpeakTestApp;
import net.betrayd.webspeak.testapp.ui.util.ZoomableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.WeakHashMap;

public class MainUIController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainUIController.class);

    private WebSpeakTestApp app;

    private static final Color ON_COLOR = Color.GREEN;
    private static final Color OFF_COLOR = Color.RED;

    private static final String ON_TEXT = "Server Running";
    private static final String OFF_TEXT = "Server Stopped";

    @FXML
    private ZoomableGraph graph;

    @FXML
    private VBox playerBox;

    @FXML
    private Shape serverStatusIcon;

    @FXML
    private Label serverStatusText;

    @FXML
    private TextField connectionAddressField;

    @FXML
    private Button startStopButton;

    public final ObjectProperty<Player> selectedPlayerProperty = new SimpleObjectProperty<>();

    private final Map<Player, PlayerInfoController> playerInfoControllers = new WeakHashMap<>();

    @FXML
    void initialize() {
        WebSpeakTestApp.getInstance().getAddPlayerEvent().addListener(this::onAddPlayer);
        WebSpeakTestApp.getInstance().getRemovePlayerEvent().addListener(this::onRemovePlayer);

        graph.graphScaleProperty().bindBidirectional(WebSpeakTestApp.getInstance().graphScaleProperty());
    }

    public void initApp(WebSpeakTestApp app) {
        this.app = app;
    }

    protected void onStopServer() {
        Platform.runLater(() -> {
            serverStatusIcon.setFill(OFF_COLOR);
            serverStatusText.setText(OFF_TEXT);
            //serverAddressProperty.set("");

            startStopButton.setText("Start Server");
            startStopButton.setDisable(false);
            connectionAddressField.setPromptText("Start server to see connection address.");
        });
    }

    public void onStartServer(WebSpeakServer server) {
        Platform.runLater(() -> {
            serverStatusIcon.setFill(ON_COLOR);
            serverStatusText.setText(ON_TEXT);

            startStopButton.setText("Stop Server");
            startStopButton.setDisable(false);

            /*CompletableFuture.supplyAsync(() -> server.getWsConnectionUrl(), server)
                .thenAcceptAsync(serverAddressProperty::set, Platform::runLater);*/

            connectionAddressField.setPromptText("Please select a player.");

            server.getOnStop().addListener(s -> onStopServer());
        });

    }

    @FXML
    private void pressStartStopButton(){
        if(app.isServerRunning()) {
            startStopButton.setDisable(true);
            app.stopServer();
        } else {
            app.startServer();
        }
    }

    private void onAddPlayer(Player player) {
        var infoPanel = PlayerInfoController.loadInstance();
        infoPanel.initPlayer(player);
        infoPanel.getTitledPane().addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            selectedPlayerProperty.set(player);
        });

        infoPanel.ON_REQUEST_REMOVE.addListener(v -> {
            removePlayer(player);
        });

        Node node = player.getNode();

        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            selectedPlayerProperty.set(player);
            node.requestFocus();
            e.consume();
        });

        graph.getGraphChildren().add(player.getNode());


        playerBox.getChildren().add(infoPanel.getTitledPane());

        BooleanBinding selectedBinding = Bindings.createBooleanBinding(
                () -> player.equals(selectedPlayerProperty.get()), selectedPlayerProperty);

        infoPanel.selectedProperty().bind(selectedBinding);
        player.getAvatar().selectedProperty().bind(selectedBinding);

        playerInfoControllers.put(player, infoPanel);
    }

    private void removePlayer(Player player) {
        app.removePlayer(player);
    }

    private void onRemovePlayer(Player player) {
        graph.getGraphChildren().remove(player.getNode());
        PlayerInfoController infoPanel = playerInfoControllers.remove(player);
        if (infoPanel != null) {
            playerBox.getChildren().remove(infoPanel.getTitledPane());
            infoPanel.onPlayerRemoved();
        }
    }

    @FXML
    private void addPlayer() {
        app.addPlayer(Player.create(Color.color(Math.random(), Math.random(), Math.random())));
    }
}
