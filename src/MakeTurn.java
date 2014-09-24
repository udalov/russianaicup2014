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

    @NotNull
    public Result makeTurn() {
        Player myPlayer = world.getMyPlayer();
        if (myPlayer.isJustScoredGoal() || myPlayer.isJustMissedGoal()) {
            return new Result(Do.STRIKE, Go.go(0.0, game.getHockeyistTurnAngleFactor()));
        }

        long puckOwnerId = puck.getOwnerHockeyistId();

        Decision decision = team.getDecision(self.getId());
        Decision.Role role = decision.role;

        if (role == Decision.Role.DEFENSE) {
            Point defensePoint = decision.defensePoint;

            if (self.getRemainingCooldownTicks() == 0 && puckOwnerId != -1) {
                Hockeyist puckOwner = findHockeyistById(puckOwnerId);
                // TODO: unhardcode
                if (!puckOwner.isTeammate() && defensePoint.distance(puckOwner) < 400) {
                    if (isPuckReachable()) {
                        // TODO: try TAKE_PUCK when the probability is high
                        return new Result(Do.STRIKE, goToUnit(puck));
                    } else {
                        return new Result(tryHitNearbyEnemies(), goToUnit(puck));
                    }
                }
            }

            // TODO: unhardcode
            if (defensePoint.sqrDist(self) > 1000) {
                return new Result(tryBlockPuck(), land(defensePoint));
            }

            if (defensePoint.sqrDist(self) > 100) {
                return new Result(tryBlockPuck(), Go.go(stop(), self.getAngleTo(puck)));
            }
            Point strikeTarget = whereEnemyWillStrike();
            Line strikeLine = Line.between(Point.of(puck), strikeTarget);
            Point catchAt = strikeLine.at(me.x);
            double angleToPuck = self.getAngleTo(puck);
            double angle = angleToPuck < -PI / 2 ? -PI / 2 :
                           angleToPuck > PI / 2 ? PI / 2 :
                           (self.getAngleTo(catchAt.x, catchAt.y) + angleToPuck) / 2;
            return new Result(tryBlockPuck(), Go.go(stop(), angle));
        } else {
            if (self.getState() == HockeyistState.SWINGING) {
                if (puckOwnerId != self.getId()) return new Result(Do.CANCEL_STRIKE, Go.go(stop(), 0));
                if (self.getSwingTicks() < 15 && safeToSwingMore()) {
                    return Result.SWING;
                } else {
                    Point goalPoint = determineGoalPoint();
                    return new Result(Do.STRIKE, Go.go(stop(), self.getAngleTo(goalPoint.x, goalPoint.y)));
                }
            }

            if (puckOwnerId == -1) {
                if (isPuckReachable()) {
                    return new Result(Do.TAKE_PUCK, Go.go(0, 0));
                } else {
                    return new Result(tryHitNearbyEnemies(), goToUnit(puck));
                }
            } else if (puckOwnerId != self.getId()) {
                return new Result(tryHitNearbyEnemies(), goToUnit(findHockeyistById(puckOwnerId)));
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

/*
                if (world.getTick() - team.lastGoalTick < 50) {
                    return new Result(Do.NONE, Go.go(1, game.getRandomSeed() % 2 == 0 ? -1 : 1));
                }
*/

                if (enemyHasNoGoalkeeper()) {
                    Player opponent = world.getOpponentPlayer();
                    Point target = Point.of(opponent.getNetFront(), (opponent.getNetTop() + opponent.getNetBottom()) / 2);
                    double angle = self.getAngleTo(target.x, target.y);
                    if (Math.abs(angle) < PI / 50 && abs(me.x - opponent.getNetFront()) > 100) {
                        return Result.SWING;
                    } else {
                        return new Result(Do.NONE, Go.go(stop(), angle));
                    }
                }

                Point attackPoint = determineAttackPoint();
                // TODO: unhardcode
                if (attackPoint.sqrDist(self) > 10000) {
                    State state = State.of(self, world);
                    double bestGoResult = Double.MIN_VALUE;
                    Go bestGo = null;
                    for (Go go : Arrays.asList(
                            Go.go(1, 1), Go.go(1, -1), Go.go(1, 0), Go.go(1, 0.5), Go.go(1, -0.5)
                            //TODO: ,Go.go(0, 1), Go.go(0, -1), Go.go(0, 0), Go.go(0, 0.5), Go.go(0, -0.5)
                            //TODO: ,Go.go(-1, 1), Go.go(-1, -1), Go.go(-1, 0), Go.go(-1, 0.5), Go.go(-1, -0.5)
                    )) {
                        double cur = evaluate(state, go, attackPoint);
                        if (bestGo == null || cur > bestGoResult) {
                            bestGoResult = cur;
                            bestGo = go;
                        }
                    }
                    assert bestGo != null;
                    return new Result(Do.NONE, bestGo);
/*
                    double angle = self.getAngleTo(attackPoint.x, attackPoint.y);
                    return new Result(Do.NONE, Go.go(abs(angle) < PI / 2 ? 1 : stop(), angle));
*/
                } else {
                    Point target = determineGoalPoint();
                    double angle = self.getAngleTo(target.x, target.y);
                    if (Math.abs(angle) < PI / 180) {
                        return Result.SWING;
                    } else {
                        return new Result(Do.NONE, Go.go(stop(), angle));
                    }
                }
            }
        }
    }

    private boolean safeToSwingMore() {
        if (speed(self) > 2) return false;
        for (Hockeyist enemy : world.getHockeyists()) {
            if (enemy.isTeammate() || enemy.getType() == HockeyistType.GOALIE) continue;
            if (isReachable(enemy, puck) || isReachable(enemy, self)) return false;
        }
        return true;
    }

    private boolean enemyHasNoGoalkeeper() {
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (!hockeyist.isTeammate() && hockeyist.getType() == HockeyistType.GOALIE) return false;
        }
        return true;
    }

    @NotNull
    private Go goToUnit(@NotNull Unit unit) {
        // TODO: unhardcode
        double speed = max(speed(self), 10);
        double distance = self.getDistanceTo(unit);
        Point futurePosition = Point.of(unit).shift(distance / speed * unit.getSpeedX(), distance / speed * unit.getSpeedY());
        double angle = self.getAngleTo(futurePosition.x, futurePosition.y);
        return Go.go(abs(angle) < PI / 2 ? 1 : stop(), angle);
    }

    @NotNull
    private Point determineAttackPoint() {
        // TODO: unhardcode
        double x = (game.getRinkLeft() + game.getRinkRight()) / 2 + team.attack * 272.0;

        boolean onTop = me.y < (game.getRinkTop() + game.getRinkBottom()) / 2;

        boolean onOurSideOfField = abs(me.x - world.getMyPlayer().getNetFront()) < abs(me.x - world.getOpponentPlayer().getNetFront());
        if (onOurSideOfField) {
            return Point.of(x, onTop ? game.getRinkTop() + self.getRadius() : game.getRinkBottom() - self.getRadius());
        }

        double y = onTop
                   ? game.getGoalNetTop() - game.getGoalNetHeight() / 6
                   : game.getGoalNetTop() + game.getGoalNetHeight() + game.getGoalNetHeight() / 6;

        return Point.of(x, y);
    }

    @NotNull
    private Point determineGoalPoint() {
        double x = (world.getOpponentPlayer().getNetFront() + world.getOpponentPlayer().getNetBack()) / 2;

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

    private double evaluate(@NotNull State currentState, @NotNull Go go, @NotNull Point attackPoint) {
        double score = 0;
        State state = currentState.apply(go);
        score += evaluate(state, attackPoint);

        for (int t = 2; t <= 10; t++) {
            state = state.apply(Go.go(0, 0));
            score += evaluate(state, attackPoint) / t / 2;
        }

        return score;
    }

    private double evaluate(@NotNull State state, @NotNull Point attackPoint) {
        double penalty = 0;

        Position myPosition = state.pos[state.myIndex];
        Point me = myPosition.point();
        double angle = myPosition.angle;

        penalty += me.distance(attackPoint);

        double dangerousAngle = PI / 2;

        for (int i = 0; i < state.unit.length; i++) {
            if (!(state.unit[i] instanceof Hockeyist)) continue;
            Hockeyist hockeyist = (Hockeyist) state.unit[i];
            if (hockeyist.isTeammate() || hockeyist.getType() == HockeyistType.GOALIE) continue;
            Position enemy = state.pos[i];

            double angleToEnemy = Util.angleDiff(angle, atan2(enemy.y - me.y, enemy.x - me.x));
            if (abs(angleToEnemy) > dangerousAngle) continue;

            double distance = me.distance(enemy.point());
            double convergenceSpeed = myPosition.speed().minus(enemy.speed()).length();
            if (distance > 150 && convergenceSpeed < 20) continue;

            if (distance < 150) penalty += sqrt(150 - distance);

            penalty += -150 / dangerousAngle * abs(angleToEnemy) + 150;
        }

        penalty += Util.sqr(max(game.getRinkLeft() - me.x, 0)) * 10;
        penalty += Util.sqr(max(me.x - game.getRinkRight(), 0)) * 10;
        penalty += Util.sqr(max(game.getRinkTop() - me.y, 0)) * 10;
        penalty += Util.sqr(max(me.y - game.getRinkBottom(), 0)) * 10;

        return -penalty;
    }

    @NotNull
    private Do tryHitNearbyEnemies() {
        if (self.getRemainingCooldownTicks() > 0) return Do.NONE;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() || hockeyist.getType() == HockeyistType.GOALIE) continue;
            if (isReachable(hockeyist)) return Do.STRIKE;
        }
        return Do.NONE;
    }

    @NotNull
    private Do tryBlockPuck() {
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
        return isReachable(self, unit);
    }

    private boolean isReachable(@NotNull Hockeyist from, @NotNull Unit unit) {
        double angle = from.getAngleTo(unit);
        return from.getDistanceTo(unit) <= game.getStickLength() &&
               -game.getStickSector() / 2 <= angle && angle <= game.getStickSector() / 2;
    }

    @NotNull
    private Point whereEnemyWillStrike() {
        double x = team.myStartingPlayer.getNetFront() - team.attack * game.getGoalNetWidth() / 2;
        double y = puck.getY() < (game.getRinkTop() + game.getRinkBottom()) / 2
                   ? game.getGoalNetTop()
                   : game.getGoalNetTop() + game.getGoalNetHeight();
        return Point.of(x, y);
    }

    private double stop() {
        if (speed(self) < 1.0) return 0.0;
        return Util.angleDiff(atan2(self.getSpeedY(), self.getSpeedX()), self.getAngle()) > PI / 2 ? 1.0 : -1.0;
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

    public static double speed(@NotNull Unit unit) {
        return Math.hypot(unit.getSpeedX(), unit.getSpeedY());
    }
}
