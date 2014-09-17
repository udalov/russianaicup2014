import model.Unit;

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
    public Point shiftX(double x) {
        return new Point(this.x + x, y);
    }

    @NotNull
    public Point shiftY(double y) {
        return new Point(x, this.y + y);
    }

    @NotNull
    public Point shift(double x, double y) {
        return new Point(this.x + x, this.y + y);
    }

    @NotNull
    public Point transpose(@NotNull Point center) {
        return new Point(2 * center.x - x, 2 * center.y - y);
    }

    public double sqrDist(double x0, double y0) {
        return (x - x0) * (x - x0) + (y - y0) * (y - y0);
    }

    public double sqrDist(@NotNull Unit unit) {
        return sqrDist(unit.getX(), unit.getY());
    }

    @Override
    public int hashCode() {
        return Double.valueOf(x).hashCode() * 31 + Double.valueOf(y).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Point && ((Point) obj).x == x && ((Point) obj).y == y;
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f)", x, y);
    }
}
