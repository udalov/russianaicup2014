import model.Game;
import model.Hockeyist;
import model.Move;
import model.World;

public class MyStrategy implements Strategy {
    private static Team TEAM;

    static {
        new Debug();
    }

    @Override
    public void move(@NotNull Hockeyist self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        if (TEAM == null) {
            Const.initialize(game);
            Players.initialize(world);
            TEAM = new Team();
        }

        TEAM.solveTick(world);

        Result result = new MakeTurn(TEAM, self, world, game).makeTurn();
        move.setAction(result.action.type);
        move.setPassPower(result.action.passPower);
        move.setPassAngle(result.action.passAngle);
        move.setTeammateIndex(result.action.teammateIndex);
        move.setSpeedUp(result.direction.speedup);
        move.setTurn(result.direction.turn);

        Debug.update(world);
    }
}
