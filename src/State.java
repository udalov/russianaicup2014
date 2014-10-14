import model.Hockeyist;
import model.HockeyistState;
import model.HockeyistType;
import model.World;

import java.util.*;

import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;

public class State {
    public final HockeyistPosition[] pos;
    public final HockeyistPosition me;
    public final PuckPosition puck;
    public final int puckOwnerIndex; // -1 if nobody, -2 if me
    public final double goalieY; // if goalieY < 0, there are no goalies

    public State(@NotNull HockeyistPosition[] pos, @NotNull HockeyistPosition me, @NotNull PuckPosition puck, int puckOwnerIndex,
                 double goalieY) {
        this.pos = pos;
        this.me = me;
        this.puck = puck;
        this.puckOwnerIndex = puckOwnerIndex;
        this.goalieY = goalieY;
    }

    @NotNull
    public static State of(@NotNull Hockeyist self, @NotNull World world) {
        List<HockeyistPosition> positions = new ArrayList<>(5);
        HockeyistPosition me = null;
        int puckOwnerIndex = -1;
        double goalieY = -1;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getState() == HockeyistState.RESTING) continue;
            if (hockeyist.getType() == HockeyistType.GOALIE) {
                goalieY = hockeyist.getY();
                continue;
            }
            boolean isPuckOwner = hockeyist.getId() == world.getPuck().getOwnerHockeyistId();
            if (hockeyist.getId() == self.getId()) {
                me = HockeyistPosition.of(hockeyist);
                if (isPuckOwner) puckOwnerIndex = -2;
                continue;
            }
            if (isPuckOwner) puckOwnerIndex = positions.size();
            positions.add(HockeyistPosition.of(hockeyist));
        }
        assert me != null : "No self: " + Arrays.toString(world.getHockeyists());
        return new State(positions.toArray(new HockeyistPosition[positions.size()]), me, PuckPosition.of(world.getPuck()),
                         puckOwnerIndex, goalieY);
    }

    public boolean overtimeNoGoalies() {
        return goalieY < 0;
    }

    @NotNull
    public Iterable<HockeyistPosition> allies() {
        return filterTeam(true);
    }

    @NotNull
    public Iterable<HockeyistPosition> enemies() {
        return filterTeam(false);
    }

    @NotNull
    private Iterable<HockeyistPosition> filterTeam(final boolean allies) {
        final Iterator<HockeyistPosition> iterator = new Iterator<HockeyistPosition>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                while (i < pos.length && (pos[i].hockeyist.isTeammate() != allies)) i++;
                return i < pos.length;
            }

            @Override
            public HockeyistPosition next() {
                return pos[i++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return new Iterable<HockeyistPosition>() {
            @NotNull
            @Override
            public Iterator<HockeyistPosition> iterator() {
                return iterator;
            }
        };
    }

    private static final double[] SPEEDUPS = {1, -1, -0.5, 0.5, 0};
    @NotNull
    public Iterable<Go> iteratePossibleMoves(int step) {
        double d = Const.hockeyistTurnAngleFactor * me.agility();
        Collection<Go> result = new ArrayList<>((2 * step + 1) * SPEEDUPS.length);
        for (double speedup : SPEEDUPS) {
            double turn = d;
            for (int i = 0; i < step; i++) {
                result.add(Go.go(speedup, -turn));
                result.add(Go.go(speedup, turn));
                turn *= 0.6;
            }
            result.add(Go.go(speedup, 0));
        }

        return result;
    }

    @NotNull
    public State apply(@NotNull Go myDirection, @NotNull Go othersDirection) {
        int n = pos.length;
        HockeyistPosition[] newPos = new HockeyistPosition[n];
        for (int i = 0; i < n; i++) {
            newPos[i] = pos[i].move(othersDirection);
        }
        HockeyistPosition newMe = me.move(myDirection);
        PuckPosition newPuck = puckOwnerIndex == -1 ? puck.move() :
                               puckOwnerIndex == -2 ? puck.inFrontOf(newMe) :
                               puck.inFrontOf(newPos[puckOwnerIndex]);
        return new State(newPos, newMe, newPuck, puckOwnerIndex, moveGoalie());
    }

    @NotNull
    public State apply(@NotNull Go go) {
        HockeyistPosition newMe = me.move(go);
        PuckPosition newPuck = puckOwnerIndex == -1 ? puck.move() :
                               puckOwnerIndex == -2 ? puck.inFrontOf(newMe) :
                               puck;
        return new State(pos, newMe, newPuck, puckOwnerIndex, moveGoalie());
    }

    private double moveGoalie() {
        return max(min(
                goalieY + max(min(puck.point.y - goalieY, Const.goalieMaxSpeed), -Const.goalieMaxSpeed),
                Const.goalNetTop + Const.goalNetHeight - Static.HOCKEYIST_RADIUS
        ), Const.goalNetTop + Static.HOCKEYIST_RADIUS);
    }

    @Override
    public String toString() {
        return "me at " + me;
    }
}
