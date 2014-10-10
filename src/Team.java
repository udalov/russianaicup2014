import model.Hockeyist;
import model.HockeyistState;
import model.HockeyistType;
import model.World;

import java.util.*;

public class Team {
    private int currentTick = -1;
    private final List<Decision> decisions = new ArrayList<>(3);

    public Collection<Long> timeToRest;

    public int lastGoalTick;

    private static final Decision.Role[] ROLES = {Decision.Role.DEFENSE, Decision.Role.ATTACK, Decision.Role.MIDFIELD};

    public void solveTick(@NotNull World world) {
        if (currentTick == world.getTick()) return;
        currentTick = world.getTick();
        decisions.clear();

        boolean justAfterGoal = world.getMyPlayer().isJustMissedGoal() || world.getMyPlayer().isJustScoredGoal();
        if (justAfterGoal) {
            lastGoalTick = currentTick;
        }

        List<Hockeyist> candidates = new ArrayList<>(3);
        List<Hockeyist> resting = new ArrayList<>(3);
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getPlayerId() != Players.me.getId() || hockeyist.getType() == HockeyistType.GOALIE) continue;
            (hockeyist.getState() == HockeyistState.RESTING ? resting : candidates).add(hockeyist);
        }

        Collections.sort(candidates, new Comparator<Hockeyist>() {
            @Override
            public int compare(@NotNull Hockeyist o1, @NotNull Hockeyist o2) {
                return Integer.compare(o1.getOriginalPositionIndex(), o2.getOriginalPositionIndex());
            }
        });

        timeToRest = Players.teamSize == 6
                     ? idsToSubstitute(new ArrayList<>(candidates), resting, justAfterGoal)
                     : Collections.<Long>emptySet();

        int n = candidates.size();
        double best = Double.MAX_VALUE;
        Point[] bestFormation = null;
        int[] bestPermutation = null;
        for (Point[] formation : formations()) {
            for (int[] permutation : permutations(n)) {
                double cur = 0;
                for (int i = 0; i < n; i++) {
                    cur += Util.sqr(formation[i].distance(candidates.get(permutation[i])));
                }
                // TODO: this is a hack to make attacker stay on the puck's half of the rink in 2x2 games
                if (Players.teamSize == 2) {
                    for (int i = 0; i < n; i++) {
                        cur += Util.sqr(formation[i].distance(world.getPuck()));
                    }
                }
                if (cur < best) {
                    best = cur;
                    bestFormation = formation;
                    bestPermutation = permutation;
                }
            }
        }
        assert bestFormation != null && bestPermutation != null;
        for (int i = 0; i < n; i++) {
            Hockeyist candidate = candidates.get(bestPermutation[i]);
            decisions.add(new Decision(candidate.getId(), ROLES[i], bestFormation[i]));
        }

        // TODO: this is a hack to make defender come out and attack in 2x2 games
        if (Players.teamSize == 2) {
            long puckOwnerId = world.getPuck().getOwnerHockeyistId();
            boolean swap = false;
            for (Decision decision : decisions) {
                swap |= decision.id == puckOwnerId && decision.role == Decision.Role.DEFENSE;
            }
            if (swap) {
                Decision d0 = decisions.get(0);
                Decision d1 = decisions.get(1);
                decisions.set(0, new Decision(d0.id, d1.role, d1.dislocation));
                decisions.set(1, new Decision(d1.id, d0.role, d0.dislocation));
            }
        }
    }

    private static final int[][] PERMUTATIONS_2 = {{0, 1}, {1, 0}};
    private static final int[][] PERMUTATIONS_3 = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};

    @NotNull
    private static int[][] permutations(int n) {
        return n == 2 ? PERMUTATIONS_2 : PERMUTATIONS_3;
    }

    private static Point[][] formations;

    // Defense, attack, midfield
    @NotNull
    private static Point[][] formations() {
        if (formations == null) {
            Vec attack = Players.attack;
            Point defensePoint = Point.of(Players.me.getNetFront(), Static.CENTER.y).shift(attack.multiply(Static.HOCKEYIST_RADIUS * 3.2));
            formations = new Point[][]{
                    {
                            defensePoint,
                            Static.CENTER.shift(Vec.of(0, -200).plus(attack.multiply(200))),
                            Static.CENTER.shift(Vec.of(0, 200).plus(attack.multiply(-100)))
                    },
                    {
                            defensePoint,
                            Static.CENTER.shift(Vec.of(0, 200).plus(attack.multiply(200))),
                            Static.CENTER.shift(Vec.of(0, -200).plus(attack.multiply(-100)))
                    },
            };
        }
        return formations;
    }

    private static final Comparator<Hockeyist> STAMINA_COMPARATOR = new Comparator<Hockeyist>() {
        @Override
        public int compare(@NotNull Hockeyist o1, @NotNull Hockeyist o2) {
            return Double.compare(o1.getStamina(), o2.getStamina());
        }
    };

    @NotNull
    private static Collection<Long> idsToSubstitute(
            @NotNull List<Hockeyist> candidates,
            @NotNull List<Hockeyist> resting,
            boolean justAfterGoal
    ) {
        Collections.sort(candidates, STAMINA_COMPARATOR);
        Collections.sort(resting, STAMINA_COMPARATOR);
        Collections.reverse(resting);

        Collection<Long> result = null;
        for (int i = 0; i < 3; i++) {
            double diff = resting.get(i).getStamina() - candidates.get(i).getStamina();
            boolean substitute = justAfterGoal ? diff > 50 : diff > 500;
            if (substitute) {
                if (result == null) result = new ArrayList<>(3);
                result.add(candidates.get(i).getId());
            }
        }

        return result != null ? result : Collections.<Long>emptySet();
    }

    @NotNull
    public Decision getDecision(long id) {
        for (Decision decision : decisions) {
            if (decision.id == id) return decision;
        }
        throw new AssertionError("You're on your own, dude #" + id);
    }
}
