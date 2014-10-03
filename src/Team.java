import model.Hockeyist;
import model.HockeyistType;
import model.World;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private int currentTick = -1;
    private final List<Decision> decisions = new ArrayList<>(3);

    public int lastGoalTick;

    public void solveTick(@NotNull World world) {
        if (currentTick == world.getTick()) return;
        currentTick = world.getTick();
        decisions.clear();

        if (world.getMyPlayer().isJustMissedGoal() || world.getMyPlayer().isJustScoredGoal()) {
            lastGoalTick = currentTick;
        }

        List<Hockeyist> myFieldPlayers = myFieldPlayers(world);
        Point defensePoint = determineDefensePoint();

        Hockeyist closestToDefend = findClosestToPoint(myFieldPlayers, defensePoint);

        long puckOwnerPlayerId = world.getPuck().getOwnerPlayerId();
        if (puckOwnerPlayerId == Players.me.getId()) {
            long puckOwnerId = world.getPuck().getOwnerHockeyistId();
            for (Hockeyist hockeyist : myFieldPlayers) {
                long id = hockeyist.getId();
                if (id == puckOwnerId) {
                    decisions.add(new Decision(id, Decision.Role.ATTACK, defensePoint));
                } else if (id == closestToDefend.getId()) {
                    decisions.add(new Decision(id, Decision.Role.DEFENSE, defensePoint));
                } else {
                    decisions.add(new Decision(id, Decision.Role.MIDFIELD, Static.CENTER.shift(Players.defense.multiply(100))));
                }
            }
            return;
        }

        Hockeyist defender = closestToDefend;

        // If the defender is really close to the puck, he should run to it
        // TODO: do this even if the puck is owned by an enemy attacker, to tackle him
        if (puckOwnerPlayerId == -1 && findClosestGlobalPlayerToPuck(world) == closestToDefend) {
            // TODO: unhardcode
            if (closestToDefend.getDistanceTo(world.getPuck()) < 500) {
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
            if (closest == null || point.distance(hockeyist) < point.distance(closest)) {
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
            // s = v0*t + t^2/2
            // t = sqrt(v0^2 + 2*s) - v0
            double s = hockeyist.getDistanceTo(world.getPuck());
            double v0 = Util.speed(hockeyist);
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
    private static Point determineDefensePoint() {
        return Point.of(Players.me.getNetFront(), (Players.me.getNetTop() + Players.me.getNetBottom()) / 2)
                .shift(Players.attack.multiply(Static.HOCKEYIST_RADIUS * 3.2));
    }

    @NotNull
    public Decision getDecision(long id) {
        for (Decision decision : decisions) {
            if (decision.id == id) return decision;
        }
        throw new AssertionError("You're on your own, dude #" + id);
    }
}
