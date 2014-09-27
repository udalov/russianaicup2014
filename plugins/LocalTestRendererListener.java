import model.Game;
import model.Hockeyist;
import model.World;

import java.awt.*;

@SuppressWarnings({"MethodMayBeStatic", "MagicNumber", "UnusedParameters"})
public final class LocalTestRendererListener {
    public void beforeDrawScene(@NotNull Graphics graphics, @NotNull World world, @NotNull Game game, double scale) {
        graphics.drawString(String.format("puck speed %.3f", Vec.speedOf(world.getPuck()).length()), 200, 24);

        long puckOwnerId = world.getPuck().getOwnerHockeyistId();
        if (puckOwnerId != -1) {
            Hockeyist puckOwner = Util.findById(world, puckOwnerId);
            graphics.drawString(String.format("puck owner speed %.3f", Vec.speedOf(puckOwner).length()), 200, 48);
        }
/*
        graphics.drawRect(100, 100, 500, 500);
        for (Hockeyist hockeyist : world.getHockeyists()) {
            graphics.drawArc((int) hockeyist.getX() - 50, (int) hockeyist.getY() - 50, 100, 100, 0, 360);
        }
*/
    }

    public void afterDrawScene(@NotNull Graphics graphics, @NotNull World world, @NotNull Game game, double scale) {
/*
        graphics.drawRect(200, 200, 550, 550);
        for (Hockeyist hockeyist : world.getHockeyists()) {
            graphics.drawArc((int) hockeyist.getX() - 40, (int) hockeyist.getY() - 40, 80, 80, 0, 360);
        }
*/
    }
}
