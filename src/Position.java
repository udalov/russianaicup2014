public abstract class Position {
    public final Point point;
    public final Vec velocity;

    public Position(@NotNull Point point, @NotNull Vec velocity) {
        this.point = point;
        this.velocity = velocity;
    }
}
