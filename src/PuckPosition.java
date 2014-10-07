import model.Puck;

public class PuckPosition extends Position {
    // Use with caution, coordinates are outdated
    public final Puck puck;

    public PuckPosition(@NotNull Puck puck, @NotNull Point point, @NotNull Vec velocity) {
        super(point, velocity);
        this.puck = puck;
    }

    @NotNull
    public static PuckPosition of(@NotNull Puck puck) {
        return new PuckPosition(puck, Point.of(puck), Vec.velocity(puck));
    }

    @NotNull
    public PuckPosition move() {
        Vec newVelocity = velocity.multiply(0.999);
        return new PuckPosition(puck, point.shift(newVelocity), newVelocity);
    }

    @NotNull
    public PuckPosition inFrontOf(@NotNull HockeyistPosition position) {
        return new PuckPosition(puck, Util.puckBindingPoint(position), position.velocity);
    }

    @Override
    public String toString() {
        return String.format("%s velocity %s", point, velocity);
    }
}
