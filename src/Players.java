import model.Player;
import model.World;

public class Players {
    public static Player me;
    public static Player opponent;

    public static Vec attack;
    public static Vec defense;

    public static Point myGoalCenter;
    public static Point opponentGoalCenter;

    public static Point opponentBottomCorner;
    public static Point opponentBottomGoalPoint;
    public static Point opponentTopCorner;
    public static Point opponentTopGoalPoint;

    public static void initialize(@NotNull World world) {
        me = world.getMyPlayer();
        opponent = world.getOpponentPlayer();

        attack = Vec.of(me.getNetFront() < Static.CENTER.x ? 1 : -1, 0);
        defense = attack.multiply(-1);

        myGoalCenter = Point.of(me.getNetFront(), (me.getNetTop() + me.getNetBottom()) / 2);
        opponentGoalCenter = Point.of(opponent.getNetFront(), (opponent.getNetTop() + opponent.getNetBottom()) / 2);

        opponentBottomCorner = Point.of(opponent.getNetFront(), opponent.getNetBottom());
        opponentBottomGoalPoint = Point.of(opponent.getNetFront(), opponent.getNetBottom() - Static.PUCK_RADIUS - Solution.GOAL_POINT_SHIFT);
        opponentTopCorner = Point.of(opponent.getNetFront(), opponent.getNetTop());
        opponentTopGoalPoint = Point.of(opponent.getNetFront(), opponent.getNetTop() + Static.PUCK_RADIUS + Solution.GOAL_POINT_SHIFT);
    }

    @NotNull
    public static Point opponentNearbyCorner(@NotNull Point me) {
        return me.y > Static.CENTER.y ? opponentBottomCorner : opponentTopCorner;
    }

    @NotNull
    public static Point opponentDistantCorner(@NotNull Point me) {
        return me.y > Static.CENTER.y ? opponentTopCorner : opponentBottomCorner;
    }

    @NotNull
    public static Point opponentDistantGoalPoint(@NotNull Point me) {
        return me.y > Static.CENTER.y ? opponentTopGoalPoint : opponentBottomGoalPoint;
    }
}
