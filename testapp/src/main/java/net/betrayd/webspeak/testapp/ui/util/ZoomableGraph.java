package net.betrayd.webspeak.testapp.ui.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class ZoomableGraph extends Region {
    private final Pane pane = new Pane();

    private final Translate translation = new Translate();
    private final Scale scale = new Scale();
    private final Rectangle clipRect = new Rectangle();

    private final Region graph = new Region();

    private final Line xAxis = new Line();
    private final Line yAxis = new Line();

    public ZoomableGraph() {
        pane.getStyleClass().add("grid-background");
        pane.getTransforms().addAll(translation, scale);
        setClip(clipRect);

        getChildren().addAll(graph, xAxis, yAxis, pane);

        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);
        setOnScroll(this::onScroll);

        translation.xProperty().bindBidirectional(xOffsetProperty);
        translation.yProperty().bindBidirectional(yOffsetProperty);

        zoomAmountProperty.addListener((prop, oldVal, newVal) -> {
            double val = calcScaleMultiplier(newVal.doubleValue());
            scale.setX(val);
            scale.setY(val);
        });

        zoomAmountProperty.addListener((prop, oldVal, newVal) -> updateBackground());
        xOffsetProperty.addListener((prop, oldVal, newVal) -> updateBackground());
        yOffsetProperty.addListener((prop, oldVal, newVal) -> updateBackground());

        xAxis.setStroke(Color.BLACK);
        yAxis.setStroke(Color.BLACK);
    }

    private boolean hasInitSize;

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!hasInitSize) {
            setXCenter(0);
            setYCenter(0);
            hasInitSize = true;
        }

        clipRect.setWidth(getWidth());
        clipRect.setHeight(getHeight());
        graph.resizeRelocate(0, 0, getWidth(), getHeight());
        updateBackground();
    }


    private final BooleanProperty allowManualTransform = new SimpleBooleanProperty(true);

    public boolean getAllowManualTransform() {
        return allowManualTransform.get();
    }

    public void setAllowManualTransform(boolean allowManualTransform) {
        this.allowManualTransform.set(allowManualTransform);
    }

    public BooleanProperty allowManualTransformProperty() {
        return allowManualTransform;
    }

    private final DoubleProperty xOffsetProperty = new SimpleDoubleProperty();

    public double getXOffset() {
        return xOffsetProperty.get();
    }

    public void setXOffset(double xOffset) {
        xOffsetProperty.set(xOffset);
    }

    public DoubleProperty xOffsetProperty() {
        return xOffsetProperty;
    }

    public DoubleProperty yOffsetProperty = new SimpleDoubleProperty();

    public double getYOffset() {
        return yOffsetProperty.get();
    }

    public void setYOffset(double yOffset) {
        yOffsetProperty.set(yOffset);
    }

    public DoubleProperty yOffsetProperty() {
        return yOffsetProperty;
    }

    private final DoubleProperty zoomAmountProperty = new SimpleDoubleProperty(0);

    public double getZoomAmount() {
        return zoomAmountProperty.get();
    }

    public void setZoomAmount(double zoomAmount) {
        zoomAmountProperty.set(zoomAmount);
    }

    public DoubleProperty zoomAmountProperty() {
        return zoomAmountProperty;
    }

    private final DoubleProperty graphScaleProperty = new SimpleDoubleProperty(20);

    public double getGraphScale() {
        return graphScaleProperty.get();
    }

    public void setGraphScale(double graphScale) {
        graphScaleProperty.set(graphScale);
    }

    public DoubleProperty graphScaleProperty() {
        return graphScaleProperty;
    }

    public final ObservableList<Node> getGraphChildren() {
        return pane.getChildren();
    }

    private double lastX;
    private double lastY;

    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY) {
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            e.consume();
        }
    }
    
    private void onMouseDragged(MouseEvent e) {
        if (!allowManualTransform.get() || e.getButton() != MouseButton.SECONDARY)
            return;
        double deltaX = e.getSceneX() - lastX;
        double deltaY = e.getSceneY() - lastY;

        lastX = e.getSceneX();
        lastY = e.getSceneY();

        translation.setX(translation.getX() + deltaX);
        translation.setY(translation.getY() + deltaY);
        e.consume();
    }

    private void onScroll(ScrollEvent e) {
        double scrollDelta = e.getDeltaY() * 0.005;
        double oldScale = calcScaleMultiplier(getZoomAmount());

        double globalXOffset = getXCenter() / oldScale;
        double globalYOffset = getYCenter() / oldScale;

        setZoomAmount(getZoomAmount() + scrollDelta);
        
        double newScale = calcScaleMultiplier(getZoomAmount());

        setXCenter(globalXOffset * newScale);
        setYCenter(globalYOffset * newScale);
    }

    public double getXCenter() {
        return getXOffset() - getWidth() / 2;
    }

    public double getYCenter() {
        return getYOffset() - getHeight() / 2;
    }

    private void setXCenter(double xCenter) {
        setXOffset(xCenter + getWidth() / 2);
    }

    private void setYCenter(double yCenter) {
        setYOffset(yCenter + getHeight() / 2);
    }

    private void updateBackground() {
        double size = getGraphScale() * calcScaleMultiplier(getZoomAmount());
        double xOffset = getXOffset();
        double yOffset = getYOffset();


        graph.setBackground(Background.fill(JavaFXUtils.createGridPattern(size, xOffset, yOffset)));

        xAxis.setStartX(0);
        xAxis.setEndX(getWidth());
        xAxis.setStartY(yOffset);
        xAxis.setEndY(yOffset);

        yAxis.setStartY(0);
        yAxis.setEndY(getHeight());
        yAxis.setStartX(xOffset);
        yAxis.setEndX(xOffset);
    }

    private static double calcScaleMultiplier(double zoomAmount) {
        return Math.pow(2, zoomAmount);
    }

}
