import model.Hockeyist;
import model.HockeyistState;
import model.HockeyistType;
import model.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;

public class State {
    public static final Go DEFAULT_HOCKEYIST_DIRECTION = Go.NOWHERE;

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

    @Nullable
    public HockeyistPosition puckOwner() {
        return puckOwnerIndex == -2 ? me : puckOwnerIndex != -1 ? pos[puckOwnerIndex] : null;
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
    private Iterable<HockeyistPosition> filterTeam(boolean allies) {
        // TODO: optimize
        long myPlayerId = Players.me.getId();
        List<HockeyistPosition> result = new ArrayList<>(3);
        for (HockeyistPosition position : pos) {
            if ((position.hockeyist.getPlayerId() == myPlayerId) == allies) {
                result.add(position);
            }
        }
        return result;
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
    public State applyWithCollisions(@NotNull Go go) {
        int n = pos.length;
        HockeyistPosition[] newPos = new HockeyistPosition[n];
        for (int i = 0; i < n; i++) {
            HockeyistPosition newPosition = pos[i].move(DEFAULT_HOCKEYIST_DIRECTION);
            if (i != puckOwnerIndex && isOutsideRink(newPosition, Static.HOCKEYIST_RADIUS)) {
                // TODO: improve collisions of hockeyists with walls
                newPos[i] = pos[i];
            } else {
                newPos[i] = newPosition;
            }
        }

        HockeyistPosition newMe = me.move(go);
        if (puckOwnerIndex != -2 && isOutsideRink(newMe, Static.HOCKEYIST_RADIUS)) {
            // TODO: improve collisions of self with walls
            newMe = me;
        }

        PuckPosition newPuck;
        if (puckOwnerIndex != -1) {
            HockeyistPosition newPuckOwner = puckOwnerIndex == -2 ? newMe : newPos[puckOwnerIndex];
            newPuck = puck.inFrontOf(newPuckOwner);
            // TODO: improve collisions of puck owner with walls
            if (isOutsideRink(newPuck, Static.PUCK_RADIUS) || isOutsideRink(newPuckOwner, Static.HOCKEYIST_RADIUS)) {
                newPuck = puck;
                if (puckOwnerIndex != -2) {
                    newPos[puckOwnerIndex] = puckOwner();
                } else {
                    newMe = me;
                }
            }
        } else {
            // TODO: support collisions of puck with goalies
            newPuck = puck.move();
        }

        // TODO: support collisions of hockeyists

        return new State(newPos, newMe, newPuck, puckOwnerIndex, moveGoalie());
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

    private static boolean isOutsideRink(@NotNull Position position, double radius) {
        Point point = position.point;
        return point.x - Const.rinkLeft < radius ||
               Const.rinkRight - point.x < radius ||
               point.y - Const.rinkTop < radius ||
               Const.rinkBottom - point.y < radius;
    }

    @Override
    public String toString() {
        return "me at " + me;
    }
}
