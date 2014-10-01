import model.Hockeyist;
import model.Unit;

import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;

public class Position {
    public final double x;
    public final double y;
    public final double speedX;
    public final double speedY;
    public final double angle;
    // TODO: angular speed

    public Position(double x, double y, double speedX, double speedY, double angle) {
        this.x = x;
        this.y = y;
        this.speedX = speedX;
        this.speedY = speedY;
        this.angle = angle;
    }

    @NotNull
    public static Position of(@NotNull Unit unit) {
        return new Position(unit.getX(), unit.getY(), unit.getSpeedX(), unit.getSpeedY(), unit.getAngle());
    }

    @NotNull
    public Point point() {
        return Point.of(x, y);
    }

    @NotNull
    public Vec velocity() {
        return Vec.of(speedX, speedY);
    }

    @NotNull
    public Vec direction() {
        return Vec.of(angle);
    }

    @NotNull
    public Position moveHockeyist(@NotNull Go go, @NotNull Hockeyist hockeyist) {
        double condition = Util.effectiveAttribute(hockeyist, hockeyist.getAgility());
        double turnLimit = Const.hockeyistTurnAngleFactor * condition;
        double turn = max(min(go.turn, turnLimit), -turnLimit);
        double relativeSpeedup = max(min(go.speedup, 1), -1);
        double speedup = relativeSpeedup * (relativeSpeedup > 0 ? Const.hockeyistSpeedUpFactor : Const.hockeyistSpeedDownFactor) * condition;
        Vec direction = Vec.of(angle + turn);
        Vec velocity = velocity().plus(direction.multiply(speedup)).multiply(0.98);
        return new Position(x + velocity.x, y + velocity.y, velocity.x, velocity.y, direction.angle());
    }

    // TODO: use inheritance or something
    @NotNull
    public Position movePuck() {
        Vec velocity = velocity().multiply(0.999);
        return new Position(x + velocity.x, y + velocity.y, velocity.x, velocity.y, angle);
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f) speed (%.3f, %.3f) angle %.3f", x, y, speedX, speedY, angle);
    }
}
