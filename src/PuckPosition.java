import model.Puck;

public class PuckPosition extends Position {
    private static final double PUCK_FRICTION = 0.999;
    private static final double DAMPING = 0.25;
    private static final double PENETRATION_MULTIPLIER = 0.2;
    private static final double PENETRATION_CORRECTION = 0.008;
    private static final double FIELD_LEFT = Const.rinkLeft + Static.PUCK_RADIUS;
    private static final double FIELD_RIGHT = Const.rinkRight - Static.PUCK_RADIUS;
    private static final double FIELD_TOP = Const.rinkTop + Static.PUCK_RADIUS;
    private static final double FIELD_BOTTOM = Const.rinkBottom - Static.PUCK_RADIUS;

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
        double outLeft = FIELD_LEFT - point.x;
        double outRight = point.x - FIELD_RIGHT;
        double outTop = FIELD_TOP - point.y;
        double outBottom = point.y - FIELD_BOTTOM;
        boolean outX = outLeft > 0 || outRight > 0;
        boolean outY = outTop > 0 || outBottom > 0;
        Vec velocityWithFriction = velocity.multiply(PUCK_FRICTION);
        if (!outX && !outY) {
            return new PuckPosition(puck, point.shift(velocityWithFriction), velocityWithFriction);
        }
        Vec newVelocity = Vec.of(velocityWithFriction.x * (outX ? -DAMPING : 1), velocityWithFriction.y * (outY ? -DAMPING : 1));
        Point newPoint = Point.of(
                outLeft > 0 && newVelocity.x < outLeft ? FIELD_LEFT - PENETRATION_MULTIPLIER * outLeft - PENETRATION_CORRECTION :
                outRight > 0 && -newVelocity.x < outRight ? FIELD_RIGHT + PENETRATION_MULTIPLIER * outRight + PENETRATION_CORRECTION :
                point.x,
                outTop > 0 && newVelocity.y < outTop ? FIELD_TOP - PENETRATION_MULTIPLIER * outTop - PENETRATION_CORRECTION :
                outBottom > 0 && -newVelocity.y < outBottom ? FIELD_BOTTOM + PENETRATION_MULTIPLIER * outBottom + PENETRATION_CORRECTION :
                point.y
        );
        return new PuckPosition(puck, newPoint.shift(newVelocity), newVelocity);
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
