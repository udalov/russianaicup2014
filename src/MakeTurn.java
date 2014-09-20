import model.*;

import java.util.Arrays;

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
        Player myPlayer = world.getMyPlayer();
        if (myPlayer.isJustScoredGoal() || myPlayer.isJustMissedGoal()) {
            return new Result(Do.STRIKE, Go.go(0.0, game.getHockeyistTurnAngleFactor()));
        }

        Decision decision = team.getDecision(self.getId());
        Decision.Role role = decision.role;

        if (role == Decision.Role.DEFENSE) {
            Point target = decision.defensePoint;
            // TODO: unhardcode
            if (target.sqrDist(self) > 1000) {
                return new Result(tryPreventPuckFromGoingToGoal(), land(target));
            }
            Point strikeTarget = whereEnemyWillStrike();
            Line strikeLine = Line.between(Point.of(puck), strikeTarget);
            Point catchAt = strikeLine.at(me.x);
            // TODO: consider some average between this direction and the puck
            return new Result(tryPreventPuckFromGoingToGoal(), Go.go(0, self.getAngleTo(catchAt.x, catchAt.y)));
        } else {
            long ownerId = puck.getOwnerHockeyistId();
            if (self.getState() == HockeyistState.SWINGING) {
                return new Result(ownerId == self.getId() ? Do.STRIKE : Do.CANCEL_STRIKE, land(me));
            }

            if (ownerId == -1) {
                if (isPuckReachable()) {
                    return new Result(Do.TAKE_PUCK, Go.go(0, 0));
                } else {
                    // TODO: unhardcode
                    Point nextPuck = Point.of(puck).shift(2 * puck.getSpeedX(), 2 * puck.getSpeedY());
                    return new Result(Do.NONE, Go.go(1, self.getAngleTo(nextPuck.x, nextPuck.y)));
                }
            } else if (ownerId != self.getId()) {
                Hockeyist owner = findHockeyistById(ownerId);
                return new Result(tryHitPuckOwner(owner), Go.go(1, self.getAngleTo(owner)));
            } else {
                Point attackPoint = determineAttackPoint();
                // TODO: unhardcode
                if (attackPoint.sqrDist(self) > 10000) {
                    return new Result(Do.NONE, Go.go(1, self.getAngleTo(attackPoint.x, attackPoint.y)));
                } else {
                    Point target = determineGoalPoint();
                    double angle = self.getAngleTo(target.x, target.y);
                    if (Math.abs(angle) < PI / 180) {
                        return new Result(Do.SWING, Go.go(0, 0) /* TODO: more accurate */);
                    } else {
                        return new Result(Do.NONE, Go.go(0, angle));
                    }
                }
            }
        }
    }

    @NotNull
    private Point determineAttackPoint() {
        // TODO: unhardcode
        double x = 600.0 + team.attack * 272.0;

        double y = me.y < (game.getRinkTop() + game.getRinkBottom()) / 2
                   ? game.getGoalNetTop()
                   : game.getGoalNetTop() + game.getGoalNetHeight();

        return Point.of(x, y);
    }

    @NotNull
    private Point determineGoalPoint() {
        double x = world.getOpponentPlayer().getNetFront() + team.attack * game.getGoalNetWidth() / 2;

        double y = me.y < (game.getRinkTop() + game.getRinkBottom()) / 2
                   ? game.getGoalNetTop() + game.getGoalNetHeight()
                   : game.getGoalNetTop();

        return Point.of(x, y);
    }

    @NotNull
    private Hockeyist findHockeyistById(long id) {
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getId() == id) return hockeyist;
        }
        throw new AssertionError("Invisible hockeyist: " + id + ", world: " + Arrays.toString(world.getHockeyists()));
    }

    @NotNull
    private Do tryHitPuckOwner(@NotNull Hockeyist owner) {
        if (self.getRemainingCooldownTicks() > 0) return Do.NONE;
        if (owner.isTeammate()) return Do.NONE;
        if (owner.getType() == HockeyistType.GOALIE) return Do.NONE;
        return isReachable(owner) ? Do.STRIKE : Do.NONE;
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
        return isReachable(puck);
    }

    private boolean isReachable(@NotNull Unit unit) {
        double angle = self.getAngleTo(unit);
        return self.getDistanceTo(unit) <= game.getStickLength() &&
               -game.getStickSector() / 2 <= angle && angle <= game.getStickSector() / 2;
    }

    @NotNull
    private Point whereEnemyWillStrike() {
        double x = team.myPlayer.getNetFront() - team.attack * game.getGoalNetWidth() / 2;
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
