import model.Player;
import model.World;

public class Players {
    public static Player me;
    public static Player opponent;

    public static void initialize(@NotNull World world) {
        me = world.getMyPlayer();
        opponent = world.getOpponentPlayer();
    }
}
