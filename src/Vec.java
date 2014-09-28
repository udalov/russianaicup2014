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
    public static Vec of(@NotNull Point from, @NotNull Point to) {
        return new Vec(to.x - from.x, to.y - from.y);
    }

    @NotNull
    public static Vec of(@NotNull Unit from, @NotNull Unit to) {
        return new Vec(to.getX() - from.getX(), to.getY() - from.getY());
    }

    @NotNull
    public static Vec of(double angle) {
        return new Vec(cos(angle), sin(angle));
    }

    @NotNull
    public static Vec velocity(@NotNull Unit unit) {
        return new Vec(unit.getSpeedX(), unit.getSpeedY());
    }

    @NotNull
    public static Vec direction(@NotNull Unit unit) {
        return of(unit.getAngle());
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

    public double length() {
        return hypot(x, y);
    }

    @NotNull
    public Vec normalize() {
        return abs(length()) < 1e-9 ? this : divide(length());
    }

    @NotNull
    public Vec project(@NotNull Vec other) {
        return other.normalize().multiply(projection(other));
    }

    public double projection(@NotNull Vec other) {
        return this.innerProduct(other) / other.length();
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
        double d = angle() - other.angle();
        if (d > PI) d -= 2 * PI;
        if (d < -PI) d += 2 * PI;
        return d;
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f)", x, y);
    }
}
