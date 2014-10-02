import model.Puck;

public class PuckPosition extends Position {
    // Use with caution, coordinates are outdated
    public final Puck puck;

    public PuckPosition(@NotNull Puck puck, double x, double y, double speedX, double speedY, double angle) {
        super(x, y, speedX, speedY, angle);
        this.puck = puck;
    }

    @NotNull
    public static PuckPosition of(@NotNull Puck puck) {
        return new PuckPosition(puck, puck.getX(), puck.getY(), puck.getSpeedX(), puck.getSpeedY(), puck.getAngle());
    }

    @NotNull
    public PuckPosition move() {
        Vec velocity = velocity().multiply(0.999);
        return new PuckPosition(puck, x + velocity.x, y + velocity.y, velocity.x, velocity.y, angle);
    }

    @NotNull
    public PuckPosition inFrontOf(@NotNull HockeyistPosition position) {
        Vec direction = Vec.of(position.angle);
        Point point = position.point().shift(direction.multiply(Const.puckBindingRange));
        return new PuckPosition(puck, point.x, point.y, position.speedX, position.speedY, 0);
    }
}