import model.Hockeyist;

import static java.lang.StrictMath.*;

public class HockeyistPosition extends Position {
    // Use with caution, coordinates are outdated
    public final Hockeyist hockeyist;

    public final double angle;
    public final double angularSpeed;

    public HockeyistPosition(@NotNull Hockeyist hockeyist, @NotNull Point point, @NotNull Vec velocity, double angle, double angularSpeed) {
        super(point, velocity);
        this.hockeyist = hockeyist;
        this.angle = angle;
        this.angularSpeed = angularSpeed;
    }

    @NotNull
    public static HockeyistPosition of(@NotNull Hockeyist hockeyist) {
        return new HockeyistPosition(hockeyist, Point.of(hockeyist), Vec.velocity(hockeyist),
                                     hockeyist.getAngle(), hockeyist.getAngularSpeed());
    }

    @NotNull
    public Vec direction() {
        return Vec.of(angle);
    }

    public double angleTo(@NotNull Vec other) {
        return Util.normalize(angle - other.angle());
    }

    public double angleTo(@NotNull Point other) {
        return angleTo(other.x, other.y);
    }

    public double angleTo(@NotNull PuckPosition puck) {
        return angleTo(puck.point.x, puck.point.y);
    }

    public double angleTo(double x, double y) {
        return Util.normalize(angle - atan2(y - point.y, x - point.x));
    }

    public long id() {
        return hockeyist.getId();
    }

    public boolean teammate() {
        return hockeyist.getPlayerId() == Players.me.getId();
    }

    @NotNull
    public HockeyistPosition move(@NotNull Go go) {
        double condition = Util.effectiveAttribute(hockeyist, hockeyist.getAgility());
        double turnLimit = Const.hockeyistTurnAngleFactor * condition;
        double turn = max(min(go.turn, turnLimit), -turnLimit);
        double relativeSpeedup = max(min(go.speedup, 1), -1);
        double speedup = relativeSpeedup * (relativeSpeedup > 0 ? Const.hockeyistSpeedUpFactor : Const.hockeyistSpeedDownFactor) * condition;
        double newAngular = angularSpeed * (1 - 0.0270190131); // Thank you, Mr.Smile
        Vec direction = Vec.of(angle + turn + newAngular);
        Vec velocity = this.velocity.plus(direction.multiply(speedup)).multiply(0.98);
        return new HockeyistPosition(hockeyist, point.shift(velocity), velocity, direction.angle(), newAngular);
    }

    @Override
    public String toString() {
        return String.format("%s velocity %s angle %.3f angular %.3f", point, velocity, angle, angularSpeed);
    }
}
