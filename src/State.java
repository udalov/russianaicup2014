import model.Hockeyist;
import model.HockeyistType;
import model.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class State {
    public static final Go DEFAULT_DIRECTION = Go.go(0, 0);

    public final Position[] pos;
    public final Hockeyist[] hockeyists;
    public final Position puck;
    public final int myIndex;

    public State(@NotNull Position[] pos, @NotNull Hockeyist[] hockeyists, @NotNull Position puck, int myIndex) {
        this.pos = pos;
        this.hockeyists = hockeyists;
        this.puck = puck;
        this.myIndex = myIndex;
    }

    @NotNull
    public static State of(@NotNull Hockeyist self, @NotNull World world) {
        List<Position> positions = new ArrayList<>(9);
        List<Hockeyist> hockeyists = new ArrayList<>(9);
        int myIndex = -1;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getId() == self.getId()) myIndex = hockeyists.size();
            positions.add(Position.of(hockeyist));
            hockeyists.add(hockeyist);
        }
        assert myIndex >= 0 : "No self: " + Arrays.toString(world.getHockeyists());
        return new State(positions.toArray(new Position[positions.size()]), hockeyists.toArray(new Hockeyist[hockeyists.size()]),
                         Position.of(world.getPuck()), myIndex);
    }

    @Nullable
    public Position enemyGoalie() {
        long opponentId = Players.opponent.getId();
        for (int i = 0; i < hockeyists.length; i++) {
            Hockeyist hockeyist = this.hockeyists[i];
            if (hockeyist.getPlayerId() == opponentId && hockeyist.getType() == HockeyistType.GOALIE) {
                return pos[i];
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
        return hockeyists[myIndex];
    }

    @NotNull
    public State apply(@NotNull Go go) {
        Position[] positions = Arrays.copyOf(pos, pos.length);
        Hockeyist[] hockeyists = Arrays.copyOf(this.hockeyists, this.hockeyists.length);
        for (int i = 0, n = hockeyists.length; i < n; i++) {
            positions[i] = positions[i].move(i == myIndex ? go : DEFAULT_DIRECTION, 0.02);
        }
        Position newPuck = puck.move(DEFAULT_DIRECTION, 0.001);
        return new State(positions, hockeyists, newPuck, myIndex);
    }

    @Override
    public String toString() {
        return "me at " + me();
    }
}
