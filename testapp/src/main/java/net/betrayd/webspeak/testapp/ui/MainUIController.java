package net.betrayd.webspeak.testapp.ui;

import javafx.fxml.FXML;
import net.betrayd.webspeak.testapp.Player;
import net.betrayd.webspeak.testapp.WebSpeakTestApp;
import net.betrayd.webspeak.testapp.ui.util.ZoomableGraph;

public class MainUIController {


    @FXML
    private ZoomableGraph graph;

    @FXML
    void initialize() {
        WebSpeakTestApp.getInstance().getAddPlayerEvent().addListener(this::onAddPlayer);
        WebSpeakTestApp.getInstance().getRemovePlayerEvent().addListener(this::onRemovePlayer);

        graph.graphScaleProperty().bindBidirectional(WebSpeakTestApp.getInstance().graphScaleProperty());
    }

    private void onAddPlayer(Player player) {

    }

    private void onRemovePlayer(Player player) {

    }
}
