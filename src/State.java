import model.Hockeyist;
import model.Unit;
import model.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class State {
    public static final Go DEFAULT_DIRECTION = Go.go(0, 0);

    public final Position[] pos;
    public final Unit[] unit;
    public final int myIndex;

    private State(@NotNull Position[] pos, @NotNull Unit[] unit, int myIndex) {
        this.pos = pos;
        this.unit = unit;
        this.myIndex = myIndex;
    }

    @NotNull
    public static State of(@NotNull Hockeyist self, @NotNull World world) {
        List<Position> positions = new ArrayList<>(9);
        List<Unit> units = new ArrayList<>(9);
        int myIndex = -1;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getId() == self.getId()) myIndex = units.size();
            positions.add(Position.of(hockeyist));
            units.add(hockeyist);
        }
        assert myIndex >= 0 : "No self: " + Arrays.toString(world.getHockeyists());
        return new State(positions.toArray(new Position[positions.size()]), units.toArray(new Unit[units.size()]), myIndex);
    }

    @NotNull
    public State apply(@NotNull Go go) {
        Position[] positions = Arrays.copyOf(pos, pos.length);
        Unit[] units = Arrays.copyOf(unit, unit.length);
        for (int i = 0, n = units.length; i < n; i++) {
            positions[i] = positions[i].move(i == myIndex ? go : DEFAULT_DIRECTION);
        }
        return new State(positions, units, myIndex);
    }
}
