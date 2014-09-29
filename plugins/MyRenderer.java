import model.Hockeyist;
import model.World;

import java.awt.*;
import java.util.Stack;

import static java.lang.Math.round;

@SuppressWarnings("MagicNumber")
public class MyRenderer {
    private final Graphics2D g;
    private final World world;

    private final Stack<Color> savedColors = new Stack<>();
    private final Stack<Stroke> savedStrokes = new Stack<>();

    public MyRenderer(@NotNull Graphics2D g, @NotNull World world) {
        this.g = g;
        this.world = world;
    }

    public void renderBefore() {
        renderPuckAndOwnerSpeed();
        renderPuckOwnerDirection();
    }

    public void renderAfter() {
    }

    // -------------------------------------------------------------------------------------------------------------

    private void renderPuckAndOwnerSpeed() {
        g.drawString(String.format("puck speed %.3f", Util.speed(world.getPuck())), 220, 30);

        Hockeyist puckOwner = findPuckOwner();
        if (puckOwner != null) {
            g.drawString(String.format("puck owner speed %.3f", Util.speed(puckOwner)), 220, 50);
        }
    }

    private void renderPuckOwnerDirection() {
        Hockeyist puckOwner = findPuckOwner();
        if (puckOwner == null) return;

        Point puck = Point.of(world.getPuck());
        Point target = Point.of(puckOwner).shift(Vec.direction(puckOwner).multiply(500));

        save();
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{10f}, 0f));
        g.setColor(new Color(0, 0, 0, 0x40));
        drawLine(puck.x, puck.y, target.x, target.y);
        restore();
    }

    @Nullable
    private Hockeyist findPuckOwner() {
        try {
            return Util.findById(world, world.getPuck().getOwnerHockeyistId());
        } catch (AssertionError e) {
            return null;
        }
    }

    private void save() {
        savedColors.push(g.getColor());
        savedStrokes.push(g.getStroke());
    }

    private void restore() {
        g.setStroke(savedStrokes.pop());
        g.setColor(savedColors.pop());
    }

    private void drawLine(double x1, double y1, double x2, double y2) {
        g.drawLine((int) round(x1), (int) round(y1), (int) round(x2), (int) round(y2));
    }
}

