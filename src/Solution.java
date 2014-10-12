import model.ActionType;
import model.Hockeyist;
import model.HockeyistState;
import model.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.StrictMath.*;

public class Solution {
    public static final int MAXIMUM_TICKS_TO_SWING = 35;
    public static final double ALLOWED_ANGLE_DIFFERENCE_TO_SHOOT = 3 * PI / 180;
    public static final double DISTANCE_ALLOWED_TO_COVER_BACKWARDS = 200;
    public static final double DEFAULT_PASS_POWER = 0.75;
    public static final double GOAL_POINT_SHIFT = 3; // TODO: revise
    public static final double TAKE_FREE_PUCK_MINIMUM_PROBABILITY = 0.8;
    public static final double ACCEPTABLE_PROBABILITY_TO_SCORE = 0.8;
    public static final double ALLOWED_ANGLE_FOR_GOING_WITH_FULL_SPEED = 2 * Const.hockeyistTurnAngleFactor;
    public static final double MINIMUM_ALLOWED_PASS_SAFETY = 120;
    public static final double MINIMUM_ALLOWED_WALL_PASS_SAFETY = 80;

    public static final boolean DEBUG_DO_NOTHING_UNTIL_ENEMY_MIDFIELD_MOVES = false;

    public static final boolean DEBUG_LAND_WITH_ANGLE = false;
    public static volatile Point debugTarget;
    public static volatile Point debugDirection;

    public static final boolean DEBUG_GO_TO_PUCK = false;
    public static volatile boolean goToPuck;

    private final Team team;
    private final Hockeyist self;
    private final World world;

    private final State current;
    private final HockeyistPosition me;
    private final PuckPosition puck;
    private final HockeyistPosition puckOwner;

    private final Decision decision;
    public static final int TICKS_TO_CONSIDER_PASS_AGAINST_THE_WALL = 15;
    public static final double PASS_AGAINST_THE_WALL_ACCEPTABLE_DISTANCE = 80;

    public Solution(@NotNull Team team, @NotNull Hockeyist self, @NotNull World world) {
        this.team = team;
        this.self = self;
        this.world = world;

        this.current = State.of(self, world);
        this.me = current.me();
        this.puck = current.puck;
        this.puckOwner = current.puckOwner();

        this.decision = team.getDecision(me.id());
    }

    @NotNull
    public Result solve() {
        if (DEBUG_LAND_WITH_ANGLE) {
            if (self.getOriginalPositionIndex() != 0) return Result.NOTHING;
            if (debugTarget != null) {
                return landWithAngle(debugTarget, Vec.of(debugTarget, debugDirection).angle());
            }
            if (DEBUG_LAND_WITH_ANGLE) return Result.NOTHING;
        }

        if (DEBUG_GO_TO_PUCK) {
            if (self.getOriginalPositionIndex() != 0 || !goToPuck) return Result.NOTHING;
            return new Result(Do.NOTHING, goToPuck());
        }

        if (world.getMyPlayer().isJustScoredGoal() || world.getMyPlayer().isJustMissedGoal()) {
            Result substitute = substitute();
            return substitute != null ? substitute : new Result(hitEnemyIfReachable(), goTo(Static.CENTER));
        }

        // If we are swinging, strike or cancel strike or continue swinging
        if (self.getState() == HockeyistState.SWINGING) {
            return new Result(strikeOrCancelOrContinueSwinging(), Go.NOWHERE);
        }

        // If not swinging, maybe we have the puck and are ready to shoot or the puck is just flying by at the moment
        Do shoot = maybeShootOrStartSwinging();
        if (shoot != null) return new Result(shoot, Go.NOWHERE);

        // If we have the puck, swing/shoot/pass or just go to the attack point
        if (puckOwner != null && puckOwner.id() == me.id()) {
            return withPuck();
        }

        // Else if the puck is free or owned by an enemy, try to obtain/volley it
        if (puckOwner == null || !puckOwner.teammate()) {
            // If we can score in several turns, prepare to volley
            Result prepare = prepareForVolley(current);
            if (prepare != null) return prepare;

            // Else if the puck is close, try to obtain it, i.e. either wait for it to come or go and try to take/strike it
            Result obtain = obtainPuck();
            if (obtain != null) return obtain;
        }

        // Else stay where we're supposed to stay
        return obey();
    }

    @NotNull
    private Result withPuck() {
        switch (decision.role) {
            case MIDFIELD:
                Result passMidfieldToAttacker = maybePassToAttacker();
                if (passMidfieldToAttacker != null) return passMidfieldToAttacker;
                return goForwardToShootingPosition();
            case ATTACK:
                // TODO
                HockeyistPosition defender = findAlly(Decision.Role.DEFENSE);
                if (defender != null) {
                    if (me.direction().innerProduct(Players.defense) > 0 && abs(me.angleTo(defender.point)) < PI / 4) {
                        Result passAttackerToDefender = makePassMaybeTurnBefore(Util.puckBindingPoint(defender));
                        if (passAttackerToDefender != null) return passAttackerToDefender;
                    }
                }
                return goForwardToShootingPosition();
            case DEFENSE:
                // TODO
                Result passDefenderToAttacker = maybePassToAttacker();
                if (passDefenderToAttacker != null && Players.teamSize != 2 /* TODO */) return passDefenderToAttacker;
                HockeyistPosition midfield = findAlly(Decision.Role.MIDFIELD);
                if (midfield != null) {
                    Result passDefenderToMidfield = makePassMaybeTurnBefore(Util.puckBindingPoint(midfield));
                    if (passDefenderToMidfield != null) return passDefenderToMidfield;
                }
                return goForwardToShootingPosition();
        }
        throw new AssertionError(decision.role);
    }

    @NotNull
    private Result goForwardToShootingPosition() {
        // TODO: (!) improve this heuristic
        for (Go go : current.iteratePossibleMoves(8)) {
            State state = current;
            for (int i = 0; i < 35; i++) {
                state = state.apply(i < 10 ? go : Go.NOWHERE);
                if (permissionToShoot(i - 10 >= 10 ? i - 10 : 0, state) || canScoreWithPass(state)) {
                    return new Result(Do.NOTHING, go);
                }
            }
        }

        Go best = null;
        int bestTick = Integer.MAX_VALUE;
        firstMove: for (Go firstMove : current.iteratePossibleMoves(8)) {
            State state = current;
            for (int i = 0; i < 20 && i < bestTick - 3; i++) {
                state = state.apply(firstMove);
                if (permissionToShoot(0, state) || canScoreWithPass(state)) {
                    best = firstMove;
                    bestTick = i;
                    continue firstMove;
                }
            }
            if (abs(firstMove.speedup) < 1e-9) continue;
            secondMove: for (Go secondMove : state.iteratePossibleMoves(2)) {
                if (firstMove.speedup > secondMove.speedup || (firstMove.speedup == secondMove.speedup && firstMove.turn > secondMove.turn)) break;
                if (firstMove.speedup * secondMove.speedup < 0) break;
                State next = state;
                for (int i = 20; i < 40 && i < bestTick - 3; i++) {
                    next = next.apply(secondMove);
                    if (permissionToShoot(0, next) || canScoreWithPass(next)) {
                        best = firstMove;
                        bestTick = i;
                        continue secondMove;
                    }
                }
                for (int i = 40; i < 55 && i < bestTick - 3; i++) {
                    next = next.apply(Go.NOWHERE);
                    if (permissionToShoot(i - 40 >= 10 ? i - 40 : 0, next) || canScoreWithPass(next)) {
                        best = firstMove;
                        bestTick = i;
                        continue secondMove;
                    }
                }
            }
        }

        if (best != null) {
            return new Result(Do.NOTHING, best);
        }

        if (abs(me.point.x - Players.opponentGoalCenter.x) - abs(me.point.x - Players.myGoalCenter.x) > 200) {
            Point target1 = Point.of(Static.CENTER.x, Const.rinkTop + 50);
            Point target2 = Point.of(Static.CENTER.x, Const.rinkBottom - 50);
            double value1 = 0;
            double value2 = 0;
            for (HockeyistPosition enemy : current.enemies()) {
                value1 += enemy.distance(target1);
                value2 += enemy.distance(target2);
            }
            return new Result(Do.NOTHING, naiveGoTo(value1 > value2 ? target1 : target2));
        }

        // TODO: (!) improve
        Point target = Point.of(
                Players.opponent.getNetFront() - Players.attack.x * 300,
                me.point.y > Static.CENTER.y ? Static.CENTER.y + 200 : Static.CENTER.y - 200
        );
        return new Result(Do.NOTHING, naiveGoTo(target));
    }

    @Nullable
    private static Result prepareForVolley(@NotNull State current) {
        for (int ticks = 5; ticks <= 15; ticks += 5) {
            for (Go go : current.iteratePossibleMoves(4)) {
                State state = current;
                for (int i = 0; i < 60; i++) {
                    state = state.apply(i < ticks ? go : Go.NOWHERE);
                    int swingTicks = i - ticks >= 10 ? i - ticks : 0;
                    if (isReachable(state.me(), state.puck) && permissionToShoot(swingTicks, state)) {
                        return new Result(Do.NOTHING, go);
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private Do maybeShootOrStartSwinging() {
        if (me.cooldown > 0) return null;
        if (puckOwner != null && puckOwner.id() == me.id() && canScoreWithPass(current)) {
            Point target = current.overtimeNoGoalies() ? Players.opponentGoalCenter : Players.opponentDistantGoalPoint(puck.point);
            double correctAngle = Vec.of(puck.point, target).angleTo(me.direction());
            return Do.pass(1, correctAngle);
        }
        if (shouldStartSwinging(current)) return Do.SWING;
        if (isReachable(me, puck) && permissionToShoot(0, current)) return Do.STRIKE;
        return null;
    }

    @NotNull
    private Result obey() {
        if (decision.role != Decision.Role.DEFENSE) {
            Result substitute = substitute();
            if (substitute != null) return substitute;
        }

        Point dislocation = decision.dislocation;
        switch (decision.role) {
            case MIDFIELD:
                return landWithAngle(dislocation, Vec.of(dislocation, puck.point).angle());
            case ATTACK:
                return landWithAngle(dislocation, Vec.of(dislocation, Players.opponentDistantGoalPoint(me.point)).angle());
            case DEFENSE:
                return landWithAngle(dislocation, Vec.of(dislocation, puck.point).angle());
        }
        throw new AssertionError(decision.role);
    }

    @Nullable
    private Result maybePassToAttacker() {
        HockeyistPosition attacker = findAlly(Decision.Role.ATTACK);
        if (attacker == null) return null;
        Vec desirableDirection = Vec.of(attacker.point, Players.opponentDistantGoalPoint(attacker.point)).normalize();
        Point location = attacker.point.shift(desirableDirection.multiply(Const.puckBindingRange));
        if (feasibleLocationToShoot(1, attacker.direction(), null, Util.puckBindingPoint(attacker), attacker, current.overtimeNoGoalies())) {
            Result pass = makePassMaybeTurnBefore(location);
            if (pass != null) return pass;
        }
        return null;
    }

    @Nullable
    private Result maybePassAgainstTheWall(@NotNull Point location) {
        if (me.cooldown >= TICKS_TO_CONSIDER_PASS_AGAINST_THE_WALL) return null;

        if (me.cooldown == 0 && shootAgainstTheWallBestDistance(current, location) < PASS_AGAINST_THE_WALL_ACCEPTABLE_DISTANCE) {
            return new Result(Do.STRIKE, Go.NOWHERE);
        }

        double bestDistance = Double.MAX_VALUE;
        Go best = null;
        for (Go go : current.iteratePossibleMoves(8)) {
            State state = current;
            for (int i = 0; i < TICKS_TO_CONSIDER_PASS_AGAINST_THE_WALL; i++) {
                state = state.apply(go);
                if (state.me().cooldown > 0) continue;
                double cur = shootAgainstTheWallBestDistance(state, location);
                if (cur < bestDistance) {
                    bestDistance = cur;
                    best = go;
                }
            }
        }

        return bestDistance < PASS_AGAINST_THE_WALL_ACCEPTABLE_DISTANCE &&
               passSafety(Vec.of(current.puck.point, location).angleTo(me.direction())) > MINIMUM_ALLOWED_WALL_PASS_SAFETY ?
               new Result(Do.NOTHING, best) : null;
    }

    private static double shootAgainstTheWallBestDistance(@NotNull State state, @NotNull Point location) {
        PuckPosition puck = state.puck.strike(state.me(), 0);
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < 70; i++) {
            puck = puck.move();
            Point point = puck.point;
            if (Const.goalNetTop - 100 <= point.y && point.y <= Const.goalNetTop + Const.goalNetHeight + 100 &&
                min(abs(point.x - Players.opponent.getNetFront()), abs(point.x - Players.me.getNetFront())) < 80) {
                break;
            }
            double cur = point.distance(location);
            if (cur < bestDistance) {
                bestDistance = cur;
            }
        }
        return bestDistance;
    }

    private static final double[] BUF = new double[3];

    private double passSafety(double angle) {
        PuckPosition puck = this.puck.strike(me, angle);
        List<HockeyistPosition> enemies = new ArrayList<>(3);
        for (HockeyistPosition enemy : current.enemies()) enemies.add(enemy);
        int n = enemies.size();
        double[] minDist = BUF;
        Arrays.fill(minDist, 100000);
        for (int i = 0; i < 40; i++) {
            puck = puck.move();
            if ((i & 3) == 3) {
                for (int j = 0; j < n; j++) {
                    HockeyistPosition enemy = enemies.get(j);
                    if (enemy.cooldown - i <= 0) {
                        minDist[j] = min(minDist[j], puck.distance(Util.puckBindingPoint(enemy)));
                    }
                }
            }
        }
        double average = 0;
        for (double dist : minDist) {
            average += 1 / dist;
        }
        return n / average;
    }

    @Nullable
    private Result makePassMaybeTurnBefore(@NotNull Point location) {
        Result againstTheWall = maybePassAgainstTheWall(location);
        if (againstTheWall != null) return againstTheWall;

        State state = current;
        for (int i = 0; i < 40; i++) {
            Result move = makePassTo(state.me(), location);
            if (move.action.type == ActionType.PASS) {
                if (passSafety(move.action.passAngle) > MINIMUM_ALLOWED_PASS_SAFETY) return makePassTo(me, location);
            }
            state = state.apply(move.direction);
        }
        return null;
    }

    @NotNull
    private static Result makePassTo(@NotNull HockeyistPosition me, @NotNull Point location) {
        double angle = me.angleTo(location);
        if (me.cooldown > 0 || abs(angle) >= Const.passSector / 2) {
            return new Result(Do.NOTHING, Go.go(0, angle));
        } else {
            return new Result(Do.pass(min(1, DEFAULT_PASS_POWER / me.strength()), angle), Go.NOWHERE);
        }
    }

    @Nullable
    private Result substitute() {
        if (!team.timeToRest.contains(me.id())) return null;

        if (me.point.y < Const.rinkTop + Const.substitutionAreaHeight &&
            me.distance(Players.myGoalCenter) < me.distance(Players.opponentGoalCenter)) {
            return new Result(Do.substitute(Util.findRestingAllyWithMaxStamina(world).getTeammateIndex()), Go.NOWHERE);
        }

        return new Result(hitEnemyIfReachable(), goTo(Point.of((Static.CENTER.x + Players.me.getNetFront()) / 2, 0)));
    }

    @Nullable
    private Result obtainPuck() {
        double distance = me.distance(puck);
        boolean close = distance < 300 || (distance < 400 && puck.velocity.projection(Vec.of(puck, me)) > 2);
        if (!close) return null;

        if (puckOwner == null) {
            Result wait;
            // TODO: not puck binding point, but intersection of puck trajectory and our direction
            if (decision.role == Decision.Role.ATTACK &&
                feasibleLocationToShoot(me.strength(), me.direction(), null, Util.puckBindingPoint(me), me, current.overtimeNoGoalies())) {
                wait = waitForPuckToCome(Vec.of(me.point, Players.opponentDistantGoalPoint(me.point)).angle(), true);
            } else {
                wait = waitForPuckToCome(me.angle, false);
            }
            if (wait != null) return wait;

            return new Result(takeOrStrikePuckIfReachable(), goToPuck());
        }

        // Defender should not go out very far away from his point because the enemy can dribble him
        if (decision.role == Decision.Role.DEFENSE) {
            if (puck.distance(decision.dislocation) > 200) return null;
        }

        if (isReachable(me, puck) || isReachable(me, puckOwner)) {
            return new Result(Do.STRIKE, goToPuck());
        }

        return new Result(hitEnemyIfReachable(), goToPuck());
    }

    @Nullable
    private Result waitForPuckToCome(double desiredAngle, boolean finalAngleMatters) {
        State state = current;
        double best = Double.MAX_VALUE;
        // If I won't be going anywhere but rather will turn to the desired angle, will any enemy obtain the puck before me?
        for (int i = 0; i < 60; i++) {
            Go go = finalAngleMatters ? Go.go(0, Util.normalize(desiredAngle - state.me().angle)) : Go.NOWHERE;
            state = state.apply(go, Go.go(1, 0));
            for (HockeyistPosition enemy : state.enemies()) {
                if (isReachable(enemy, state.puck)) return null;
            }
            if (state.puck.distance(state.me()) >= Const.stickLength) continue;
            if (finalAngleMatters) {
                if (abs(Util.normalize(state.me().angle - desiredAngle)) < 0.01 && abs(state.me().angleTo(state.puck)) < Const.stickSector / 2) {
                    return new Result(takeOrStrikePuckIfReachable(), Go.go(0, Util.normalize(desiredAngle - me.angle)));
                }
            } else {
                double cur = Util.normalize(Vec.of(state.me(), state.puck).angle() - me.angle);
                if (abs(cur) < abs(best)) {
                    best = cur;
                }
            }
        }
        if (best == Double.MAX_VALUE) return null;
        return new Result(takeOrStrikePuckIfReachable(), Go.go(0, best));
    }

    @NotNull
    private Do takeOrStrikePuckIfReachable() {
        if (me.cooldown > 0) return Do.NOTHING;

        if (!isReachable(me, puck)) return hitEnemyIfReachable();

        if (Util.takeFreePuckProbability(me, puck) > TAKE_FREE_PUCK_MINIMUM_PROBABILITY) return Do.TAKE_PUCK;

        // Don't score own goals
        Vec direction = me.direction();
        if (direction.innerProduct(Players.defense) > 0) {
            if (signum(direction.crossProduct(Vec.of(me.point, Players.myBottomCorner))) !=
                signum(direction.crossProduct(Vec.of(me.point, Players.myTopCorner)))) {
                return Do.TAKE_PUCK;
            }
        }

        return Do.STRIKE;
    }

    @NotNull
    private Do hitEnemyIfReachable() {
        if (puckOwner != null && puckOwner.id() == me.id()) return Do.NOTHING;
        for (HockeyistPosition ally : current.allies()) {
            if (isReachable(me, ally)) return Do.NOTHING;
        }
        for (HockeyistPosition enemy : current.enemies()) {
            if (isReachable(me, enemy)) return Do.STRIKE;
        }
        return Do.NOTHING;
    }

    @NotNull
    private Do strikeOrCancelOrContinueSwinging() {
        int swingTicks = self.getSwingTicks();
        if (swingTicks < Const.swingActionCooldownTicks || me.cooldown > 0) return Do.SWING;

        if (isReachable(me, puck)) {
            State nextTurn = current.applyWithCollisions(Go.NOWHERE);
            for (HockeyistPosition enemy : nextTurn.enemies()) {
                if (isReachable(enemy, nextTurn.puck) || isReachable(enemy, nextTurn.me())) return Do.STRIKE;
            }
        }

        if (continueSwinging(current, swingTicks)) return Do.SWING;

        if (!isReachable(me, puck)) return Do.CANCEL_STRIKE;

        if (current.overtimeNoGoalies()) return Do.STRIKE;

        boolean puckIsFreeOrOwnedByEnemy = puckOwner == null || (puckOwner.id() != me.id() && !puckOwner.teammate());
        return puckIsFreeOrOwnedByEnemy || permissionToShoot(swingTicks, current) ? Do.STRIKE : Do.CANCEL_STRIKE;
    }

    private static boolean shouldStartSwinging(@NotNull State current) {
        State state = current;
        for (int i = 0; i < Const.swingActionCooldownTicks; i++) {
            state = state.apply(Go.NOWHERE);
        }
        return (isReachable(state.me(), state.puck) && permissionToShoot(Const.swingActionCooldownTicks, state)) ||
               continueSwinging(state, Const.swingActionCooldownTicks);
    }

    private static boolean continueSwinging(@NotNull State state, int swingTicks) {
        for (int i = swingTicks; i < MAXIMUM_TICKS_TO_SWING; i++) {
            state = state.apply(Go.NOWHERE);
            if (isReachable(state.me(), state.puck) && permissionToShoot(i, state)) return true;
        }
        return false;
    }

    private static boolean permissionToShoot(int swingTicks, @NotNull State state) {
        return feasibleLocationToShoot(effectiveShotPower(swingTicks), state.me().direction(), state.enemyGoalie(),
                                       state.puck.point, state.me(), state.overtimeNoGoalies()) &&
               angleDifferenceToOptimal(state) <= ALLOWED_ANGLE_DIFFERENCE_TO_SHOOT * (state.overtimeNoGoalies() ? 3 : 1);
    }

    private static double angleDifferenceToOptimal(@NotNull State state) {
        Point puck = state.puck.point;
        Point target = state.overtimeNoGoalies() ? Players.opponentGoalCenter : Players.opponentDistantGoalPoint(puck);
        Vec trajectory = Vec.of(puck, target);
        return abs(state.me().angleTo(trajectory));
    }

    private static boolean canScoreWithPass(@NotNull State state) {
        HockeyistPosition me = state.me();
        Point target = state.overtimeNoGoalies() ? Players.opponentGoalCenter : Players.opponentDistantGoalPoint(me.point);
        double passAngle = Vec.of(state.puck.point, target).angleTo(me.direction());
        if (abs(passAngle) >= Const.passSector / 2) return false;
        Vec strikeDirection = Vec.of(Util.normalize(me.angle + passAngle));
        return feasibleLocationToShoot(Const.passPowerFactor, strikeDirection, state.enemyGoalie(), state.puck.point,
                                       me, state.overtimeNoGoalies());
    }

    private static double effectiveShotPower(int swingTicks) {
        return (Const.strikePowerBaseFactor + Const.strikePowerGrowthFactor * min(swingTicks, Const.maxEffectiveSwingTicks));
    }

    public static boolean feasibleLocationToShoot(
            double strikePower,
            @NotNull Vec strikeDirection,
            @Nullable Point defendingGoalie,
            @NotNull Point puck,
            @NotNull HockeyistPosition attacker,
            boolean overtimeNoGoalies
    ) {
        if (overtimeNoGoalies) {
            // TODO: maybe something more clever?
            return strikePower * attacker.strength() > 0.75 ||
                   abs(puck.x - Players.opponent.getNetFront()) <= abs(Static.CENTER.x - Players.opponent.getNetFront());
        }
        return probabilityToScore(strikePower, strikeDirection, defendingGoalie, puck, attacker) > ACCEPTABLE_PROBABILITY_TO_SCORE;
    }

    @NotNull
    private Go goToPuck() {
        // TODO: (!) improve this heuristic

        // If the puck is owned by someone, simulate the world as if the owner is bound to the puck which moves freely
        // (v + |v|*x) * 0.98 = v
        // 0.98v + 0.98|v|x = v
        // x = 0.02/0.98
        Go puckOwnerDirection = puckOwner != null ? Go.go(0.02 / 0.98, 0) : Go.NOWHERE;

        Go bestGo = null;
        int bestFirstTickToReach = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (int ticks = 10; ticks <= 60; ticks += 10) {
            for (Go go : current.iteratePossibleMoves(4)) {
                State state = current;
                for (int i = 0; i < 60 && i < bestFirstTickToReach; i++) {
                    state = state.apply(i < ticks ? go : Go.NOWHERE, puckOwnerDirection);
                    if (isReachable(state.me(), state.puck)) {
                        bestFirstTickToReach = i;
                        bestGo = go;
                        break;
                    } else if (bestFirstTickToReach == Integer.MAX_VALUE) {
                        double cur = Util.puckBindingPoint(state.me()).distance(state.puck.point);
                        if (cur < bestDistance) {
                            bestDistance = cur;
                            bestGo = go;
                        }
                    }
                }
            }
        }
        assert bestGo != null;
        return bestGo;
    }

    private static boolean isReachable(@NotNull HockeyistPosition hockeyist, @NotNull Position victim) {
        return hockeyist.cooldown == 0 &&
               hockeyist.distance(victim) < Const.stickLength &&
               abs(hockeyist.angleTo(victim.point)) < Const.stickSector / 2;
    }

    @Nullable
    private HockeyistPosition findAlly(@NotNull Decision.Role role) {
        for (HockeyistPosition ally : current.allies()) {
            if (team.getDecision(ally.id()).role == role) return ally;
        }
        return null;
    }

    // TODO: optimize
    public static double probabilityToScore(
            double strikePower,
            @NotNull Vec strikeDirection, // can be different from attacker.direction() only in case of a pass
            @Nullable Point defendingGoalie, // null means just take the nearby corner position
            @NotNull Point puck,
            @NotNull HockeyistPosition attacker
    ) {
        Vec verticalMovement = puck.y > Static.CENTER.y ? Vec.UP : Vec.DOWN;
        Point target = Players.opponentDistantGoalPoint(puck);

        if (abs(puck.x - target.x) <= 2 * Static.HOCKEYIST_RADIUS) return 0;

        Vec goalieHorizontalShift = Players.attack.multiply(-Static.HOCKEYIST_RADIUS);
        Point goalieNearby = Players.opponentNearbyCorner(puck).shift(verticalMovement.multiply(Static.HOCKEYIST_RADIUS)).shift(goalieHorizontalShift);
        if (defendingGoalie == null) defendingGoalie = goalieNearby;
        Point goalieDistant = Players.opponentDistantCorner(puck).shift(verticalMovement.multiply(-Static.HOCKEYIST_RADIUS)).shift(goalieHorizontalShift);

        double puckSpeed = Const.struckPuckInitialSpeedFactor * strikePower * attacker.strength() + attacker.velocity.projection(strikeDirection);

        Line line = Line.between(puck, target);
        boolean withinGoalieReach = min(goalieNearby.y, goalieDistant.y) <= puck.y && puck.y <= max(goalieNearby.y, goalieDistant.y);
        Point puckStart = line.when(withinGoalieReach ? puck.y : goalieNearby.y);

        // Ignore friction since no rebounds are expected and the distance is very small
        // -1 because goalie falls behind on 1 tick
        double time = puckStart.distance(target) / puckSpeed - 1;
        Point goalieFinish = defendingGoalie.shift(verticalMovement.multiply(time * Const.goalieMaxSpeed));

        // Now we should check if distance between the following segments is >= radius(puck) + radius(goalie):
        // (goalie, goalieFinish) and (puckStart, target)
        Vec trajectory = Vec.of(puck, target);
        boolean intersects = signum(Vec.of(puck, goalieNearby).crossProduct(trajectory)) !=
                             signum(Vec.of(puck, goalieFinish).crossProduct(trajectory));
        if (intersects) return 0;

        return min(1, line.project(goalieFinish).distance(goalieFinish) / (Static.HOCKEYIST_RADIUS + Static.PUCK_RADIUS));
    }

    @NotNull
    private Go naiveGoTo(@NotNull Point target) {
        return Go.go(1, me.angleTo(target));
    }

    @NotNull
    private Go goTo(@NotNull Point target) {
        double alpha = me.angleTo(target);
        double distance = me.distance(target);
        double speed = me.velocity.length();

        boolean closeBy = distance < speed * speed / 2 / Const.hockeyistSpeedUpFactor / me.agility();

        double speedTowards = me.velocity.projection(Vec.of(me.point, target));
        if (abs(alpha) < PI / 2) {
            // The target is ahead, moving forward
            if (abs(alpha) < ALLOWED_ANGLE_FOR_GOING_WITH_FULL_SPEED) {
                // Keep moving forward, accelerate or slow down depending on the distance
                return Go.go(closeBy ? -1 : 1, alpha);
            }
            // Else lower our speed if needed and turn to the target
            return Go.go(speedTowards > 1 || closeBy ? -1 : 0, alpha);
        } else if (distance > DISTANCE_ALLOWED_TO_COVER_BACKWARDS) {
            // The target is behind, but we need to turn around and go forward
            return Go.go(0, alpha);
        } else {
            double turn = alpha > 0 ? alpha - PI : alpha + PI;
            if (PI - abs(alpha) < ALLOWED_ANGLE_FOR_GOING_WITH_FULL_SPEED) {
                return Go.go(closeBy ? 1 : -1, turn);
            }
            return Go.go(speedTowards > 1 || closeBy ? 1 : 0, turn);
        }
    }

    @NotNull
    private Result landWithAngle(@NotNull Point target, double angle) {
        for (int fullBack = 0; fullBack <= 50; fullBack += 10) {
            State state = current;
            for (int i = 0; i < fullBack; i++) {
                state = state.apply(Go.go(-1, 0));
            }
            while (state.me().velocity.length() > 0.1) {
                state = state.apply(Go.go(0, Util.normalize(angle - state.me().angle)));
            }
            if (state.me().distance(target) < 20) {
                return new Result(hitEnemyIfReachable(), fullBack > 0 ? Go.go(-1, 0) : Go.go(0, Util.normalize(angle - me.angle)));
            }
        }

        return new Result(hitEnemyIfReachable(), goTo(target));
    }

    @Override
    public String toString() {
        return String.format("tick %d puck %s %s", world.getTick(), puck, current);
    }
}
