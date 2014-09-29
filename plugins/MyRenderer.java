import model.Hockeyist;
import model.World;

import java.awt.*;

@SuppressWarnings("MagicNumber")
public class MyRenderer {
    private final Graphics2D g;
    private final World world;

    public MyRenderer(@NotNull Graphics2D g, @NotNull World world) {
        this.g = g;
        this.world = world;
    }

    public void renderBefore() {
        renderPuckAndOwnerSpeed();
    }

    public void renderAfter() {
    }

    private void renderPuckAndOwnerSpeed() {
        g.drawString(String.format("puck speed %.3f", Util.speed(world.getPuck())), 220, 30);

        long puckOwnerId = world.getPuck().getOwnerHockeyistId();
        if (puckOwnerId != -1) {
            Hockeyist puckOwner = Util.findById(world, puckOwnerId);
            g.drawString(String.format("puck owner speed %.3f", Util.speed(puckOwner)), 220, 50);
        }
    }
}

