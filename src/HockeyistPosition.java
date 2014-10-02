import model.Hockeyist;

import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;

public class HockeyistPosition extends Position {
    // Use with caution, coordinates are outdated
    public final Hockeyist hockeyist;

    public final double angularSpeed;

    public HockeyistPosition(@NotNull Hockeyist hockeyist, double x, double y, double speedX, double speedY, double angle,
                             double angularSpeed) {
        super(x, y, speedX, speedY, angle);
        this.hockeyist = hockeyist;
        this.angularSpeed = angularSpeed;
    }

    @NotNull
    public static HockeyistPosition of(@NotNull Hockeyist hockeyist) {
        return new HockeyistPosition(hockeyist, hockeyist.getX(), hockeyist.getY(), hockeyist.getSpeedX(), hockeyist.getSpeedY(),
                                     hockeyist.getAngle(), hockeyist.getAngularSpeed());
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
        Vec velocity = velocity().plus(direction.multiply(speedup)).multiply(0.98);
        return new HockeyistPosition(hockeyist, x + velocity.x, y + velocity.y, velocity.x, velocity.y, direction.angle(), newAngular);
    }
}
