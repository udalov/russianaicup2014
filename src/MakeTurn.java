import model.*;

import java.util.Arrays;

import static java.lang.StrictMath.*;

public class MakeTurn {
    private final Team team;
    private final Hockeyist self;
    private final World world;

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"}) // Maybe use randomSeed
    private final Game game;

    private final Point me;
    private final Puck puck;
    private final Player myPlayer;
    private final Player opponentPlayer;

    public MakeTurn(@NotNull Team team, @NotNull Hockeyist self, @NotNull World world, @NotNull Game game) {
        this.team = team;
        this.self = self;
        this.world = world;
        this.game = game;

        this.me = Point.of(self);
        this.puck = world.getPuck();
        this.myPlayer = world.getMyPlayer();
        this.opponentPlayer = world.getOpponentPlayer();
    }

    @NotNull
    public Result makeTurn() {
        if (self.getRemainingKnockdownTicks() > 0) return new Result(Do.NONE, Go.go(0, 0));

        if (myPlayer.isJustScoredGoal()) return winningDance();
        if (myPlayer.isJustMissedGoal()) return losingDance();

        long puckOwnerId = puck.getOwnerHockeyistId();

        Decision decision = team.getDecision(self.getId());
        Decision.Role role = decision.role;

        if (role == Decision.Role.DEFENSE) {
            Point defensePoint = decision.defensePoint;

            // TODO: unhardcode
            if (defensePoint.distance(self) > 300) {
                return new Result(tryBlockPuck(), land(defensePoint));
            }

            if (self.getRemainingCooldownTicks() == 0) {
                if (puckOwnerId == -1 || puck.getOwnerPlayerId() != myPlayer.getId()) {
                    double distanceToPuck = defensePoint.distance(puck);
                    double puckSpeedAgainstOurGoal = Vec.velocity(puck).projection(Vec.of(-team.attack, 0));
                    // TODO: unhardcode
                    if (distanceToPuck < 400 || (puckSpeedAgainstOurGoal > 3 && (abs(myPlayer.getNetFront() - puck.getX()) < 700))) {
                        return new Result(tryHitNearbyEnemiesOrPuck(), goToUnit(puck));
                    }
                }
            }

            // TODO: unhardcode
            if (defensePoint.distance(self) > 32) {
                return new Result(tryBlockPuck(), land(defensePoint));
            }

            if (defensePoint.distance(self) > 10) {
                return new Result(tryBlockPuck(), Go.go(stop(), self.getAngleTo(puck)));
            }
            Point strikeTarget = determineGoalPoint(team.myStartingPlayer);
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
                    Point goalPoint = determineGoalPoint(opponentPlayer);
                    return new Result(Do.STRIKE, Go.go(stop(), self.getAngleTo(goalPoint.x, goalPoint.y)));
                }
            }

            if (puckOwnerId == -1) {
                if (isReachable(puck)) {
                    return new Result(Do.TAKE_PUCK, Go.go(0, 0));
                } else {
                    return new Result(tryHitNearbyEnemiesOrPuck(), goToUnit(puck));
                }
            } else if (puckOwnerId != self.getId()) {
                return new Result(tryHitNearbyEnemiesOrPuck(), goToUnit(puck));
            } else {
                if (enemyHasNoGoalkeeper()) {
                    Point target = Point.of(opponentPlayer.getNetFront(), Static.CENTER.y);
                    double angle = self.getAngleTo(target.x, target.y);
                    if (Math.abs(angle) < PI / 50 && abs(me.x - opponentPlayer.getNetFront()) > 100) {
                        return Result.SWING;
                    } else {
                        return new Result(Do.NONE, Go.go(stop(), angle));
                    }
                }

                if (abs(self.getAngle()) > 3 * PI / 4 && team.attack == 1 ||
                    abs(self.getAngle()) < PI / 4 && team.attack == -1) {
                    Do pass = tryPass();
                    if (pass != null) return new Result(pass, Go.go(0, 0));
                }

                Point attackPoint = determineAttackPoint();
                // TODO: unhardcode
                if (attackPoint.distance(self) > 100) {
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
                    Point target = determineGoalPoint(opponentPlayer);
                    double angle = self.getAngleTo(target.x, target.y);
                    if (abs(angle) < PI / 180) {
                        return Result.SWING;
                    } else if (-Const.passSector / 5 < abs(angle) && abs(angle) < Const.passSector / 5) {
                        return new Result(Do.pass(1, angle), Go.go(stop(), angle));
                    } else {
                        return new Result(Do.NONE, Go.go(stop(), angle));
                    }
                }
            }
        }
    }

    @NotNull
    private Result losingDance() {
        Hockeyist closestEnemy = null;
        double bestDistance = Double.MAX_VALUE;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() || hockeyist.getType() == HockeyistType.GOALIE) continue;
            double cur = self.getDistanceTo(hockeyist);
            if (cur < bestDistance) {
                bestDistance = cur;
                closestEnemy = hockeyist;
            }
        }
        if (closestEnemy == null) return new Result(Do.NONE, Go.go(0, Const.hockeyistTurnAngleFactor));
        return new Result(isReachable(closestEnemy) ? Do.STRIKE : Do.NONE, Go.go(1, self.getAngleTo(closestEnemy)));
    }

    @NotNull
    private static Result winningDance() {
        return new Result(Do.NONE, Go.go(0, Const.hockeyistTurnAngleFactor));
    }

    @Nullable
    private Do tryPass() {
        if (self.getRemainingCooldownTicks() > 0) return null;
        Vec myDirection = Vec.direction(self);
        for (Hockeyist ally : world.getHockeyists()) {
            if (!ally.isTeammate() || ally.getType() == HockeyistType.GOALIE || ally.getId() == self.getId()) continue;
            double angle = self.getAngleTo(ally);
            if (-game.getPassSector() / 2 < angle && angle < game.getPassSector() / 2) {
                if (abs(myDirection.angleTo(Vec.direction(ally))) > 2 * PI / 3) {
                    return Do.pass(min(400.0 / self.getDistanceTo(ally), 1.0), angle);
                }
            }
        }
        return null;
    }

    private boolean safeToSwingMore() {
        if (Util.speed(self) > 4) return false;
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
        double speed = max(Util.speed(self), 10);
        double distance = self.getDistanceTo(unit);
        Vec movement = Vec.velocity(unit).multiply(distance / speed);
        Point futurePosition = Point.of(unit).shift(movement);
        double angle = self.getAngleTo(futurePosition.x, futurePosition.y);
        return Go.go(abs(angle) < PI / 2 ? 1 : stop(), angle);
    }

    @NotNull
    private Point determineAttackPoint() {
        // TODO: unhardcode
        double x = Static.CENTER.x + team.attack * 272.0;

        double y = me.y < Static.CENTER.y
                   ? opponentPlayer.getNetTop() - Const.goalNetHeight / 6
                   : opponentPlayer.getNetBottom() + Const.goalNetHeight / 6;

        return Point.of(x, y);
    }

    private static double evaluate(@NotNull State currentState, @NotNull Go go, @NotNull Point attackPoint) {
        double score = 0;
        State state = currentState.apply(go);
        score += evaluate(state, attackPoint);

        for (int t = 2; t <= 10; t++) {
            state = state.apply(Go.go(0, 0));
            score += evaluate(state, attackPoint) / t / 2;
        }

        return score;
    }

    private static double evaluate(@NotNull State state, @NotNull Point attackPoint) {
        double penalty = 0;

        Position myPosition = state.pos[state.myIndex];
        Vec myVelocity = myPosition.velocity();
        Point me = myPosition.point();
        Vec myDirection = myPosition.direction();

        penalty += me.distance(attackPoint);

        double dangerousAngle = PI / 2;

        for (int i = 0; i < state.unit.length; i++) {
            if (!(state.unit[i] instanceof Hockeyist)) continue;
            Hockeyist hockeyist = (Hockeyist) state.unit[i];
            if (hockeyist.getId() == state.self().getId() || hockeyist.getType() == HockeyistType.GOALIE) continue;
            Position enemy = state.pos[i];

            double angleToEnemy = abs(myPosition.direction().angleTo(Vec.of(me, enemy.point())));
            if (angleToEnemy > dangerousAngle) continue;

            double distance = me.distance(enemy.point());
            double convergenceSpeed = myVelocity.length() < 1e-6 ? 0 : 1 - enemy.velocity().projection(myVelocity);
            if (distance > 150 && convergenceSpeed < 20) continue;

            if (!hockeyist.isTeammate() && distance < 150) penalty += sqrt(150 - distance);

            penalty += (hockeyist.isTeammate() ? 30 : 150) * (1 - abs(angleToEnemy) / dangerousAngle);
        }

        Point future = me.shift(myDirection.multiply(10));
        penalty += Util.sqr(max(Const.rinkLeft - future.x, 0)) * 10;
        penalty += Util.sqr(max(future.x - Const.rinkRight, 0)) * 10;
        penalty += Util.sqr(max(Const.rinkTop - future.y, 0)) * 10;
        penalty += Util.sqr(max(future.y - Const.rinkBottom, 0)) * 10;

        // penalty += pow(max(15 - myVelocity.project(myDirection).length(), 0), 1.1);

        for (Point corner : Static.CORNERS) {
            // TODO: investigate if it works as expected
            penalty += Util.sqr(max(150 - me.distance(corner), 0));
        }

        return -penalty;
    }

    @NotNull
    private Do tryHitNearbyEnemiesOrPuck() {
        if (self.getRemainingCooldownTicks() > 0) return Do.NONE;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() || hockeyist.getType() == HockeyistType.GOALIE) continue;
            if (isReachable(hockeyist)) return Do.STRIKE;
        }
        if (isReachable(puck)) {
            // TODO: something more clever, also take attributes into account
            return Util.speed(puck) < 17 && puck.getOwnerHockeyistId() == -1 ? Do.TAKE_PUCK : Do.STRIKE;
        }
        return Do.NONE;
    }

    @NotNull
    private Do tryBlockPuck() {
        if (self.getRemainingCooldownTicks() > 0) return Do.NONE;
        if (isReachable(puck)) {
            // TODO: something more clever, also take attributes into account
            return Util.speed(puck) < 17 ? Do.TAKE_PUCK : Do.STRIKE;
        }
        return Do.NONE;
    }

    private boolean isReachable(@NotNull Unit unit) {
        return isReachable(self, unit);
    }

    private static boolean isReachable(@NotNull Hockeyist from, @NotNull Unit unit) {
        double angle = from.getAngleTo(unit);
        return from.getDistanceTo(unit) <= Const.stickLength &&
               -Const.stickSector / 2 <= angle && angle <= Const.stickSector / 2;
    }

    @NotNull
    private Point determineGoalPoint(@NotNull Player defendingPlayer) {
        double x = defendingPlayer.getNetFront();
        double y = puck.getY() < Static.CENTER.y
                   ? defendingPlayer.getNetBottom() + puck.getRadius()
                   : defendingPlayer.getNetTop() - puck.getRadius();
        return Point.of(x, y);
    }

    private double stop() {
        if (Util.speed(self) < 1.0) return 0.0;
        return abs(Vec.velocity(self).angleTo(Vec.direction(self))) > PI / 2 ? 1.0 : -1.0;
    }

    @NotNull
    private Go land(@NotNull Point target) {
        double alpha = self.getAngleTo(target.x, target.y);
        double distance = self.getDistanceTo(target.x, target.y);
        double speed = Util.speed(self);

        boolean closeBy = distance < speed * speed / 2;

        // TODO: unhardcode
        double eps = 2 * Const.hockeyistTurnAngleFactor;

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
}
