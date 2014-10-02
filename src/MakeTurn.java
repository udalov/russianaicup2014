import model.*;

import java.util.ArrayList;
import java.util.Collection;

import static java.lang.StrictMath.*;

public class MakeTurn {
    private final Team team;
    private final Hockeyist self;
    private final World world;

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"}) // Maybe use randomSeed
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
        if (self.getRemainingKnockdownTicks() > 0) return new Result(Do.NONE, Go.go(0, 0));

        if (world.getMyPlayer().isJustScoredGoal()) return winningDance();
        if (world.getMyPlayer().isJustMissedGoal()) return losingDance();

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
                if (puckOwnerId == -1 || puck.getOwnerPlayerId() != Players.me.getId()) {
                    double distanceToPuck = defensePoint.distance(puck);
                    double puckSpeedAgainstOurGoal = Vec.velocity(puck).projection(Players.defense);
                    // TODO: unhardcode
                    if (distanceToPuck < 400 || (puckSpeedAgainstOurGoal > 3 && (abs(Players.me.getNetFront() - puck.getX()) < 700))) {
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
            Point strikeTarget = determineGoalPoint(Players.me);
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
                if (self.getSwingTicks() < 20 && safeToSwingMore()) {
                    return Result.SWING;
                } else {
                    Point goalPoint = determineGoalPoint(Players.opponent);
                    double angle = self.getAngleTo(goalPoint.x, goalPoint.y);
                    if (abs(angle) > 8 * PI / 180) {
                        return new Result(Do.CANCEL_STRIKE, Go.go(stop(), 0));
                    } else {
                        return new Result(Do.STRIKE, Go.go(stop(), angle));
                    }
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
                    Point target = Point.of(Players.opponent.getNetFront(), Static.CENTER.y);
                    double angle = self.getAngleTo(target.x, target.y);
                    if (Math.abs(angle) < PI / 50 && abs(me.x - Players.opponent.getNetFront()) > 100) {
                        return Result.SWING;
                    } else {
                        return new Result(Do.NONE, Go.go(stop(), angle));
                    }
                }

                if (abs(self.getAngle()) > 3 * PI / 4 && Players.attack.x == 1 ||
                    abs(self.getAngle()) < PI / 4 && Players.attack.x == -1) {
                    Do pass = tryPass();
                    if (pass != null) return new Result(pass, Go.go(0, 0));
                }

                State state = State.of(self, world);
                Point target = determineGoalPoint(Players.opponent);
                double angle = self.getAngleTo(target.x, target.y);
                Point attackPoint = determineAttackPoint();
                if (abs(angle) < Const.passSector / 5 && probabilityToScore(state, 0.75) > 0.85) {
                    return new Result(Do.pass(1, angle), Go.go(stop(), angle));
                } else if (me.distance(attackPoint) < 100 && shouldStartSwinging(state)) {
                    return Result.SWING;
                } else {
                    double bestGoResult = Double.MIN_VALUE;
                    Go bestGo = null;
                    for (Go go : iteratePossibleMoves()) {
                        double cur = evaluate(state, go, attackPoint);
                        if (bestGo == null || cur > bestGoResult) {
                            bestGoResult = cur;
                            bestGo = go;
                        }
                    }
                    assert bestGo != null;
                    return new Result(Do.NONE, bestGo);
                }
            }
        }
    }

    // TODO: use
    private static boolean shouldStartSwinging(@NotNull State state) {
        for (int i = 0; i < 20; i++) {
            state = state.apply(Go.go(0, 0));
        }
        return /*probabilityToScore(state, 1) > 0.75 && */angleDifferenceToOptimal(state) < 3 * PI / 180;
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
    private Collection<Go> iteratePossibleMoves() {
        double d = Const.hockeyistTurnAngleFactor * Util.effectiveAttribute(self, self.getAgility());
        Collection<Go> result = new ArrayList<>(51);
        for (int speedup = -1; speedup <= 1; speedup++) {
            for (double turn = -d; turn <= d + 1e-6; turn += d / 8) {
                result.add(Go.go(speedup, turn));
            }
        }

        return result;
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
        double x = Static.CENTER.x + Players.attack.x * 150;

        double y = me.y < Static.CENTER.y ? Const.rinkTop + 50 : Const.rinkBottom - 50;

        return Point.of(x, y);
    }

    private static double evaluate(@NotNull State currentState, @NotNull Go go, @NotNull Point attackPoint) {
        double score = 0;
        State state = currentState.apply(go);
        score += evaluate(state, attackPoint);

        for (int t = 2; t <= 10; t++) {
            state = state.apply(go);
            score += evaluate(state, attackPoint) / t / 2;
        }

        return score;
    }

    private static double evaluate(@NotNull State state, @NotNull Point attackPoint) {
        double penalty = 0;

        Position myPosition = state.me();
        Vec myVelocity = myPosition.velocity();
        Point me = myPosition.point();
        Vec myDirection = myPosition.direction();

        penalty += me.distance(attackPoint) / 100;

        double dangerousAngle = PI / 2;

        for (HockeyistPosition enemy : state.pos) {
            Hockeyist hockeyist = enemy.hockeyist;
            if (hockeyist.getId() == state.self().getId() || hockeyist.getType() == HockeyistType.GOALIE) continue;

            double distance = me.distance(enemy.point());
            if (!hockeyist.isTeammate() && distance < 150) penalty += sqrt(150 - distance);

            double angleToEnemy = abs(myPosition.direction().angleTo(Vec.of(me, enemy.point())));
            if (angleToEnemy > dangerousAngle) continue;

            double convergenceSpeed = myVelocity.length() < 1e-6 ? 0 : 1 - enemy.velocity().projection(myVelocity);
            if (distance > 150 && convergenceSpeed < 20) continue;

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

        if (me.distance(attackPoint) < 100) {
            penalty -= pow(1 - angleDifferenceAfterSwing(state) / PI, 20) * 1000;
            penalty += max(myVelocity.length() - 3.5, 0) * 10; // TODO: correctly determine my effective speed
        } else {
            penalty -= 1000;
            penalty += 20;
        }

        penalty += max(abs(myDirection.angleTo(myVelocity)) - PI / 2, 0) * 50;

/*
        Position enemyGoalie = state.enemyGoalie();
        if (enemyGoalie != null) penalty += max(200 - enemyGoalie.point().distance(me), 0);

        // TODO: angle diff should be very small for non-0
        penalty -= Util.sqr(probabilityToScore(state, 1) * pow(1 - angleDifferenceToOptimal(state, myDirection) / PI, 4)) * 200;
*/

        return -penalty;
    }

    public static double probabilityToScore(@NotNull State state, double strikePower) {
        double result = 1;

        Position position = state.me();
        Vec velocity = position.velocity();

        Position goalie = state.enemyGoalie();
        if (goalie != null) {
            Point puck = state.puck.point();

            Point goalNetNearby = Players.opponentNearbyCorner(puck);
            Point goalNetDistant = Players.opponentDistantCorner(puck);
            Vec verticalMovement = Vec.of(goalNetNearby, goalNetDistant).normalize();
            Point target = goalNetDistant.shift(verticalMovement.multiply(-Static.PUCK_RADIUS));
            Vec trajectory = Vec.of(puck, target);

            if (abs(puck.x - target.x) <= 2 * Static.HOCKEYIST_RADIUS) result *= 0;

            Vec goalieHorizontalShift = Players.attack.multiply(-Static.HOCKEYIST_RADIUS);
            Point goalieNearby = goalNetNearby.shift(verticalMovement.multiply(Static.HOCKEYIST_RADIUS)).shift(goalieHorizontalShift);
            Point goalieDistant = goalNetDistant.shift(verticalMovement.multiply(-Static.HOCKEYIST_RADIUS)).shift(goalieHorizontalShift);

            // TODO: attributes and condition
            double puckSpeed = Const.struckPuckInitialSpeedFactor * strikePower + velocity.length() * cos(position.angle - velocity.angle());

            boolean withinGoalieReach = min(goalieNearby.y, goalieDistant.y) <= puck.y && puck.y <= max(goalieNearby.y, goalieDistant.y);
            double puckStartY = (withinGoalieReach ? puck.y : goalieNearby.y) - puckSpeed * sin(trajectory.angleTo(verticalMovement));
            Line line = Line.between(puck, target);
            Point puckStart = line.when(puckStartY);

            // TODO: friction
            double time = puckStart.distance(target) / puckSpeed;
            Point goalieFinish = goalie.point().shift(verticalMovement.multiply(time * Const.goalieMaxSpeed));

            // Now we should check if distance between the following segments is >= radius(puck) + radius(goalie):
            // (goalie, goalieFinish) and (puckStart, target)
            boolean intersects = signum(Vec.of(puck, goalieNearby).crossProduct(trajectory)) !=
                                 signum(Vec.of(puck, goalieFinish).crossProduct(trajectory));
            if (intersects) result *= 0;

            result *= min(1, line.project(goalieFinish).distance(goalieFinish) / (Static.HOCKEYIST_RADIUS + Static.PUCK_RADIUS));
        }

        return result;
    }

    // TODO: (!) handle overtime with no goalies
    private static double angleDifferenceToOptimal(@NotNull State state) {
        Point puck = state.puck.point();
        Point goalNetNearby = Players.opponentNearbyCorner(puck);
        Point goalNetDistant = Players.opponentDistantCorner(puck);
        Vec verticalMovement = Vec.of(goalNetNearby, goalNetDistant).normalize();
        Point target = goalNetDistant.shift(verticalMovement.multiply(-Static.PUCK_RADIUS));
        Vec trajectory = Vec.of(puck, target);
        return abs(state.me().direction().angleTo(trajectory));
    }

    private static double angleDifferenceAfterSwing(@NotNull State state) {
        for (int i = 0; i < 20; i++) {
            state = state.apply(Go.go(0, 0));
        }
        return angleDifferenceToOptimal(state);
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
                   ? defendingPlayer.getNetBottom() + Static.PUCK_RADIUS
                   : defendingPlayer.getNetTop() - Static.PUCK_RADIUS;
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
