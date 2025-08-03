package net.betrayd.webspeak.math;

/**
 * A simple, immutable three-dimensional vector.
 */
public record Vec3d(double x, double y, double z) {
    public static final Vec3d ZERO = new Vec3d(0, 0, 0);

    public Vec3d add(double x, double y, double z) {
        return new Vec3d(this.x + x, this.y + y, this.z + z);
    }

    public Vec3d add(Vec3d other) {
        return new Vec3d(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public Vec3d add(double scalar) {
        return new Vec3d(this.x + scalar, this.y + scalar, this.z + scalar);
    }

    public Vec3d subtract(double x, double y, double z) {
        return new Vec3d(this.x - x, this.y - y, this.z - z);
    }

    public Vec3d subtract(Vec3d other) {
        return new Vec3d(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vec3d subtract(double scalar) {
        return new Vec3d(this.x - scalar, this.y - scalar, this.z - scalar);
    }

    public Vec3d mul(double x, double y, double z) {
        return new Vec3d(this.x * x, this.y * y, this.z * z);
    }

    public Vec3d mul(Vec3d other) {
        return new Vec3d(this.x * other.x, this.y * other.y, this.z * other.z);
    }

    public Vec3d mul(double scalar) {
        return new Vec3d(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public Vec3d divide(double x, double y, double z) throws ArithmeticException {
        return new Vec3d(this.x / x, this.y / y, this.z / z);
    }

    public Vec3d divide(Vec3d other) throws ArithmeticException {
        return new Vec3d(this.x / other.x, this.y / other.y, this.z / other.z);
    }

    public Vec3d divide(double scalar) throws ArithmeticException {
        return new Vec3d(this.x / scalar, this.y / scalar, this.z / scalar);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double distanceToSquared(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;

        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceToSquared(Vec3d other) {
        return distanceToSquared(other.x, other.y, other.z);
    }

    public double distanceTo(double x, double y, double z) {
        return Math.sqrt(distanceToSquared(x, y, z));
    }

    public double distanceTo(Vec3d other) {
        return Math.sqrt(distanceToSquared(other.x, other.y, other.z));
    }

    public double dot(double x, double y, double z) {
        return this.x * x + this.y * y + this.z * z;
    }

    public double dot(Vec3d other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public Vec3d cross(double x, double y, double z) {
        double cx = this.y * z - this.z * y;
        double cy = this.z * x - this.x * z;
        double cz = this.x * y - this.y * x;
        return new Vec3d(cx, cy, cz);
    }

    public Vec3d cross(Vec3d other) {
        return cross(other.x, other.y, other.z);
    }

    public Vec3d normalize() throws ArithmeticException {
        double length = length();
        if (length == 0d) {
            throw new ArithmeticException("Cannot normalize a zero-length vector");
        }
        return new Vec3d(x / length, y / length, z / length);
    }
}
