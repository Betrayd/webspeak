package net.betrayd.webspeak.testapp.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import lombok.Getter;
import net.betrayd.webspeak.testapp.WebSpeakTestApp;

public class PlayerAvatarController {

    @FXML
    private Group root;

    @FXML
    private Group innerRoot;

    @FXML
    private Circle fillCircle;

    @Getter
    private Circle scopeCircle;

    private final BooleanProperty selectedProperty = new SimpleBooleanProperty();

    public boolean isSelected() {
        return selectedProperty.get();
    }

    public void setSelected(boolean selected) {
        selectedProperty.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }

    public Paint getFill() {
        return fillCircle.getFill();
    }

    public void setFill(Paint fill) {
        fillCircle.setFill(fill);
    }

    public ObjectProperty<Paint> fillProperty() {
        return fillCircle.fillProperty();
    }

    public double getRotation() {
        return innerRoot.getRotate();
    }

    private double oldStrokeWidth;
    

    @FXML
    public void initialize() {

        scopeCircle = new Circle();
        scopeCircle.setFill(Color.rgb(0, 127, 255, .125));
        scopeCircle.setStroke(Color.rgb(0, 127, 255));
        scopeCircle.setStrokeWidth(2);

        scopeCircle.setViewOrder(1);
        scopeCircle.visibleProperty().bind(selectedProperty);
        // Marking it disabled stops it from interfering with click events.
        scopeCircle.setDisable(true);

//        var graphScaleProp = WebSpeakTestApp.getInstance().graphScaleProperty();
//        var scopeSizeProp = WebSpeakTestApp.getInstance().getPannerOptionsManager().scopeRadiusProperty();
//        scopeCircle.radiusProperty().bind(Bindings.multiply(graphScaleProp, scopeSizeProp));

        root.getChildren().add(scopeCircle);

        root.viewOrderProperty().bind(Bindings.createIntegerBinding(() -> selectedProperty.get() ? -1: 0, selectedProperty));

        // I really should be using CSS for this, but this is a test app so I don't care.
        //seems wrong but I had to move it here from the function for no infinite growth
        oldStrokeWidth = fillCircle.getStrokeWidth();
        selectedProperty.addListener((prop, oldVal, newVal) -> {
            if (oldVal.equals(newVal))
                return;
            if (newVal) {
                fillCircle.setStroke(Color.BLUE);
                fillCircle.setStrokeWidth(oldStrokeWidth * 4);
            } else {
                fillCircle.setStroke(Color.BLACK);
                fillCircle.setStrokeWidth(oldStrokeWidth);
            }
        });
        
    }

//    public void initPlayer(Player player) {
//
//    }

    private double mouseAnchorX;
    private double mouseAnchorY;


    @FXML
    private void onMousePressed(MouseEvent e) {

        if (e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.SECONDARY) {
            mouseAnchorX = e.getX();
            mouseAnchorY = e.getY();
            prevX = mouseAnchorX;
            prevY = mouseAnchorY;

            e.consume();
        }
    }

    double prevX;
    double prevY;

    @FXML
    private void onMouseDragged(MouseEvent e) {

        if (e.getButton() == MouseButton.PRIMARY) {

            root.setLayoutX(e.getX() - mouseAnchorX + root.getLayoutX());
            root.setLayoutY(e.getY() - mouseAnchorY + root.getLayoutY());
            e.consume();
        } else if (e.getButton() == MouseButton.SECONDARY) {
            double rot = innerRoot.getRotate();
            rot += e.getX() - prevX;
            rot += e.getY() - prevY;
            prevX = e.getX();
            prevY = e.getY();
            innerRoot.setRotate(rot);

            e.consume();
        }
    }

    public Group getRoot() {
        return root;
    }
}
