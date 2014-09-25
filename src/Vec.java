import model.Unit;

import static java.lang.StrictMath.*;

public class Vec {
    public final double x;
    public final double y;

    private Vec(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @NotNull
    public static Vec of(double x, double y) {
        return new Vec(x, y);
    }

    @NotNull
    public static Vec speedOf(@NotNull Unit unit) {
        return new Vec(unit.getSpeedX(), unit.getSpeedY());
    }

    @NotNull
    public Vec plus(@NotNull Vec other) {
        return new Vec(x + other.x, y + other.y);
    }

    @NotNull
    public Vec minus(@NotNull Vec other) {
        return new Vec(x - other.x, y - other.y);
    }

    @NotNull
    public Vec multiply(double k) {
        return new Vec(x * k, y * k);
    }

    @NotNull
    public Vec divide(double k) {
        return new Vec(x / k, y / k);
    }

    public double sqr() {
        return x * x + y * y;
    }

    public double length() {
        return sqrt(x * x + y * y);
    }

    @NotNull
    public Vec normalize() {
        return abs(length()) < 1e-9 ? this : divide(length());
    }

    @NotNull
    public Vec project(@NotNull Vec other) {
        return other.normalize().multiply(this.innerProduct(other) / other.length());
    }

    public double angle() {
        return atan2(y, x);
    }

    public double innerProduct(@NotNull Vec other) {
        return x * other.x + y * other.y;
    }

    public double crossProduct(@NotNull Vec other) {
        return x * other.y - y * other.x;
    }

    public double angleTo(@NotNull Vec other) {
        // TODO: what the hell is this, I don't even...
        double d = angle() - other.angle();
        return atan2(sin(d), cos(d));
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f)", x, y);
    }
}
