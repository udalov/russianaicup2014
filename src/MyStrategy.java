import model.Game;
import model.Hockeyist;
import model.Move;
import model.World;

import java.util.ArrayList;
import java.util.List;

public final class MyStrategy implements Strategy {
    private static final List<Team> TEAMS = new ArrayList<>(2);

    @Override
    public void move(@NotNull Hockeyist self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        Team team = findTeam(self.getPlayerId());
        if (team == null) {
            team = new Team(self.getPlayerId(), game, world);
            TEAMS.add(team);
        }

        team.solveTick(world);

        MakeTurn.Result result = new MakeTurn(team, self, world, game).makeTurn();
        move.setAction(result.action.type);
        move.setPassPower(result.action.passPower);
        move.setPassAngle(result.action.passAngle);
        move.setTeammateIndex(result.action.teammateIndex);
        move.setSpeedUp(result.direction.speedup);
        move.setTurn(result.direction.turn);
    }

    @Nullable
    private static Team findTeam(long playerId) {
        for (Team team : TEAMS) {
            if (team.playerId == playerId) return team;
        }
        return null;
    }
}
