import model.*;

import static java.lang.StrictMath.PI;

public class MakeTurn {
    private final Team team;
    private final Hockeyist self;
    private final World world;
    private final Game game;

    private final Point me;
    private final Puck puck;

    public MakeTurn(@NotNull Team team, @NotNull Hockeyist self, @NotNull World world, @NotNull Game game) {
        this.team = team;
        this.self = self;
        this.world = world;
        this.game = game;

        this.me = Point.of(self);
        this.puck = world.getPuck();
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
            if (target.sqrDist(self) > 1000) {
                return new Result(tryPreventPuckFromGoingToGoal(), land(target));
            }
            Point strikeTarget = whereEnemyWillStrike();
            Line strikeLine = Line.between(Point.of(puck), strikeTarget);
            Point catchAt = strikeLine.at(me.x);
            // TODO: consider some average between this direction and the puck
            return new Result(tryPreventPuckFromGoingToGoal(), Go.go(0, self.getAngleTo(catchAt.x, catchAt.y)));
        } else {
            return new Result(Do.STRIKE, Go.go(0, 0));
        }
    }

    @NotNull
    private Do tryPreventPuckFromGoingToGoal() {
        if (self.getRemainingCooldownTicks() > 0) return Do.NONE;
        if (isPuckReachable()) {
            // TODO: something more clever, also take attributes into account
            return speed(puck) < 17 ? Do.TAKE_PUCK : Do.STRIKE;
        }
        return Do.NONE;
    }

    private boolean isPuckReachable() {
        double angle = self.getAngleTo(puck);
        return self.getDistanceTo(puck) <= game.getStickLength() &&
               -game.getStickSector() / 2 <= angle && angle <= game.getStickSector() / 2;
    }

    @NotNull
    private Point whereEnemyWillStrike() {
        double x = team.areWeOnTheLeft
                   ? game.getRinkLeft() - game.getGoalNetWidth() / 2
                   : game.getRinkRight() + game.getGoalNetWidth() / 2;
        double y = puck.getY() < (game.getRinkTop() + game.getRinkBottom()) / 2
                   ? game.getGoalNetTop()
                   : game.getGoalNetTop() + game.getGoalNetHeight();
        return Point.of(x, y);
    }

    @NotNull
    private Go land(@NotNull Point target) {
        double alpha = self.getAngleTo(target.x, target.y);
        double speed = speed(self);
        double distance = self.getDistanceTo(target.x, target.y);
        // TODO: consider raising this value to make defenders move back more frequently
        if (alpha > PI / 2 || alpha < -PI / 2) {
            return Go.go(distance < speed * speed / 2 ? 1 : -1, alpha > 0 ? alpha - PI : PI - alpha);
        } else {
            return Go.go(distance < speed * speed / 2 ? -1 : 1, alpha);
        }
    }

    private static double speed(@NotNull Unit unit) {
        return Math.hypot(unit.getSpeedX(), unit.getSpeedY());
    }
}
