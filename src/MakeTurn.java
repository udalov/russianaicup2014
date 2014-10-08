import model.ActionType;
import model.Hockeyist;
import model.HockeyistState;
import model.World;

import java.util.ArrayList;
import java.util.Collection;

import static java.lang.StrictMath.*;

public class MakeTurn {
    public static final int MAXIMUM_TICKS_TO_SWING = 25;
    public static final double ALLOWED_ANGLE_DIFFERENCE_TO_SHOOT = 3 * PI / 180;
    public static final double DISTANCE_ALLOWED_TO_COVER_BACKWARDS = 200;
    public static final double DEFAULT_PASS_POWER = 0.75;
    public static final double GOAL_POINT_SHIFT = 3; // TODO: revise
    public static final double TAKE_FREE_PUCK_MINIMUM_PROBABILITY = 0.8;
    public static final double ACCEPTABLE_PROBABILITY_TO_SCORE = 0.8;
    public static final double ALLOWED_ANGLE_FOR_GOING_WITH_FULL_SPEED = 2 * Const.hockeyistTurnAngleFactor;

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

    public MakeTurn(@NotNull Team team, @NotNull Hockeyist self, @NotNull World world) {
        this.team = team;
        this.self = self;
        this.world = world;

        this.current = State.of(self, world);
        this.me = current.me();
        this.puck = current.puck;
    }

    @NotNull
    public Result makeTurn() {
        if (DEBUG_LAND_WITH_ANGLE) {
            if (self.getOriginalPositionIndex() != 0) return Result.NOTHING;
            if (debugTarget != null) {
                return landWithAngle(debugTarget, Vec.of(debugTarget, debugDirection).angle());
            }
            if (DEBUG_LAND_WITH_ANGLE) return Result.NOTHING;
        }

        if (DEBUG_GO_TO_PUCK) {
            if (self.getOriginalPositionIndex() != 0 || !goToPuck) return Result.NOTHING;
            return new Result(Do.NONE, goToPuck());
        }

        if (world.getMyPlayer().isJustScoredGoal() || world.getMyPlayer().isJustMissedGoal()) {
            // TODO: maybe substitute
            return Result.NOTHING;
        }

        HockeyistPosition puckOwner = current.puckOwner();
        Decision decision = team.getDecision(me.id());

        // TODO: entirely different logic should be present for overtime with no goalies

        // If we are swinging, strike or cancel strike or continue swinging
        if (self.getState() == HockeyistState.SWINGING) {
            return new Result(strikeOrCancelOrContinueSwinging(), Go.NOWHERE);
        }

        // If we have the puck, swing/shoot/pass or just go to the attack point
        if (puckOwner != null && puckOwner.id() == me.id()) {
            return withPuck(decision.role);
        }

        // Else if the puck is free or owned by an enemy, try to obtain/volley it
        if ((puckOwner == null || puckOwner.hockeyist.getPlayerId() != Players.me.getId())) {
            // If we can score in 10-20 turns, start swinging to volley
            // TODO: (!) also try to turn a little before receiving the puck for the volley
            if (shouldStartSwinging()) {
                return Result.SWING;
            }
            // Else if the puck is close, try to obtain it, i.e. either wait for it to come or go and try to take/strike it
            Result obtain = obtainPuck(decision.role);
            if (obtain != null) return obtain;
        }

        // Else stay where we're supposed to stay
        return obey(decision);
    }

    @NotNull
    private Result withPuck(@NotNull Decision.Role role) {
        switch (role) {
            case MIDFIELD:
                Result passMidfieldToAttacker = maybePassToAttacker();
                if (passMidfieldToAttacker != null) return passMidfieldToAttacker;
                return goForwardMaybeShoot();
            case ATTACK:
                // TODO
                HockeyistPosition defender = findAlly(Decision.Role.DEFENSE);
                if (defender != null) {
                    if (abs(me.angleTo(defender.point)) < PI / 4) {
                        Result passAttackerToDefender = makePassMaybeTurnBefore(Util.puckBindingPoint(defender));
                        if (passAttackerToDefender != null) return passAttackerToDefender;
                    }
                }
                return goForwardMaybeShoot();
            case DEFENSE:
                // TODO
                Result passDefenderToAttacker = maybePassToAttacker();
                if (passDefenderToAttacker != null) return passDefenderToAttacker;
                HockeyistPosition midfield = findAlly(Decision.Role.MIDFIELD);
                if (midfield != null) {
                    Result passDefenderToMidfield = makePassMaybeTurnBefore(Util.puckBindingPoint(midfield));
                    if (passDefenderToMidfield != null) return passDefenderToMidfield;
                }
                return goForwardMaybeShoot();
        }
        throw new AssertionError(role);
    }

    @NotNull
    private Result goForwardMaybeShoot() {
        Do shoot = maybeShoot();
        if (shoot != null) return new Result(shoot, Go.NOWHERE);

        Go best = null;
        int bestTick = Integer.MAX_VALUE;
        for (int ticks = 10; ticks <= 40; ticks += 10) {
            for (Go go : iteratePossibleMoves(4)) {
                State state = current;
                for (int i = 0; i < 60 && i < bestTick; i++) {
                    state = state.moveAllNoCollisions(i < ticks ? go : Go.NOWHERE, Go.NOWHERE);
                    int couldBeSwinging = i - ticks >= 10 ? i - ticks : 0;
                    if (permissionToShoot(couldBeSwinging, state) || canScoreWithPass(state)) {
                        best = go;
                        bestTick = i;
                        break;
                    }
                }
            }
        }
        if (best != null) {
            return new Result(Do.NONE, best);
        }

/*
        double bestProbability = 0;
        for (int ticks = 10; ticks <= 40; ticks += 10) {
            for (Go go : iteratePossibleMoves(4)) {
                State state = current;
                for (int i = 0; i < 60; i++) {
                    state = state.moveAllNoCollisions(i < ticks ? go : Go.NOWHERE);
                    double strikePower = effectiveShotPower(0, state.me());
                    double cur = probabilityToScore(strikePower, state.enemyGoalie(), state.puck.point, state.me());
                    if (cur > bestProbability) {
                        bestProbability = cur;
                        best = go;
                        break;
                    }
                }
            }
        }
        if (best != null) return new Result(Do.NONE, best);

        // At this point, no matter where we go, there's 0 probability to score in a few ticks
*/

        // TODO: (!) improve
        // TODO: (!) check which trajectory is safest
        Point target = Point.of(
                Players.opponent.getNetFront() - Players.attack.x * 300,
                me.point.y > Static.CENTER.y ? Static.CENTER.y + 200 : Static.CENTER.y - 200
        );
        return new Result(Do.NONE, goTo(target));
    }

    @Nullable
    private Do maybeShoot() {
        if (canScoreWithPass(current)) {
            Point target = Players.opponentDistantGoalPoint(puck.point);
            double correctAngle = Vec.of(puck.point, target).angleTo(me.direction());
            return Do.pass(1, correctAngle);
        }
        if (shouldStartSwinging()) return Do.SWING;
        if (permissionToShoot(self.getSwingTicks(), current)) return Do.STRIKE;
        return null;
    }

    @NotNull
    private Result obey(@NotNull Decision decision) {
        // TODO: maybe substitute
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
        if (feasibleLocationToShoot(1, null, Util.puckBindingPoint(attacker), attacker)) {
            Result pass = makePassMaybeTurnBefore(location);
            if (pass != null) return pass;
        }
        return null;
    }

    @Nullable
    private Result makePassMaybeTurnBefore(@NotNull Point location) {
        State state = current;
        for (int i = 0; i < 40; i++) {
            Result move = makePassTo(state.me(), location);
            if (move.action.type == ActionType.PASS) {
                // TODO: check if it's safe
                return makePassTo(me, location);
            }
            state = state.moveAllNoCollisions(move.direction, Go.NOWHERE);
        }
        return null;
    }

    @NotNull
    private static Result makePassTo(@NotNull HockeyistPosition me, @NotNull Point location) {
        // TODO: (!) also strike against the wall
        double angle = me.angleTo(location);
        if (me.cooldown > 0 || abs(angle) >= Const.passSector / 2) {
            return new Result(Do.NONE, Go.go(0, angle));
        } else {
            return new Result(Do.pass(DEFAULT_PASS_POWER, angle), Go.NOWHERE);
        }
    }

    @Nullable
    private Result obtainPuck(@NotNull Decision.Role role) {
        double distance = me.distance(puck);
        boolean close = distance < 200 || (distance < 400 && puck.velocity.projection(Vec.of(puck, me)) > 2);
        if (!close) return null;

        HockeyistPosition puckOwner = current.puckOwner();
        if (puckOwner == null) {
            Result wait;
            // TODO: not puck binding point, but intersection of puck trajectory and our direction
            if (role == Decision.Role.ATTACK && feasibleLocationToShoot(me.strength(), null, Util.puckBindingPoint(me), me)) {
                wait = waitForPuckToCome(Vec.of(me.point, Players.opponentDistantGoalPoint(me.point)).angle(), true);
            } else {
                wait = waitForPuckToCome(me.angle, false);
            }
            if (wait != null) return wait;

            return new Result(takeOrStrikePuckIfReachable(), goToPuck());
        }

        // TODO: defender should not come out very early because the enemy can dribble him

        if (isReachable(me, current.puck) || isReachable(me, puckOwner)) {
            return new Result(Do.STRIKE, goToPuck());
        }

        return new Result(Do.NONE, goToPuck());
    }

    @Nullable
    private Result waitForPuckToCome(double desiredAngle, boolean finalAngleMatters) {
        State state = current;
        double best = Double.MAX_VALUE;
        // If I won't be going anywhere but rather will turn to the desired angle, will any enemy obtain the puck before me?
        for (int i = 0; i < 60; i++) {
            Go go = finalAngleMatters ? Go.go(0, Util.normalize(desiredAngle - state.me().angle)) : Go.NOWHERE;
            state = state.moveAllNoCollisions(go, Go.go(1, 0));
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
        if (!isReachable(me, puck)) return Do.NONE;
        if (Util.takeFreePuckProbability(me, puck) > TAKE_FREE_PUCK_MINIMUM_PROBABILITY) return Do.TAKE_PUCK;
        return Do.STRIKE;
    }

    @NotNull
    private Do strikeOrCancelOrContinueSwinging() {
        int swingTicks = self.getSwingTicks();
        if (swingTicks < Const.swingActionCooldownTicks) return Do.SWING;

        if (isReachable(me, puck)) {
            State nextTurn = current.apply(Go.NOWHERE);
            for (HockeyistPosition enemy : nextTurn.enemies()) {
                if (isReachable(enemy, nextTurn.puck) || isReachable(enemy, nextTurn.me())) return Do.STRIKE;
            }
        }

        if (continueSwinging(current, swingTicks)) return Do.SWING;

        return permissionToShoot(swingTicks, current) ? Do.STRIKE : Do.CANCEL_STRIKE;
    }

    private boolean shouldStartSwinging() {
        State state = current;
        for (int i = 0; i < Const.swingActionCooldownTicks; i++) {
            state = state.moveAllNoCollisions(Go.NOWHERE, Go.NOWHERE);
        }
        return (isReachable(state.me(), state.puck) && permissionToShoot(Const.swingActionCooldownTicks, state)) ||
               continueSwinging(state, Const.swingActionCooldownTicks);
    }

    private static boolean continueSwinging(@NotNull State state, int swingTicks) {
        for (int i = swingTicks; i < MAXIMUM_TICKS_TO_SWING; i++) {
            state = state.moveAllNoCollisions(Go.NOWHERE, Go.NOWHERE);
            if (isReachable(state.me(), state.puck) && permissionToShoot(i, state)) return true;
        }
        return false;
    }

    private static boolean permissionToShoot(int swingTicks, @NotNull State state) {
        double strikePower = effectiveShotPower(swingTicks, state.me());
        return feasibleLocationToShoot(strikePower, state.enemyGoalie(), state.puck.point, state.me()) &&
               angleDifferenceToOptimal(state) <= ALLOWED_ANGLE_DIFFERENCE_TO_SHOOT;
    }

    // TODO: (!) handle overtime with no goalies
    private static double angleDifferenceToOptimal(@NotNull State state) {
        Point puck = state.puck.point;
        Point target = Players.opponentDistantGoalPoint(puck);
        Vec trajectory = Vec.of(puck, target);
        return abs(state.me().angleTo(trajectory));
    }

    // TODO: (!) verify that it works as expected
    private static boolean canScoreWithPass(@NotNull State state) {
        HockeyistPosition me = state.me();
        if (!feasibleLocationToShoot(maximumEffectivePassPower(me), state.enemyGoalie(), state.puck.point, me)) return false;
        Point target = Players.opponentDistantGoalPoint(me.point);
        return abs(Vec.of(state.puck.point, target).angleTo(me.direction())) < Const.passSector / 2;
    }

    private static double effectiveShotPower(int swingTicks, @NotNull HockeyistPosition me) {
        return (Const.strikePowerBaseFactor + Const.strikePowerGrowthFactor * min(swingTicks, Const.maxEffectiveSwingTicks)) * me.strength();
    }

    private static double maximumEffectivePassPower(@NotNull HockeyistPosition me) {
        return Const.passPowerFactor * me.strength();
    }

    public static boolean feasibleLocationToShoot(
            double strikePower,
            @Nullable Point defendingGoalie,
            @NotNull Point puck,
            @NotNull HockeyistPosition attacker
    ) {
        return probabilityToScore(strikePower, defendingGoalie, puck, attacker) > ACCEPTABLE_PROBABILITY_TO_SCORE;
    }

    @NotNull
    private Iterable<Go> iteratePossibleMoves(int step) {
        double d = Const.hockeyistTurnAngleFactor * me.agility();
        Collection<Go> result = new ArrayList<>(51);
        for (int speedup = -1; speedup <= 1; speedup++) {
            for (int t = -step; t <= step; t++) {
                double turn = t * d / step;
                result.add(Go.go(speedup, turn));
            }
        }

        return result;
    }

    @NotNull
    private Go goToPuck() {
        // TODO: (!) improve this heuristic
        Go bestGo = null;
        int bestFirstTickToReach = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (int ticks = 10; ticks <= 40; ticks += 10) {
            for (Go go : iteratePossibleMoves(4)) {
                State state = current;
                for (int i = 0; i < 60 && i < bestFirstTickToReach; i++) {
                    state = state.moveAllNoCollisions(i < ticks ? go : Go.NOWHERE, Go.NOWHERE);
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

        double puckSpeed = Const.struckPuckInitialSpeedFactor * strikePower + attacker.velocity.projection(attacker.direction());

        Vec trajectory = Vec.of(puck, target);
        boolean withinGoalieReach = min(goalieNearby.y, goalieDistant.y) <= puck.y && puck.y <= max(goalieNearby.y, goalieDistant.y);
        double puckStartY = (withinGoalieReach ? puck.y : goalieNearby.y)/* TODO: - puckSpeed * sin(trajectory.angleTo(verticalMovement))*/;
        Line line = Line.between(puck, target);
        Point puckStart = line.when(puckStartY);

        // Ignore friction since no rebounds are expected and the distance is very small
        double time = puckStart.distance(target) / puckSpeed;
        Point goalieFinish = defendingGoalie.shift(verticalMovement.multiply(time * Const.goalieMaxSpeed));

        // Now we should check if distance between the following segments is >= radius(puck) + radius(goalie):
        // (goalie, goalieFinish) and (puckStart, target)
        boolean intersects = signum(Vec.of(puck, goalieNearby).crossProduct(trajectory)) !=
                             signum(Vec.of(puck, goalieFinish).crossProduct(trajectory));
        if (intersects) return 0;

        return min(1, line.project(goalieFinish).distance(goalieFinish) / (Static.HOCKEYIST_RADIUS + Static.PUCK_RADIUS));
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
            double turn = alpha > 0 ? alpha - PI : PI - alpha;
            if (abs(PI - abs(alpha)) < ALLOWED_ANGLE_FOR_GOING_WITH_FULL_SPEED) {
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
                state = state.moveAllNoCollisions(Go.go(-1, 0), Go.NOWHERE);
            }
            while (state.me().velocity.length() > 0.1) {
                state = state.moveAllNoCollisions(Go.go(0, Util.normalize(angle - state.me().angle)), Go.NOWHERE);
            }
            if (state.me().distance(target) < 10) {
                return new Result(Do.NONE, fullBack > 0 ? Go.go(-1, 0) : Go.go(0, Util.normalize(angle - me.angle)));
            }
        }

        return new Result(Do.NONE, goTo(target));
    }
}
