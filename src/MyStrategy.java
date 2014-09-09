import model.*;

import static java.lang.StrictMath.PI;

public final class MyStrategy implements Strategy {
    @Override
    public void move(@NotNull Hockeyist self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        move.setSpeedUp(-1.0D);
        move.setTurn(PI);
        move.setAction(ActionType.STRIKE);
    }
}
