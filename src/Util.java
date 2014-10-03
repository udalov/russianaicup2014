import model.Hockeyist;
import model.Unit;
import model.World;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.hypot;

public class Util {
    public static double sqr(double x) {
        return x * x;
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

    @NotNull
    public static Point puckBindingPoint(@NotNull HockeyistPosition hockeyist) {
        return hockeyist.point.shift(hockeyist.direction().multiply(Const.puckBindingRange));
    }

    public static double effectiveAttribute(@NotNull Hockeyist hockeyist, double attribute) {
        return (0.75 + 0.25 * hockeyist.getStamina() / 2000) * attribute / 100;
    }
}
