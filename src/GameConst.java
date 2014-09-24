import model.Game;

public class GameConst {
    public static double hockeyistSpeedUpFactor;
    public static double hockeyistSpeedDownFactor;
    public static double hockeyistTurnAngleFactor;

    public static void initialize(@NotNull Game game) {
        hockeyistSpeedUpFactor = game.getHockeyistSpeedUpFactor();
        hockeyistSpeedDownFactor = game.getHockeyistSpeedDownFactor();
        hockeyistTurnAngleFactor = game.getHockeyistTurnAngleFactor();
    }
}
