import model.Unit;

import static java.lang.StrictMath.*;

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

    // TODO: all constants depend on the hockeyist's condition
    @NotNull
    public Position move(@NotNull Go go) {
        double turn = max(min(go.turn, Const.hockeyistTurnAngleFactor), -Const.hockeyistTurnAngleFactor);
        double speedup = go.speedup * (go.speedup > 0 ? Const.hockeyistSpeedUpFactor : Const.hockeyistSpeedDownFactor);
        Vec direction = Vec.of(cos(angle + turn), sin(angle + turn));
        Vec speed = velocity().plus(direction.multiply(speedup)).multiply(0.98);
        return new Position(x + speed.x, y + speed.y, speed.x, speed.y, Util.normalize(angle + turn));
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f) speed (%.3f, %.3f) angle %.3f", x, y, speedX, speedY, angle);
    }
}
