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
    public final PuckPosition puck;
    public final int myIndex;
    public final int puckOwnerIndex;

    public State(@NotNull HockeyistPosition[] pos, @NotNull PuckPosition puck, int myIndex, int puckOwnerIndex) {
        this.pos = pos;
        this.puck = puck;
        this.myIndex = myIndex;
        this.puckOwnerIndex = puckOwnerIndex;
    }

    @NotNull
    public static State of(@NotNull Hockeyist self, @NotNull World world) {
        List<HockeyistPosition> positions = new ArrayList<>(9);
        int myIndex = -1;
        int puckOwnerIndex = -1;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getState() == HockeyistState.RESTING) continue;
            if (hockeyist.getId() == self.getId()) myIndex = positions.size();
            if (hockeyist.getId() == world.getPuck().getOwnerHockeyistId()) puckOwnerIndex = positions.size();
            positions.add(HockeyistPosition.of(hockeyist));
        }
        assert myIndex >= 0 : "No self: " + Arrays.toString(world.getHockeyists());
        return new State(positions.toArray(new HockeyistPosition[positions.size()]), PuckPosition.of(world.getPuck()),
                         myIndex, puckOwnerIndex);
    }

    @Nullable
    public Point enemyGoalie() {
        long opponentId = Players.opponent.getId();
        for (HockeyistPosition position : pos) {
            Hockeyist hockeyist = position.hockeyist;
            if (hockeyist.getPlayerId() == opponentId && hockeyist.getType() == HockeyistType.GOALIE) {
                return position.point;
            }
        }
        return null;
    }

    public boolean overtimeNoGoalies() {
        return enemyGoalie() == null;
    }

    @Nullable
    public HockeyistPosition puckOwner() {
        return puckOwnerIndex != -1 ? pos[puckOwnerIndex] : null;
    }

    @NotNull
    public HockeyistPosition me() {
        return pos[myIndex];
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
        Hockeyist myHockeyist = me().hockeyist;
        long myPlayerId = myHockeyist.getPlayerId();
        long myId = myHockeyist.getId();
        List<HockeyistPosition> result = new ArrayList<>(3);
        for (HockeyistPosition position : pos) {
            Hockeyist hockeyist = position.hockeyist;
            if (hockeyist.getType() == HockeyistType.GOALIE ||
                hockeyist.getId() == myId ||
                hockeyist.getState() == HockeyistState.RESTING) continue;
            if ((hockeyist.getPlayerId() == myPlayerId) == allies) {
                result.add(position);
            }
        }
        return result;
    }

    private static final int[] SPEEDUPS = {1, -1, 0};
    @NotNull
    public Iterable<Go> iteratePossibleMoves(int step) {
        double d = Const.hockeyistTurnAngleFactor * me().agility();
        Collection<Go> result = new ArrayList<>((2 * step + 1) * SPEEDUPS.length);
        for (int speedup : SPEEDUPS) {
            for (int t = step; t > 0; t--) {
                result.add(Go.go(speedup, -t * d / step));
                result.add(Go.go(speedup, t * d / step));
            }
            result.add(Go.go(speedup, 0));
        }

        return result;
    }

    @NotNull
    public State apply(@NotNull Go go) {
        HockeyistPosition[] positions = Arrays.copyOf(pos, pos.length);
        PuckPosition newPuck = null;
        int n = positions.length;
        for (int i = 0; i < n; i++) {
            positions[i] = moveSingleHockeyist(positions[i], i == myIndex ? go : DEFAULT_HOCKEYIST_DIRECTION);
            if (i == puckOwnerIndex) {
                newPuck = puck.inFrontOf(positions[i]);
                // TODO: improve collisions of puck owner with walls
                if (isOutsideRink(newPuck, Static.PUCK_RADIUS) || isOutsideRink(positions[i], Static.HOCKEYIST_RADIUS)) {
                    newPuck = puck;
                    positions[i] = pos[i];
                }
            } else {
                // TODO: improve collisions of hockeyists with walls
                if (isOutsideRink(positions[i], Static.HOCKEYIST_RADIUS)) {
                    positions[i] = pos[i];
                }
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // TODO: improve collisions of hockeyists
                if (positions[i].distance(positions[j]) < 2 * Static.HOCKEYIST_RADIUS) {
                    HockeyistPosition first = pos[i];
                    HockeyistPosition second = pos[j];
                    // TODO: this is so ad-hoc and wrong...
                    double newSpeed = (first.velocity.length() + second.velocity.length()) * 0.25;
                    positions[i] = new HockeyistPosition(first.hockeyist, first.point,
                                                         first.velocity.normalize().multiply(-newSpeed),
                                                         first.cooldown, first.angle, first.angularSpeed);
                    positions[j] = new HockeyistPosition(second.hockeyist, second.point,
                                                         second.velocity.normalize().multiply(-newSpeed),
                                                         second.cooldown, second.angle, second.angularSpeed);
                }
            }
        }

        if (newPuck == null) {
            // TODO: support collisions of puck with goalies
            newPuck = puck.move();
        }

        return new State(positions, newPuck, myIndex, puckOwnerIndex);
    }

    @NotNull
    public State moveAllNoCollisions(@NotNull Go myDirection, @NotNull Go othersDirection) {
        HockeyistPosition[] positions = Arrays.copyOf(pos, pos.length);
        for (int i = 0, n = positions.length; i < n; i++) {
            positions[i] = moveSingleHockeyist(positions[i], i == myIndex ? myDirection : othersDirection);
        }
        return new State(positions, puckOwnerIndex == -1 ? puck.move() : puck.inFrontOf(positions[puckOwnerIndex]), myIndex, puckOwnerIndex);
    }

    @NotNull
    private HockeyistPosition moveSingleHockeyist(@NotNull HockeyistPosition position, @NotNull Go go) {
        if (position.hockeyist.getType() == HockeyistType.GOALIE) {
            double puckY = puck.point.y;
            double goalieY = position.point.y;
            double newY = max(min(
                    goalieY + max(min(puckY - goalieY, Const.goalieMaxSpeed), -Const.goalieMaxSpeed),
                    Const.goalNetTop + Const.goalNetHeight - Static.HOCKEYIST_RADIUS
            ), Const.goalNetTop + Static.HOCKEYIST_RADIUS);
            return new HockeyistPosition(position.hockeyist, Point.of(position.point.x, newY), Vec.ZERO, 0, 0, 0);
        }
        return position.move(go);
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
        return "me at " + me();
    }
}
