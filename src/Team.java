import model.*;

import java.util.ArrayList;
import java.util.List;

public class Team {
    public final Player myPlayer;

    // TODO: assert that Game doesn't change between ticks
    private final Game game;

    // 1 if we are on the left, -1 if we are on the right
    public final int attack;

    private int currentTick = -1;
    private final List<Decision> decisions;

    public Team(@NotNull Game game, @NotNull World startingWorld) {
        this.game = game;

        myPlayer = startingWorld.getMyPlayer();
        attack = myPlayer.getNetFront() < (game.getRinkLeft() + game.getRinkRight()) / 2 ? 1 : -1;

        decisions = new ArrayList<>(countControllablePlayers(startingWorld));
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

        if (world.getPuck().getOwnerPlayerId() == world.getMyPlayer().getId()) {
            long puckOwnerId = world.getPuck().getOwnerHockeyistId();
            for (Hockeyist hockeyist : myFieldPlayers) {
                long id = hockeyist.getId();
                if (id == puckOwnerId) decisions.add(new Decision(id, Decision.Role.ATTACK, defensePoint));
                else decisions.add(new Decision(id, Decision.Role.DEFENSE, defensePoint));
            }
        }
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
        y = Math.min(y, game.getGoalNetTop() + game.getGoalNetHeight() - radius);

        Point transposed = Point.of(myPlayer.getNetFront() + attack * radius, game.getRinkTop() + game.getRinkBottom() - y);

        return transposed.shiftX(attack * radius);
    }

    @NotNull
    public Decision getDecision(long id) {
        for (Decision decision : decisions) {
            if (decision.id == id) return decision;
        }
        throw new AssertionError("You're on your own, dude #" + id);
    }
}
