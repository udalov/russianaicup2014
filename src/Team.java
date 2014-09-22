import model.*;

import java.util.ArrayList;
import java.util.List;

public class Team {
    public final Player myStartingPlayer;

    private final Game game;

    // 1 if we are on the left, -1 if we are on the right
    public final int attack;

    private int currentTick = -1;
    private final List<Decision> decisions;

    public int lastGoalTick;

    public Team(@NotNull Game game, @NotNull World startingWorld) {
        this.game = game;

        myStartingPlayer = startingWorld.getMyPlayer();
        attack = myStartingPlayer.getNetFront() < (game.getRinkLeft() + game.getRinkRight()) / 2 ? 1 : -1;

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

        if (world.getMyPlayer().isJustMissedGoal() || world.getMyPlayer().isJustScoredGoal()) {
            lastGoalTick = currentTick;
        }

        List<Hockeyist> myFieldPlayers = myFieldPlayers(world);
        Point defensePoint = determineDefensePoint(world);

        long puckOwnerPlayerId = world.getPuck().getOwnerPlayerId();
        if (puckOwnerPlayerId == world.getMyPlayer().getId()) {
            long puckOwnerId = world.getPuck().getOwnerHockeyistId();
            for (Hockeyist hockeyist : myFieldPlayers) {
                long id = hockeyist.getId();
                if (id == puckOwnerId) decisions.add(new Decision(id, Decision.Role.ATTACK, defensePoint));
                else decisions.add(new Decision(id, Decision.Role.DEFENSE, defensePoint));
            }
            return;
        }

        Hockeyist closestToDefend = findClosestToPoint(myFieldPlayers, defensePoint);
        Hockeyist defender = closestToDefend;

        // If the defender is really close to the puck, he should run to it
        // TODO: do this even if the puck is owned by an enemy attacker, to tackle him
        if (puckOwnerPlayerId == -1 && findClosestGlobalPlayerToPuck(world) == closestToDefend) {
            // TODO: unhardcode
            if (closestToDefend.getDistanceTo(world.getPuck()) < 300) {
                List<Hockeyist> possibleDefenders = new ArrayList<>(myFieldPlayers);
                possibleDefenders.remove(closestToDefend);
                defender = findClosestToPoint(possibleDefenders, defensePoint);
            }
        }

        // TODO: make decisions based on previous decisions to make transitions smooth
        for (Hockeyist hockeyist : myFieldPlayers) {
            Decision.Role role = hockeyist == defender ? Decision.Role.DEFENSE : Decision.Role.ATTACK;
            decisions.add(new Decision(hockeyist.getId(), role, defensePoint));
        }
    }

    @NotNull
    private static Hockeyist findClosestToPoint(@NotNull List<Hockeyist> players, @NotNull Point point) {
        Hockeyist closest = null;
        for (Hockeyist hockeyist : players) {
            if (closest == null || point.sqrDist(hockeyist) < point.sqrDist(closest)) {
                closest = hockeyist;
            }
        }
        assert closest != null : "No field players: " + players;
        return closest;
    }

    @NotNull
    private static Hockeyist findClosestGlobalPlayerToPuck(@NotNull World world) {
        double bestTime = 0.0;
        Hockeyist bestPlayer = null;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            // TODO: take into account his speed direction, time to turn, etc.
            // TODO: hockeyists have max speed
            // s = v0*t + t^2/2
            // t = sqrt(v0^2 + 2*s) - v0
            double s = hockeyist.getDistanceTo(world.getPuck());
            double v0 = MakeTurn.speed(hockeyist);
            double t = Math.sqrt(v0*v0 + 2*s) - v0;
            if (bestPlayer == null || t < bestTime) {
                bestTime = t;
                bestPlayer = hockeyist;
            }
        }
        assert bestPlayer != null : "No field players: " + world;
        return bestPlayer;
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

        return Point.of(myStartingPlayer.getNetFront() + attack * (radius * 3.2), (myStartingPlayer.getNetTop() + myStartingPlayer.getNetBottom()) / 2);
    }

    @NotNull
    public Decision getDecision(long id) {
        for (Decision decision : decisions) {
            if (decision.id == id) return decision;
        }
        throw new AssertionError("You're on your own, dude #" + id);
    }
}
