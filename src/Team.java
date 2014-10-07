import model.Hockeyist;
import model.HockeyistState;
import model.HockeyistType;
import model.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Team {
    private int currentTick = -1;
    private final List<Decision> decisions = new ArrayList<>(3);

    public int lastGoalTick;

    private static final Decision.Role[] ROLES = {Decision.Role.DEFENSE, Decision.Role.ATTACK, Decision.Role.MIDFIELD};

    public void solveTick(@NotNull World world) {
        if (currentTick == world.getTick()) return;
        currentTick = world.getTick();
        decisions.clear();

        if (world.getMyPlayer().isJustMissedGoal() || world.getMyPlayer().isJustScoredGoal()) {
            lastGoalTick = currentTick;
        }

        List<Hockeyist> candidates = new ArrayList<>(3);
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getPlayerId() != Players.me.getId() || hockeyist.getType() == HockeyistType.GOALIE) continue;
            if (hockeyist.getState() == HockeyistState.RESTING) continue;
            candidates.add(hockeyist);
        }

        Collections.sort(candidates, new Comparator<Hockeyist>() {
            @Override
            public int compare(@NotNull Hockeyist o1, @NotNull Hockeyist o2) {
                return Integer.compare(o1.getOriginalPositionIndex(), o2.getOriginalPositionIndex());
            }
        });

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
    }

    private static final int[][] PERMUTATIONS_2 = {{0, 1}, {1, 0}};
    private static final int[][] PERMUTATIONS_3 = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};

    @NotNull
    private static int[][] permutations(int n) {
        return n == 2 ? PERMUTATIONS_2 : PERMUTATIONS_3;
    }

    // Defense, attack, midfield
    // TODO: cache
    @NotNull
    public static Point[][] formations() {
        Vec attack = Players.attack;
        Point defensePoint = Point.of(Players.me.getNetFront(), Static.CENTER.y).shift(attack.multiply(Static.HOCKEYIST_RADIUS * 3.2));
        return new Point[][]{
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

    @NotNull
    public Decision getDecision(long id) {
        for (Decision decision : decisions) {
            if (decision.id == id) return decision;
        }
        throw new AssertionError("You're on your own, dude #" + id);
    }
}
