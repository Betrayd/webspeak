package net.betrayd.webspeak.testapp;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.Getter;
import net.betrayd.webspeak.testapp.ui.MainUIController;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSpeakTestApp extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSpeakTestApp.class);

    @Getter
    private static WebSpeakTestApp instance;

    private final ObjectProperty<ServerContainer> server = new SimpleObjectProperty<>();

    @Nullable
    public ServerContainer getServer() {
        return server.get();
    }

    public ReadOnlyObjectProperty<ServerContainer> serverProperty() {
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

    @Getter
    private MainUIController mainUIController;

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
