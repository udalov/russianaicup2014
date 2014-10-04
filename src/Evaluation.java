import static java.lang.StrictMath.*;

public abstract class Evaluation {
    public final State startingState;

    public Evaluation(@NotNull State startingState) {
        this.startingState = startingState;
    }

    public abstract double evaluate(@NotNull State state);

    // TODO: (!) handle overtime with no goalies
    public static double angleDifferenceToOptimal(@NotNull State state) {
        Point puck = state.puck.point;
        Point goalNetNearby = Players.opponentNearbyCorner(puck);
        Point goalNetDistant = Players.opponentDistantCorner(puck);
        Vec verticalMovement = Vec.of(goalNetNearby, goalNetDistant).normalize();
        Point target = goalNetDistant.shift(verticalMovement.multiply(-Static.PUCK_RADIUS));
        Vec trajectory = Vec.of(puck, target);
        return abs(state.me().angleTo(trajectory));
    }

    public static double angleDifferenceAfterSwing(@NotNull State state) {
        for (int i = 0; i < 20; i++) {
            state = state.apply(Go.go(0, 0));
        }
        return angleDifferenceToOptimal(state);
    }

    public static class AttackOnEnemySide extends Evaluation {
        public AttackOnEnemySide(@NotNull State startingState) {
            super(startingState);
        }

        @Override
        public double evaluate(@NotNull State state) {
            double penalty = 0;

            HockeyistPosition me = state.me();

            Point[] attackPoints = MakeTurn.determineAttackPoints(state);
            double distanceToAttackPoint = min(me.distance(attackPoints[0]), me.distance(attackPoints[1]));
            penalty += distanceToAttackPoint / 2;

            double dangerousAngle = PI / 2;

            for (HockeyistPosition hockeyist : state.all()) {
                double distance = me.distance(hockeyist);
                if (!hockeyist.teammate()) penalty += sqrt(max(150 - distance, 0));

                double angleToEnemy = abs(me.angleTo(Vec.of(me, hockeyist)));
                if (angleToEnemy <= dangerousAngle) {
                    double convergenceSpeed = me.velocity.length() < 1e-6 ? 0 : 1 - hockeyist.velocity.projection(me.velocity);
                    if (distance <= 150 || convergenceSpeed >= 20) {
                        penalty += (hockeyist.teammate() ? 30 : 150) * (1 - angleToEnemy / dangerousAngle);
                    }
                }
            }

            Point future = me.point.shift(me.direction().multiply(10));
            penalty += Util.sqr(max(Const.rinkLeft - future.x, 0)) * 10;
            penalty += Util.sqr(max(future.x - Const.rinkRight, 0)) * 10;
            penalty += Util.sqr(max(Const.rinkTop - future.y, 0)) * 10;
            penalty += Util.sqr(max(future.y - Const.rinkBottom, 0)) * 10;

            // penalty += pow(max(15 - myVelocity.project(myDirection).length(), 0), 1.1);

            for (Point corner : Static.CORNERS) {
                // TODO: investigate if it works as expected
                penalty += Util.sqr(max(150 - me.distance(corner), 0));
            }

            if (distanceToAttackPoint < 100) {
                penalty -= pow(1 - angleDifferenceAfterSwing(state) / PI, 20) * 1000;
                penalty += max(me.velocity.length() - 3.5, 0) * 10; // TODO: correctly determine my effective speed
            } else {
                penalty -= 1000;
                penalty += 20;
            }

            penalty += max(50 - startingState.me().distance(me), 0);

            penalty += max(abs(me.angleTo(me.velocity)) - PI / 2, 0) * 50;

            if (me.distance(Players.myGoalCenter) < me.distance(Players.opponentGoalCenter)) {
                penalty += Util.sqr(me.angleTo(Players.attack)) * 50;
            }

/*
            Position enemyGoalie = state.enemyGoalie();
            if (enemyGoalie != null) penalty += max(200 - enemyGoalie.point().distance(me), 0);

            // TODO: angle diff should be very small for non-0
            double probabilityToScore = MakeTurn.probabilityToScore(state, 1);
            if (probabilityToScore > 0.7) {
                penalty -= Util.sqr(probabilityToScore * pow(1 - angleDifferenceToOptimal(state) / PI, 4)) * 10;
            }
*/

            return -penalty;
        }
    }
}
