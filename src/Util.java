import model.Hockeyist;
import model.HockeyistState;
import model.Unit;
import model.World;

import static java.lang.StrictMath.*;

public class Util {
    private static final double HALF_PI = PI / 2;

    @SuppressWarnings("MagicNumber")
    public static double fastSin(double x) {
        x /= HALF_PI;
        if (x > 0.999999999) x = 2 - x;
        else if (x < -0.999999999) x = -2 - x;
        double x2 = x * x;
        return ((((.00015148419 * x2 - .00467376557) * x2 + .07968967928) * x2 - .64596371106) * x2 + 1.57079631847) * x;
    }

    public static double fastCos(double x) {
        return fastSin(HALF_PI - x);
    }

    public static double sqr(double x) {
        return x * x;
    }

    public static double hypot(double a, double b) {
        // Surprisingly, hypot is much slower than the naive method
        return sqrt(a * a + b * b);
    }

    public static double normalize(double angle) {
        while (angle < -PI) angle += 2 * PI;
        while (angle > PI) angle -= 2 * PI;
        return angle;
    }

    public static double speed(@NotNull Unit unit) {
        return hypot(unit.getSpeedX(), unit.getSpeedY());
    }

    @NotNull
    public static Point puckBindingPoint(@NotNull HockeyistPosition hockeyist) {
        return hockeyist.point.shift(hockeyist.direction().multiply(Const.puckBindingRange));
    }

    public static double effectiveAttribute(@NotNull Hockeyist hockeyist, double attribute) {
        double d = Const.zeroStaminaHockeyistEffectivenessFactor;
        return (d + (1 - d) * hockeyist.getStamina() / Const.hockeyistMaxStamina) * attribute / 100;
    }

    public static double takeFreePuckProbability(@NotNull HockeyistPosition hockeyist, @NotNull PuckPosition puck) {
        Hockeyist h = hockeyist.hockeyist;
        return Const.pickUpPuckBaseChance +
               max(effectiveAttribute(h, h.getDexterity()), effectiveAttribute(h, h.getAgility())) -
               puck.velocity.length() / 20;
    }

    @NotNull
    public static Hockeyist findRestingAllyWithMaxStamina(@NotNull World world) {
        Hockeyist best = null;
        for (Hockeyist ally : world.getHockeyists()) {
            if (ally.isTeammate() && ally.getState() == HockeyistState.RESTING) {
                if (best == null || best.getStamina() < ally.getStamina()) {
                    best = ally;
                }
            }
        }
        assert best != null : "No resting hockeyists O_o: " + world;
        return best;
    }
}
