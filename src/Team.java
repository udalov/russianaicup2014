import model.Game;
import model.Hockeyist;
import model.HockeyistType;
import model.World;

import java.util.ArrayList;
import java.util.List;

public class Team {
    public final long playerId;

    // TODO: assert that Game doesn't change between ticks
    private final Game game;

    private final Point myGoal;
    private final boolean areWeOnTheLeft;

    private int currentTick = -1;
    private final List<Decision> decisions;

    public Team(long playerId, @NotNull Game game, @NotNull World startingWorld) {
        this.playerId = playerId;
        this.game = game;

        myGoal = Point.of(findMyGoalie(startingWorld));
        areWeOnTheLeft = myGoal.x < (game.getRinkLeft() + game.getRinkRight()) / 2;

        decisions = new ArrayList<>(countControllablePlayers(startingWorld));
    }

    @NotNull
    private static Hockeyist findMyGoalie(@NotNull World startingWorld) {
        for (Hockeyist hockeyist : startingWorld.getHockeyists()) {
            if (hockeyist.isTeammate() && hockeyist.getType() == HockeyistType.GOALIE) return hockeyist;
        }
        throw new AssertionError("No goalie on the first tick :(");
    }

    private static int countControllablePlayers(@NotNull World startingWorld) {
        int result = 0;
        for (Hockeyist hockeyist : startingWorld.getHockeyists()) {
            if (hockeyist.isTeammate() && hockeyist.getType() != HockeyistType.GOALIE) result++;
        }
        return result;
    }

    public void solveTick(@NotNull World world) {
        if (currentTick == world.getTick()) return;
        currentTick = world.getTick();
        decisions.clear();

        List<Hockeyist> myFieldPlayers = myFieldPlayers(world);
        Point defensePoint = determineDefensePoint(world);

        Hockeyist closest = null;
        for (Hockeyist hockeyist : myFieldPlayers) {
            if (closest == null || defensePoint.sqrDist(hockeyist) < defensePoint.sqrDist(closest)) closest = hockeyist;
        }
        assert closest != null : "No field players: " + world;

        // TODO: make decisions based on previous decisions to make transitions smooth
        for (Hockeyist hockeyist : myFieldPlayers) {
            long id = hockeyist.getId();
            Decision.Role role = id == closest.getId() ? Decision.Role.DEFENSE : Decision.Role.ATTACK;
            decisions.add(new Decision(id, role, defensePoint));
        }
    }

    @NotNull
    private static List<Hockeyist> myFieldPlayers(@NotNull World world) {
        List<Hockeyist> result = new ArrayList<>(3);
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() && hockeyist.getType() != HockeyistType.GOALIE) result.add(hockeyist);
        }
        return result;
    }

    @NotNull
    private Point determineDefensePoint(@NotNull World world) {
        // TODO: radius can be different for different hockeyists
        double radius = world.getHockeyists()[0].getRadius();

        double y = world.getPuck().getY();
        y = Math.max(y, game.getGoalNetTop() + radius);
        y = Math.min(y, game.getGoalNetTop() + game.getGoalNetHeight() + radius);

        Point transposed = Point.of(myGoal.x, y).transpose(myGoal);

        return transposed.shiftX((areWeOnTheLeft ? 1 : -1) * radius);
    }

    @NotNull
    public Decision getDecision(long id) {
        for (Decision decision : decisions) {
            if (decision.id == id) return decision;
        }
        throw new AssertionError("You're on your own, dude #" + id);
    }
}
