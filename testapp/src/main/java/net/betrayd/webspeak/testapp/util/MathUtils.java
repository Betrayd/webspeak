package net.betrayd.webspeak.testapp.util;

import javafx.geometry.Point2D;

public class MathUtils {
    public static Point2D rotatePoint(double x, double y, double theta) {
        double sinTheta = Math.sin(theta);
        double cosTheta = Math.cos(theta);

        return new Point2D(
                x * cosTheta - y * sinTheta,
                x * sinTheta + y * cosTheta);
    }
}
