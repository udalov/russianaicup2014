import model.Unit;

import static java.lang.StrictMath.sqrt;

public class Point {
    public final double x;
    public final double y;

    private Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @NotNull
    public static Point of(@NotNull Unit unit) {
        return new Point(unit.getX(), unit.getY());
    }

    @NotNull
    public static Point of(double x, double y) {
        return new Point(x, y);
    }

    @NotNull
    public Point shift(@NotNull Vec vector) {
        return new Point(this.x + vector.x, this.y + vector.y);
    }

    public double distance(double x0, double y0) {
        return sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0));
    }

    public double distance(@NotNull Point other) {
        return distance(other.x, other.y);
    }

    public double distance(@NotNull Unit other) {
        return distance(other.getX(), other.getY());
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f)", x, y);
    }
}
