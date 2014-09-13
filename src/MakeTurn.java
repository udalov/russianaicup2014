import model.Game;
import model.Hockeyist;
import model.World;

import static java.lang.StrictMath.PI;

public class MakeTurn {
    private final Hockeyist self;
    private final World world;
    private final Game game;

    public MakeTurn(@NotNull Hockeyist self, @NotNull World world, @NotNull Game game) {
        this.self = self;
        this.world = world;
        this.game = game;
    }

    public static class Result {
        public final Do action;
        public final Go direction;

        public Result(@NotNull Do action, @NotNull Go direction) {
            this.action = action;
            this.direction = direction;
        }
    }

    @NotNull
    public Result makeTurn() {
        return new Result(Do.STRIKE, Go.go(-1.0, PI));
    }
}
