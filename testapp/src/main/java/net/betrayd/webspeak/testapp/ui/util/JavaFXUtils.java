package net.betrayd.webspeak.testapp.ui.util;

import com.google.common.collect.MapMaker;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class JavaFXUtils {
    private record ColorSizeKey(Color lineColor, Double size) {};

    private static final Map<ColorSizeKey, Image> gridImages = new MapMaker().weakValues().makeMap();

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaFXUtils.class);

    public static ImagePattern createGridPattern(double gridSize, double x, double y, Color lineColor) {
        double w = gridSize;
        double h = gridSize;
        ColorSizeKey key = new ColorSizeKey(lineColor, gridSize);

        Image image = gridImages.computeIfAbsent(key, s -> {
            Canvas canvas = new Canvas(w, h);
            GraphicsContext gc = canvas.getGraphicsContext2D();
    
            gc.setStroke(Color.DARKGRAY);
            // gc.fillRect(0, 0, w, h);
            gc.strokeRect(0, 0, w, h);
            return canvas.snapshot(new SnapshotParameters(), null);
        });

        return new ImagePattern(image, x, y, w, h, false);
    }

    public static ImagePattern createGridPattern(double gridSize, double x, double y) {
        return createGridPattern(gridSize, x, y, Color.DARKGRAY);
    }

    public static IntegerBinding stringToIntBinding(ObservableValue<? extends String> source) {
        return new IntegerBinding() {

            {
                super.bind(source);
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(source);
            }

            @Override
            public void dispose() {
                super.dispose();
                super.unbind(source);
            }

            @Override
            protected int computeValue() {
                try {
                    return Integer.valueOf(source.getValue());
                } catch (Exception e) {
                    LOGGER.warn("NumberFormatException: {}", e.getMessage());
                    return 0;
                }
            }
            
        };
    }
}
