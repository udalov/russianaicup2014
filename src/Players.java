import model.Player;
import model.World;

public class Players {
    public static Player me;
    public static Player opponent;

    public static Vec attack;
    public static Vec defense;

    public static void initialize(@NotNull World world) {
        me = world.getMyPlayer();
        opponent = world.getOpponentPlayer();
        attack = Vec.of(me.getNetFront() < Static.CENTER.x ? 1 : -1, 0);
        defense = attack.multiply(-1);
    }

    @NotNull
    public static Point opponentNearbyCorner(@NotNull Point me) {
        return Point.of(opponent.getNetFront(), me.y > Static.CENTER.y ? opponent.getNetBottom() : opponent.getNetTop());
    }

    @NotNull
    public static Point opponentDistantCorner(@NotNull Point me) {
        return Point.of(opponent.getNetFront(), me.y <= Static.CENTER.y ? opponent.getNetBottom() : opponent.getNetTop());
    }
}
