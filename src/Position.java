import model.Unit;

public abstract class Position {
    public final Point point;
    public final Vec velocity;

    public Position(@NotNull Point point, @NotNull Vec velocity) {
        this.point = point;
        this.velocity = velocity;
    }

    public double distance(@NotNull Point other) {
        return point.distance(other);
    }

    public double distance(@NotNull Position other) {
        return point.distance(other.point);
    }

    public double distance(@NotNull Unit other) {
        return point.distance(other);
    }
}
