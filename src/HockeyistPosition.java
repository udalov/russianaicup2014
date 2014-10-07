import model.Hockeyist;

import static java.lang.StrictMath.*;

public class HockeyistPosition extends Position {
    // Use with caution, coordinates are outdated
    public final Hockeyist hockeyist;

    public final int cooldown;
    public final double angle;
    public final double angularSpeed;

    private Vec direction;

    public HockeyistPosition(@NotNull Hockeyist hockeyist, @NotNull Point point, @NotNull Vec velocity, int cooldown,
                             double angle, double angularSpeed) {
        super(point, velocity);
        this.hockeyist = hockeyist;
        this.cooldown = cooldown;
        this.angle = angle;
        this.angularSpeed = angularSpeed;
    }

    @NotNull
    public static HockeyistPosition of(@NotNull Hockeyist hockeyist) {
        return new HockeyistPosition(hockeyist, Point.of(hockeyist), Vec.velocity(hockeyist), hockeyist.getRemainingCooldownTicks(),
                                     hockeyist.getAngle(), hockeyist.getAngularSpeed());
    }

    @NotNull
    public Vec direction() {
        if (direction == null) direction = Vec.of(angle);
        return direction;
    }

    public double angleTo(@NotNull Vec other) {
        return Util.normalize(other.angle() - angle);
    }

    public double angleTo(@NotNull Point other) {
        return angleTo(other.x, other.y);
    }

    public double angleTo(@NotNull PuckPosition puck) {
        return angleTo(puck.point.x, puck.point.y);
    }

    private double angleTo(double x, double y) {
        return Util.normalize(atan2(y - point.y, x - point.x) - angle);
    }

    public long id() {
        return hockeyist.getId();
    }

    public boolean teammate() {
        return hockeyist.getPlayerId() == Players.me.getId();
    }

    // TODO: store stamina and recalculate attributes with it

    public double strength() {
        return Util.effectiveAttribute(hockeyist, hockeyist.getStrength());
    }

    public double endurance() {
        return Util.effectiveAttribute(hockeyist, hockeyist.getEndurance());
    }

    public double dexterity() {
        return Util.effectiveAttribute(hockeyist, hockeyist.getDexterity());
    }

    public double agility() {
        return Util.effectiveAttribute(hockeyist, hockeyist.getAgility());
    }

    @NotNull
    public HockeyistPosition move(@NotNull Go go) {
        double condition = agility();
        double turnLimit = Const.hockeyistTurnAngleFactor * condition;
        double turn = max(min(go.turn, turnLimit), -turnLimit);
        double relativeSpeedup = max(min(go.speedup, 1), -1);
        double speedup = relativeSpeedup * (relativeSpeedup > 0 ? Const.hockeyistSpeedUpFactor : Const.hockeyistSpeedDownFactor) * condition;
        double newAngular = angularSpeed * (1 - 0.0270190131);
        double newAngle = Util.normalize(angle + turn + newAngular);
        Vec newDirection = abs(newAngle - angle) < 1e-6 ? direction() : Vec.of(newAngle); // Optimization
        Vec newVelocity = velocity.plus(newDirection.multiply(speedup)).multiply(0.98);
        return new HockeyistPosition(hockeyist, point.shift(newVelocity), newVelocity, max(cooldown - 1, 0), newAngle, newAngular);
    }

    @Override
    public String toString() {
        return String.format("%s velocity %s angle %.3f angular %.3f", point, velocity, angle, angularSpeed);
    }
}
