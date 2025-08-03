package net.betrayd.webspeak.v1.util;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A simple vector class for use in WebSpeak
 */
@JsonAdapter(WebSpeakVectorJson.class)
public record WebSpeakVector(double x, double y, double z) {
    public static WebSpeakVector fromArray(double[] array) {
        return new WebSpeakVector(array[0], array[1], array[2]);
    }

    public static final WebSpeakVector ZERO = new WebSpeakVector(0, 0, 0);

    public WebSpeakVector add(double x, double y, double z) {
        return new WebSpeakVector(this.x + x, this.y + y, this.z + z);
    }

    public WebSpeakVector add(WebSpeakVector other) {
        return new WebSpeakVector(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public WebSpeakVector subtract(double x, double y, double z) {
        return new WebSpeakVector(this.x - x, this.y - y, this.z - z);
    }

    public WebSpeakVector subtract(WebSpeakVector other) {
        return new WebSpeakVector(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public WebSpeakVector multiply(double x, double y, double z) {
        return new WebSpeakVector(this.x * x, this.y * y, this.z * z);
    }

    public WebSpeakVector multiply(WebSpeakVector other) {
        return new WebSpeakVector(this.x * other.x, this.y * other.y, this.z * other.z);
    }

    public WebSpeakVector multiply(double scalar) {
        return new WebSpeakVector(x * scalar, y * scalar, z * scalar);
    }

    public WebSpeakVector divide(double x, double y, double z) throws ArithmeticException {
        return new WebSpeakVector(this.x / x, this.y / y, this.z / z);
    }

    public WebSpeakVector divide(WebSpeakVector other) throws ArithmeticException {
        return new WebSpeakVector(this.x / other.x, this.y / other.y, this.z / other.z);
    }
    
    public WebSpeakVector divide(double scalar) throws ArithmeticException {
        return new WebSpeakVector(x / scalar, y / scalar, z / scalar);
    }

    public WebSpeakVector invert() {
        return multiply(-1);
    }

    public double lengthSquared() {
        return x * x + y * y + z *z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double squaredDistanceTo(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;

        return dx * dx + dy * dy + dz * dz;
    }

    public double squaredDistanceTo(WebSpeakVector other) {
        return squaredDistanceTo(other.x, other.y, other.z);
    }

    public double distanceTo(double x, double y, double z) {
        return Math.sqrt(squaredDistanceTo(x, y, z));
    }

    public double distanceTo(WebSpeakVector other) {
        return Math.sqrt(squaredDistanceTo(other.x, other.y, other.z));
    }

    public WebSpeakVector normalize() {
        double length = length();
        double invLength = 1 / length;

        return new WebSpeakVector(x * invLength, y * invLength, z * invLength);
    }

    public WebSpeakVector cross(double x, double y, double z) {
        return new WebSpeakVector(
                this.x * z - this.z * y,
                this.z * x - this.x * z,
                this.x * y - this.y * x);
    }

    public WebSpeakVector cross(WebSpeakVector other) {
        return cross(other.x, other.y, other.z);
    }

    public double dot(double x, double y, double z) {
        return this.x * x + this.y * y + this.z * z;
    }

    public double dot(WebSpeakVector other) {
        return dot(other.x, other.y, other.z);
    }
}

class WebSpeakVectorJson extends TypeAdapter<WebSpeakVector> {

    @Override
    public void write(JsonWriter out, WebSpeakVector value) throws IOException {
        out.beginArray();
        out.value(value.x());
        out.value(value.y());
        out.value(value.z());
        out.endArray();
    }

    @Override
    public WebSpeakVector read(JsonReader in) throws IOException {
        in.beginArray();
        double x = in.nextDouble();
        double y = in.nextDouble();
        double z = in.nextDouble();
        in.endArray();
        return new WebSpeakVector(x, y, z);
    }
    
}