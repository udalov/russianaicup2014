import model.Hockeyist;
import model.Unit;
import model.World;

import static java.lang.StrictMath.*;

public class Util {
    public static double sqr(double x) {
        return x * x;
    }

    public static double angleDiff(double a, double b) {
        // TODO: what the hell is this, I don't even...
        return abs(atan2(sin(a - b), cos(a - b)));
    }

    public static double normalize(double angle) {
        while (angle < -PI) angle += 2 * PI;
        while (angle > PI) angle -= 2 * PI;
        return angle;
    }

    @NotNull
    public static Hockeyist findById(@NotNull World world, long id) {
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getId() == id) return hockeyist;
        }
        throw new AssertionError("Hockeyist #" + id + " is missing, maybe injured?");
    }

    public static double speed(@NotNull Unit unit) {
        return hypot(unit.getSpeedX(), unit.getSpeedY());
    }
}
