import model.*;

import java.util.ArrayList;
import java.util.Collection;

import static java.lang.StrictMath.*;

public class MakeTurn {
    private final Team team;
    private final Hockeyist self;
    private final World world;

    private final Puck puck;

    private final State current;
    private final HockeyistPosition me;

    public MakeTurn(@NotNull Team team, @NotNull Hockeyist self, @NotNull World world) {
        this.team = team;
        this.self = self;
        this.world = world;
        this.puck = world.getPuck();

        this.current = State.of(self, world);
        this.me = current.me();
    }

    @NotNull
    public Result makeTurn() {
        if (self.getRemainingKnockdownTicks() > 0) return new Result(Do.NONE, Go.go(0, 0));

        if (world.getMyPlayer().isJustScoredGoal()) return winningDance();
        if (world.getMyPlayer().isJustMissedGoal()) return losingDance();

        long puckOwnerId = puck.getOwnerHockeyistId();

        Decision decision = team.getDecision(self.getId());
        Decision.Role role = decision.role;

        if (role == Decision.Role.DEFENSE || role == Decision.Role.MIDFIELD) {
            if (puck.getOwnerPlayerId() == Players.me.getId()) {
                return whatToDoWithPuck();
            }

            Point defensePoint = decision.defensePoint;

            // TODO: unhardcode
            if (me.distance(defensePoint) > 300) {
                return new Result(tryBlockPuck(), land(defensePoint));
            }

            if (self.getRemainingCooldownTicks() == 0) {
                if (puckOwnerId == -1 || puck.getOwnerPlayerId() != Players.me.getId()) {
                    double distanceToPuck = defensePoint.distance(puck);
                    double puckSpeedAgainstOurGoal = Vec.velocity(puck).projection(Players.defense);
                    // TODO: unhardcode
                    if (distanceToPuck < 400 || (puckSpeedAgainstOurGoal > 3 && (abs(Players.me.getNetFront() - puck.getX()) < 700))) {
                        return new Result(tryHitNearbyEnemiesOrPuck(), goToPuck());
                    }
                }
            }

            // TODO: unhardcode
            if (me.distance(defensePoint) > 32) {
                return new Result(tryBlockPuck(), land(defensePoint));
            }

            if (me.distance(defensePoint) > 10) {
                return new Result(tryBlockPuck(), Go.go(stop(), me.angleTo(puck)));
            }

            Point strikeTarget = determineGoalPoint(Players.me);
            Line strikeLine = Line.between(Point.of(puck), strikeTarget);
            double angleToPuck = me.angleTo(puck);
            double angle = angleToPuck < -PI / 2 ? -PI / 2 :
                           angleToPuck > PI / 2 ? PI / 2 :
                           (me.angleTo(strikeLine.at(me.point.x)) + angleToPuck) / 2;
            return new Result(tryBlockPuck(), Go.go(stop(), angle));
        } else {
            if (self.getState() == HockeyistState.SWINGING) {
                if (puckOwnerId != self.getId()) return new Result(Do.CANCEL_STRIKE, Go.go(stop(), 0));
                if (self.getSwingTicks() < 20 && safeToSwingMore()) {
                    return Result.SWING;
                } else {
                    if (enemyHasNoGoalkeeper()) {
                        Point goal = Line.of(me.angle).at(Players.opponent.getNetFront());
                        if (Players.opponent.getNetTop() + Static.PUCK_RADIUS <= goal.y &&
                            goal.y <= Players.opponent.getNetBottom() + Static.PUCK_RADIUS) {
                            return new Result(Do.STRIKE, Go.go(0, 0));
                        }
                    }
                    double angle = me.angleTo(determineGoalPoint(Players.opponent));
                    if (abs(angle) > 8 * PI / 180) {
                        return new Result(Do.CANCEL_STRIKE, Go.go(stop(), 0));
                    } else {
                        return new Result(Do.STRIKE, Go.go(stop(), angle));
                    }
                }
            }

            if (puckOwnerId != self.getId()) {
                if (isReachable(puck)) {
                    if (canShoot(current) && Evaluation.angleDifferenceToOptimal(current) < 3 * PI / 180) {
                        return new Result(Do.STRIKE, goToPuck());
                    } else {
                        return new Result(Do.TAKE_PUCK, goToPuck());
                    }
                } else {
                    // TODO: debug
                    State state = current;
                    for (int i = 0; i < 20; i++) {
                        state = state.apply(Go.go(0, 0));
                        if (i >= 10 && isReachable(state.me(), state.puck) &&
                            Evaluation.angleDifferenceToOptimal(state) < 3 * PI / 180) {
                            return Result.SWING;
                        }
                    }
                    return new Result(tryHitNearbyEnemiesOrPuck() /* TODO: ?! */, goToPuck()/* TODO: standWaitToAttack() */);
                }
            } else {
                return whatToDoWithPuck();
            }
        }
    }

    @NotNull
    private Result whatToDoWithPuck() {
        if (enemyHasNoGoalkeeper()) {
            double angle = me.angleTo(Point.of(Players.opponent.getNetFront(), Static.CENTER.y));
            double distanceToOpponentGoal = me.distance(Players.opponentGoalCenter);
            if (abs(angle) < PI / 50 && distanceToOpponentGoal > 100) {
                return new Result(distanceToOpponentGoal > 500 ? Do.SWING : Do.STRIKE, Go.go(stop(), angle));
            } else {
                return new Result(Do.NONE, Go.go(stop(), angle));
            }
        }

        if (abs(me.angleTo(Players.attack)) > 3 * PI / 4) {
            Do pass = tryPass();
            if (pass != null) return new Result(pass, Go.go(0, 0));
        }

/*
        Hockeyist attacker = findHockeyistCloserToEnemyGoal();
        if (attacker != null) {
            Do pass = tryPass(attacker);
            if (pass != null) return new Result(pass, Go.go(0, 0));
        }
*/

        Point target = determineGoalPoint(Players.opponent);
        double angle = me.angleTo(target);
        Point[] attackPoints = determineAttackPoints(current);
        double distanceToStrikingPoint = me.distance(attackPoints[0]);
        double distanceToPassingPoint = me.distance(attackPoints[1]);
        if (min(distanceToPassingPoint, distanceToStrikingPoint) < 100) {
            if (distanceToPassingPoint < 100 && abs(angle) < Const.passSector / 5 && probabilityToScore(current, 0.75) > 0.65) {
                double correctAngle = Vec.of(Point.of(puck), target).angleTo(me.direction());
                return new Result(Do.pass(1, angle), Go.go(stop(), correctAngle));
            } else if (shouldStartSwinging(current)) {
                return Result.SWING;
            }
        }
        return new Result(Do.NONE, findBestGo(new Evaluation.AttackOnEnemySide(current)));
    }

    @Nullable
    private Hockeyist findHockeyistCloserToEnemyGoal() {
        double best = abs(me.point.x - Players.opponent.getNetFront());
        Hockeyist result = null;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getPlayerId() == Players.opponent.getId() || hockeyist.getType() == HockeyistType.GOALIE) continue;
            if (hockeyist.getId() == self.getId()) continue;
            double cur = abs(hockeyist.getX() - Players.opponent.getNetFront());
            if (cur < best) {
                best = cur;
                result = hockeyist;
            }
        }
        return result;
    }

    @NotNull
    private Go findBestGo(@NotNull Evaluation evaluation) {
        double bestGoResult = Double.MIN_VALUE;
        Go bestGo = null;
        for (Go go : iteratePossibleMoves()) {
            double cur = evaluate(evaluation, go);
            if (bestGo == null || cur > bestGoResult) {
                bestGoResult = cur;
                bestGo = go;
            }
        }
        assert bestGo != null;
        return bestGo;
    }

    @NotNull
    private Go standWaitToAttack() {
        final Point target = Team.determinePointForAttacker();
        return findBestGo(new Evaluation(current) {
            @Override
            public double evaluate(@NotNull State state) {
                double penalty = 0;

                HockeyistPosition me = state.me();
                penalty -= min(0, me.velocity.projection(me.direction())) * 100;
                penalty -= pow(1 - abs(state.me().angleTo(Vec.of(state.me().point, Players.opponentGoalCenter))) / PI, 4);
                penalty += me.distance(target);
                return -penalty;
            }
        });
    }

    private boolean canShoot(@NotNull State state) {
        Point[] attackPoints = determineAttackPoints(state);
        return min(me.distance(attackPoints[0]), me.distance(attackPoints[1])) < 100;
    }

    // TODO: use
    private static boolean shouldStartSwinging(@NotNull State state) {
        for (int i = 0; i < 20; i++) {
            state = state.apply(Go.go(0, 0));
        }
        return /*probabilityToScore(state, 1) > 0.75 && */Evaluation.angleDifferenceToOptimal(state) < 3 * PI / 180;
    }

    @NotNull
    private Result losingDance() {
        Hockeyist closestEnemy = null;
        double bestDistance = Double.MAX_VALUE;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() || hockeyist.getType() == HockeyistType.GOALIE) continue;
            double cur = me.distance(hockeyist);
            if (cur < bestDistance) {
                bestDistance = cur;
                closestEnemy = hockeyist;
            }
        }
        if (closestEnemy == null) return new Result(Do.NONE, Go.go(0, Const.hockeyistTurnAngleFactor));
        return new Result(isReachable(closestEnemy) ? Do.STRIKE : Do.NONE, Go.go(1, me.angleTo(closestEnemy)));
    }

    @NotNull
    private static Result winningDance() {
        return new Result(Do.NONE, Go.go(0, Const.hockeyistTurnAngleFactor));
    }

    @Nullable
    private Do tryPass() {
        if (self.getRemainingCooldownTicks() > 0) return null;
        for (Hockeyist ally : world.getHockeyists()) {
            if (!ally.isTeammate() || ally.getType() == HockeyistType.GOALIE || ally.getId() == self.getId()) continue;
            if (abs(me.angleTo(Vec.direction(ally))) > 2 * PI / 3) {
                Do pass = tryPass(ally);
                if (pass != null) return pass;
            }
        }
        return null;
    }

    @Nullable
    private Do tryPass(@NotNull Hockeyist ally) {
        Point point = Util.puckBindingPoint(HockeyistPosition.of(ally));
        double angle = me.angleTo(point);
        if (abs(angle) < Const.passSector / 2) {
            return Do.pass(min(400.0 / me.distance(point), 1.0), angle);
        }
        return null;
    }

    private boolean safeToSwingMore() {
        if (me.velocity.length() > 4) return false;
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
    private Go goToPuck() {
        Go bestGo = null;
        double best = Double.MAX_VALUE;
        for (Go go : iteratePossibleMoves()) {
            State state = current;
            for (int i = 0; i < 30; i++) {
                state = state.apply(i < 10 ? go : Go.go(0, 0));
                double cur = Util.puckBindingPoint(state.me()).distance(state.puck.point);
                if (cur < best) {
                    best = cur;
                    bestGo = go;
                }
            }
        }
        assert bestGo != null : "Unreachable puck";
        return bestGo;
    }

    @NotNull
    public static Point[] determineAttackPoints(@NotNull State state) {
        Point me = state.me().point;

        // TODO: unhardcode
        double x1 = Static.CENTER.x + Players.attack.x * 150;
        double y1 = me.y < Static.CENTER.y ? Const.rinkTop + 50 : Const.rinkBottom - 50;

        double x2 = Static.CENTER.x + Players.attack.x * 272;
        double y2 = me.y < Static.CENTER.y
                    ? Players.opponent.getNetTop() - Const.goalNetHeight / 6
                    : Players.opponent.getNetBottom() + Const.goalNetHeight / 6;

        return new Point[]{Point.of(x1, y1), Point.of(x2, y2)};
    }

    private static double evaluate(@NotNull Evaluation evaluation, @NotNull Go go) {
        double score = 0;
        State state = evaluation.startingState.apply(go);
        score += evaluation.evaluate(state);

        for (int t = 2; t <= 10; t++) {
            state = state.apply(go);
            score += evaluation.evaluate(state) / t / 2;
        }

        return score;
    }

    public static double probabilityToScore(@NotNull State state, double strikePower) {
        Position goalie = state.enemyGoalie();
        if (goalie == null) return 1;

        HockeyistPosition position = state.me();
        Vec velocity = position.velocity;

        Point puck = state.puck.point;

        Point goalNetNearby = Players.opponentNearbyCorner(puck);
        Point goalNetDistant = Players.opponentDistantCorner(puck);
        Vec verticalMovement = Vec.of(goalNetNearby, goalNetDistant).normalize();
        Point target = goalNetDistant.shift(verticalMovement.multiply(-Static.PUCK_RADIUS));
        Vec trajectory = Vec.of(puck, target);

        if (abs(puck.x - target.x) <= 2 * Static.HOCKEYIST_RADIUS) return 0;

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
        Point goalieFinish = goalie.point.shift(verticalMovement.multiply(time * Const.goalieMaxSpeed));

        // Now we should check if distance between the following segments is >= radius(puck) + radius(goalie):
        // (goalie, goalieFinish) and (puckStart, target)
        boolean intersects = signum(Vec.of(puck, goalieNearby).crossProduct(trajectory)) !=
                             signum(Vec.of(puck, goalieFinish).crossProduct(trajectory));
        if (intersects) return 0;

        return min(1, line.project(goalieFinish).distance(goalieFinish) / (Static.HOCKEYIST_RADIUS + Static.PUCK_RADIUS));
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

    private static boolean isReachable(@NotNull HockeyistPosition from, @NotNull Position position) {
        double angle = from.angleTo(Vec.of(from, position));
        return from.distance(position) <= Const.stickLength &&
               -Const.stickSector / 2 <= angle && angle <= Const.stickSector / 2;
    }

    @NotNull
    private Point determineGoalPoint(@NotNull Player defendingPlayer) {
        double x = defendingPlayer.getNetFront();
        double y = puck.getY() < Static.CENTER.y
                   ? defendingPlayer.getNetBottom() - Static.PUCK_RADIUS
                   : defendingPlayer.getNetTop() + Static.PUCK_RADIUS;
        return Point.of(x, y);
    }

    private double stop() {
        if (me.velocity.length() < 1.0) return 0.0;
        return abs(me.angleTo(me.velocity)) > PI / 2 ? 1.0 : -1.0;
    }

    @NotNull
    private Go land(@NotNull Point target) {
        double alpha = me.angleTo(target);
        double distance = me.distance(target);
        double speed = me.velocity.length();

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
