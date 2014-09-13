import model.Game;
import model.Hockeyist;
import model.Move;
import model.World;

public final class MyStrategy implements Strategy {
    @Override
    public void move(@NotNull Hockeyist self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        MakeTurn.Result result = new MakeTurn(self, world, game).makeTurn();
        move.setAction(result.action.type);
        move.setPassPower(result.action.passPower);
        move.setPassAngle(result.action.passAngle);
        move.setTeammateIndex(result.action.teammateIndex);
        move.setSpeedUp(result.direction.speedup);
        move.setTurn(result.direction.turn);
    }
}
