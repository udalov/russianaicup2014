import model.Game;

// TODO: generate
public class GameConst {
    public static double rinkTop;
    public static double rinkLeft;
    public static double rinkBottom;
    public static double rinkRight;
    public static double hockeyistSpeedUpFactor;
    public static double hockeyistSpeedDownFactor;
    public static double hockeyistTurnAngleFactor;

    public static void initialize(@NotNull Game game) {
        rinkTop = game.getRinkTop();
        rinkLeft = game.getRinkLeft();
        rinkBottom = game.getRinkBottom();
        rinkRight = game.getRinkRight();
        hockeyistSpeedUpFactor = game.getHockeyistSpeedUpFactor();
        hockeyistSpeedDownFactor = game.getHockeyistSpeedDownFactor();
        hockeyistTurnAngleFactor = game.getHockeyistTurnAngleFactor();
    }
}
