import model.*;

import java.util.ArrayList;
import java.util.List;

public class MyStrategy implements Strategy {
    private static final List<Team> TEAMS = new ArrayList<>(2);

    static {
        new Debug();
    }

    @Override
    public void move(@NotNull Hockeyist self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        Team team = findTeam(world.getMyPlayer());
        if (team == null) {
            team = new Team(game, world);
            TEAMS.add(team);
        }

        team.solveTick(world);

        Result result = new MakeTurn(team, self, world, game).makeTurn();
        move.setAction(result.action.type);
        move.setPassPower(result.action.passPower);
        move.setPassAngle(result.action.passAngle);
        move.setTeammateIndex(result.action.teammateIndex);
        move.setSpeedUp(result.direction.speedup);
        move.setTurn(result.direction.turn);

        Debug.update(world);
    }

    @Nullable
    private static Team findTeam(@NotNull Player player) {
        for (Team team : TEAMS) {
            if (team.myStartingPlayer.getId() == player.getId()) return team;
        }
        return null;
    }
}
