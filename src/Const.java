import model.Game;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings({"MagicNumber", "UnusedDeclaration"})
public class Const {
    public static double worldWidth = 1200.0;
    public static double worldHeight = 800.0;

    public static double goalNetTop = 360.0;
    public static double goalNetWidth = 65.0;
    public static double goalNetHeight = 200.0;

    public static double rinkTop = 150.0;
    public static double rinkLeft = 65.0;
    public static double rinkBottom = 770.0;
    public static double rinkRight = 1135.0;

    public static int afterGoalStateTickCount = 300;
    public static int overtimeTickCount = 2000;

    public static int defaultActionCooldownTicks = 60;
    public static int swingActionCooldownTicks = 10;
    public static int cancelStrikeActionCooldownTicks = 30;
    public static int actionCooldownTicksAfterLosingPuck = 10;

    public static double stickLength = 120.0;
    public static double stickSector = 0.5235987755982988;
    public static double passSector = 2.0943951023931953;

    public static int hockeyistAttributeBaseValue = 100;

    public static double minActionChance = 0.05;
    public static double maxActionChance = 0.95;

    public static double strikeAngleDeviation = 0.03490658503988659;
    public static double passAngleDeviation = 0.026179938779914945;

    public static double pickUpPuckBaseChance = 0.6;
    public static double takePuckAwayBaseChance = 0.25;

    public static int maxEffectiveSwingTicks = 20;
    public static double strikePowerBaseFactor = 0.75;
    public static double strikePowerGrowthFactor = 0.0125;

    public static double strikePuckBaseChance = 0.75;

    public static double knockdownChanceFactor = 0.5;
    public static double knockdownTicksFactor = 40.0;

    public static double maxSpeedToAllowSubstitute = 1.0;
    public static double substitutionAreaHeight = 60.0;

    public static double passPowerFactor = 0.75;

    public static double hockeyistMaxStamina = 2000.0;
    public static double activeHockeyistStaminaGrowthPerTick = 0.5;
    public static double restingHockeyistStaminaGrowthPerTick = 1.0;
    public static double zeroStaminaHockeyistEffectivenessFactor = 0.75;
    public static double speedUpStaminaCostFactor = 1.0;
    public static double turnStaminaCostFactor = 1.0;
    public static double takePuckStaminaCost = 10.0;
    public static double swingStaminaCost = 10.0;
    public static double strikeStaminaBaseCost = 20.0;
    public static double strikeStaminaCostGrowthFactor = 0.5;
    public static double cancelStrikeStaminaCost = 0.0;
    public static double passStaminaCost = 40.0;

    public static double goalieMaxSpeed = 6.0;
    public static double hockeyistMaxSpeed = 15.0;

    public static double struckHockeyistInitialSpeedFactor = 4.0;

    public static double hockeyistSpeedUpFactor = 0.11574074074074074;
    public static double hockeyistSpeedDownFactor = 0.06944444444444445;
    public static double hockeyistTurnAngleFactor = 0.05235987755982989;

    public static int versatileHockeyistStrength = 100;
    public static int versatileHockeyistEndurance = 100;
    public static int versatileHockeyistDexterity = 100;
    public static int versatileHockeyistAgility = 100;

    public static int forwardHockeyistStrength = 110;
    public static int forwardHockeyistEndurance = 80;
    public static int forwardHockeyistDexterity = 105;
    public static int forwardHockeyistAgility = 105;

    public static int defencemanHockeyistStrength = 105;
    public static int defencemanHockeyistEndurance = 110;
    public static int defencemanHockeyistDexterity = 80;
    public static int defencemanHockeyistAgility = 105;

    public static int minRandomHockeyistParameter = 80;
    public static int maxRandomHockeyistParameter = 120;

    public static double struckPuckInitialSpeedFactor = 20.0;

    public static double puckBindingRange = 55.0;

    public static void initialize(@NotNull Game game) {
        try {
            for (Field field : Const.class.getDeclaredFields()) {
                String name = field.getName();
                Method method = Game.class.getMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
                Object expectedValue = field.get(null);
                Object actualValue = method.invoke(game);
                if (!expectedValue.equals(actualValue)) {
                    System.err.println("field " + name + " expected " + expectedValue + " actual " + actualValue);
                    field.set(null, actualValue);
                }
            }
        } catch (Throwable ignored) { }
    }
}
