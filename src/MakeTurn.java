import model.Game;
import model.Hockeyist;
import model.World;

public class MakeTurn {
    private final Team team;
    private final Hockeyist self;
    private final World world;
    private final Game game;

    public MakeTurn(@NotNull Team team, @NotNull Hockeyist self, @NotNull World world, @NotNull Game game) {
        this.team = team;
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
        Decision decision = team.getDecision(self.getId());
        Decision.Role role = decision.role;

        if (role == Decision.Role.DEFENSE) {
            Point target = decision.defensePoint;
            if (target.sqrDist(self) > 4000) {
                return new Result(Do.STRIKE, Go.go(1, self.getAngleTo(target.x, target.y)));
            }
            return new Result(Do.STRIKE, Go.go(0, self.getAngleTo(world.getPuck())));
        }
        else {
            return new Result(Do.STRIKE, Go.go(1, 0));
        }
    }
}
