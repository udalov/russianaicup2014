import model.*;

import java.util.HashMap;
import java.util.Map;

public class MyStrategy implements Strategy {
    private static final boolean LOCAL = Thread.currentThread().getName().contains("local");
    public static final Map<Long, Result> LAST_RESULT = new HashMap<>();

    public static Team TEAM;

    @Override
    public void move(@NotNull Hockeyist self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        if (TEAM == null) {
            Const.initialize(game);
            Players.initialize(world);
            TEAM = new Team();
        }

        TEAM.solveTick(world);

        if (self.getRemainingKnockdownTicks() != 0) return;
        if (self.getState() == HockeyistState.RESTING) return;

        //noinspection PointlessBooleanExpression,ConstantConditions
        if (MakeTurn.DEBUG_DO_NOTHING_UNTIL_ENEMY_MIDFIELD_MOVES && waitForSignal(world)) return;

        Result result = new MakeTurn(TEAM, self, world).makeTurn();
        move.setAction(result.action.type);
        move.setPassPower(result.action.passPower);
        move.setPassAngle(result.action.passAngle);
        move.setTeammateIndex(result.action.teammateIndex);
        move.setSpeedUp(result.direction.speedup);
        move.setTurn(result.direction.turn);

        if (LOCAL) {
            LAST_RESULT.put(self.getId(), result);
        }
    }

    private boolean signalFired = false;
    private boolean waitForSignal(@NotNull World world) {
        if (signalFired) return false;
        for (Hockeyist enemy : world.getHockeyists()) {
            if (!enemy.isTeammate() && enemy.getOriginalPositionIndex() == 1) {
                if (Vec.velocity(enemy).length() > 1e-6) {
                    signalFired = true;
                    return false;
                }
            }
        }
        return true;
    }
}
