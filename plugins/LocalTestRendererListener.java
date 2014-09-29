import model.Game;
import model.World;

import java.awt.*;

@SuppressWarnings({"MethodMayBeStatic", "UnusedParameters"})
public final class LocalTestRendererListener {
    public void beforeDrawScene(@NotNull Graphics graphics, @NotNull World world, @NotNull Game game, double scale) throws Exception {
        Graphics2D g = (Graphics2D) graphics;
        g.scale(scale, scale);
        new MyRenderer(g, world).renderBefore();
        g.scale(1 / scale, 1 / scale);
    }

    public void afterDrawScene(@NotNull Graphics graphics, @NotNull World world, @NotNull Game game, double scale) {
        Graphics2D g = (Graphics2D) graphics;
        g.scale(scale, scale);
        new MyRenderer(g, world).renderAfter();
        g.scale(1 / scale, 1 / scale);
    }
}
