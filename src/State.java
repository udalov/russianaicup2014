import model.Hockeyist;
import model.HockeyistState;
import model.HockeyistType;
import model.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class State {
    public static final Go DEFAULT_DIRECTION = Go.go(0, 0);

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
    public HockeyistPosition enemyGoalie() {
        long opponentId = Players.opponent.getId();
        for (HockeyistPosition position : pos) {
            Hockeyist hockeyist = position.hockeyist;
            if (hockeyist.getPlayerId() == opponentId && hockeyist.getType() == HockeyistType.GOALIE) {
                return position;
            }
        }
        return null;
    }

    @Nullable
    public HockeyistPosition puckOwner() {
        if (puckOwnerIndex == -1) return null;
        for (int i = 0; i < pos.length; i++) {
            if (i == puckOwnerIndex) return pos[i];
        }
        return null;
    }

    @NotNull
    public HockeyistPosition me() {
        return pos[myIndex];
    }

    @NotNull
    public Hockeyist self() {
        return pos[myIndex].hockeyist;
    }

    @NotNull
    public State apply(@NotNull Go go) {
        HockeyistPosition[] positions = Arrays.copyOf(pos, pos.length);
        PuckPosition newPuck = null;
        int n = positions.length;
        for (int i = 0; i < n; i++) {
            positions[i] = positions[i].move(i == myIndex ? go : DEFAULT_DIRECTION);
            if (i == puckOwnerIndex) {
                newPuck = puck.inFrontOf(positions[i]);
                // TODO: improve collisions of puck owner with walls
                if (isOutsideRink(newPuck.point, Static.PUCK_RADIUS) || isOutsideRink(positions[i].point, Static.HOCKEYIST_RADIUS)) {
                    newPuck = puck;
                    positions[i] = pos[i];
                }
            } else {
                // TODO: improve collisions of hockeyists with walls
                if (isOutsideRink(positions[i].point, Static.HOCKEYIST_RADIUS)) {
                    positions[i] = pos[i];
                }
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // TODO: improve collisions of hockeyists
                if (positions[i].point.distance(positions[j].point) < 2 * Static.HOCKEYIST_RADIUS) {
                    HockeyistPosition first = pos[i];
                    HockeyistPosition second = pos[j];
                    // TODO: this is so ad-hoc and wrong...
                    double newSpeed = (first.velocity.length() + second.velocity.length()) * 0.25;
                    positions[i] = new HockeyistPosition(first.hockeyist, first.point,
                                                         first.velocity.normalize().multiply(-newSpeed),
                                                         first.angle, first.angularSpeed);
                    positions[j] = new HockeyistPosition(second.hockeyist, second.point,
                                                         second.velocity.normalize().multiply(-newSpeed),
                                                         second.angle, second.angularSpeed);
                }
            }
        }

        // TODO: support collisions of puck with goalies
        if (newPuck == null) {
            newPuck = puck.move();
            if (isOutsideRink(newPuck.point, Static.PUCK_RADIUS)) {
                boolean dampX = newPuck.point.x - Const.rinkLeft < Static.PUCK_RADIUS ||
                                Const.rinkRight - newPuck.point.x < Static.PUCK_RADIUS;
                boolean dampY = newPuck.point.y - Const.rinkTop < Static.PUCK_RADIUS ||
                                Const.rinkBottom - newPuck.point.y < Static.PUCK_RADIUS;
                Vec velocity = Vec.of(puck.velocity.x * (dampX ? -0.25 : 1), puck.velocity.y * (dampY ? -0.25 : 1));
                newPuck = new PuckPosition(puck.puck, puck.point, velocity);
            }
        }

        return new State(positions, newPuck, myIndex, puckOwnerIndex);
    }

    private static boolean isOutsideRink(@NotNull Point point, double radius) {
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
