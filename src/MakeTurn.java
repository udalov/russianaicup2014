import model.*;

import java.util.Arrays;

import static java.lang.StrictMath.*;

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
            else if (target.sqrDist(self) > 100) {
                return new Result(tryPreventPuckFromGoingToGoal(), Go.go(stop(), self.getAngleTo(puck)));
            }
            Point strikeTarget = whereEnemyWillStrike();
            Line strikeLine = Line.between(Point.of(puck), strikeTarget);
            Point catchAt = strikeLine.at(me.x);
            double angleToPuck = self.getAngleTo(puck);
            double angle = angleToPuck < -PI / 2 ? -PI / 2 :
                           angleToPuck > PI / 2 ? PI / 2 :
                           (self.getAngleTo(catchAt.x, catchAt.y) + angleToPuck) / 2;
            return new Result(tryPreventPuckFromGoingToGoal(), Go.go(stop(), angle));
        } else {
            long ownerId = puck.getOwnerHockeyistId();
            if (self.getState() == HockeyistState.SWINGING) {
                return new Result(ownerId == self.getId() ? Do.STRIKE : Do.CANCEL_STRIKE, Go.go(stop(), 0));
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
/*
                for (Hockeyist hockeyist : world.getHockeyists()) {
                    if (!hockeyist.isTeammate() &&
                        hockeyist.getRemainingCooldownTicks() < 5 &&
                        hockeyist.getRemainingKnockdownTicks() < 5 &&
                        self.getDistanceTo(hockeyist) < self.getRadius() + hockeyist.getRadius() + 10) {
                        return new Result(Do.STRIKE, Go.go(1, 0));
                    }
                }
*/
                if (world.getTick() < 50) {
                    return new Result(Do.NONE, Go.go(1, game.getRandomSeed() % 2 == 0 ? -1 : 1));
                }

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
        double x = (game.getRinkLeft() + game.getRinkRight()) / 2 + team.attack * 272.0;

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

    private double stop() {
        if (speed(self) < 1.0) return 0.0;
        return angleDiff(atan2(self.getSpeedY(), self.getSpeedX()), self.getAngle()) > PI / 2 ? 1.0 : -1.0;
    }

    @NotNull
    private Go land(@NotNull Point target) {
        double alpha = self.getAngleTo(target.x, target.y);
        double distance = self.getDistanceTo(target.x, target.y);
        double speed = speed(self);

        boolean closeBy = distance < speed * speed / 2;

        // TODO: unhardcode
        double eps = 2 * game.getHockeyistTurnAngleFactor();

        // TODO: unhardcode
        if (abs(alpha) < PI / 2) {
            // The target is ahead, moving forward
            if (abs(alpha) < eps) {
                // Keep moving forward, accelerate or slow down depending on the distance
                return Go.go(closeBy ? -1 : 1, alpha);
            }
            // Else lower our speed if needed and turn to the target
            return Go.go(speed < 1.0 ? 0.0 : -1, alpha);
        } else {
            double turn = alpha > 0 ? alpha - PI : PI - alpha;
            if (abs(PI - abs(alpha)) < eps) {
                return Go.go(closeBy ? 1 : -1, turn);
            }
            return Go.go(speed < 1.0 ? 0.0 : 1, turn);
        }
    }

    private static double angleDiff(double a, double b) {
        return abs(atan2(sin(a - b), cos(a - b)));
    }

    public static double speed(@NotNull Unit unit) {
        return Math.hypot(unit.getSpeedX(), unit.getSpeedY());
    }
}
