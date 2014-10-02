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

    public State(@NotNull HockeyistPosition[] pos, @NotNull PuckPosition puck, int myIndex) {
        this.pos = pos;
        this.puck = puck;
        this.myIndex = myIndex;
    }

    @NotNull
    public static State of(@NotNull Hockeyist self, @NotNull World world) {
        List<HockeyistPosition> positions = new ArrayList<>(9);
        int myIndex = -1;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getId() == self.getId()) myIndex = positions.size();
            positions.add(HockeyistPosition.of(hockeyist));
        }
        assert myIndex >= 0 : "No self: " + Arrays.toString(world.getHockeyists());
        return new State(positions.toArray(new HockeyistPosition[positions.size()]), PuckPosition.of(world.getPuck()), myIndex);
    }

    @Nullable
    public Position enemyGoalie() {
        long opponentId = Players.opponent.getId();
        for (HockeyistPosition position : pos) {
            Hockeyist hockeyist = position.hockeyist;
            if (hockeyist.getPlayerId() == opponentId && hockeyist.getType() == HockeyistType.GOALIE) {
                return position;
            }
        }
        return null;
    }

    @NotNull
    public Position me() {
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
        PuckPosition newPuck = puck.move();
        return new State(positions, newPuck, myIndex);
    }

    @Override
    public String toString() {
        return "me at " + me();
    }
}
