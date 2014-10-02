import model.Hockeyist;
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
        for (int i = 0, n = positions.length; i < n; i++) {
            positions[i] = positions[i].move(i == myIndex ? go : DEFAULT_DIRECTION);
        }
        HockeyistPosition puckOwner = puckOwner();
        PuckPosition newPuck = puckOwner == null ? puck.move() : puck.inFrontOf(puckOwner);
        return new State(positions, newPuck, myIndex, puckOwnerIndex);
    }

    @Override
    public String toString() {
        return "me at " + me();
    }
}
