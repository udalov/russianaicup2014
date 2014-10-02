import model.Hockeyist;
import model.HockeyistType;
import model.World;

import java.awt.*;
import java.util.Stack;

import static java.lang.Math.*;

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
        renderGoaliePosition();
        // renderProbabilityToScore();
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

    private void renderGoaliePosition() {
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getType() == HockeyistType.GOALIE) {
                g.drawString(String.format("goalie position %.3f", hockeyist.getY()), 220, 70);
                return;
            }
        }
    }

    private void renderProbabilityToScore() {
        Hockeyist puckOwner = findPuckOwner();
        if (puckOwner == null || puckOwner.getPlayerId() != Players.me.getId()) return;

        State state = State.of(puckOwner, world);
        int enemyGoalie = findEnemyGoalie(state);
        Position gp = state.pos[enemyGoalie];

        save();
        for (int x = (int) Static.CENTER.x + 2; x <= Const.rinkRight; x += 4) {
            for (int y = (int) Const.rinkTop + 2; y <= Const.rinkBottom; y += 4) {
                Point puck = Point.of(x, y);
                Vec direction = Vec.of(puck, findFarCorner(puck)).normalize();
                Point me = puck.shift(direction.multiply(-55));
                State cur = new State(state.pos, new PuckPosition(state.puck.puck, x, y, 0, 0, direction.angle()), state.myIndex);
                cur.pos[cur.myIndex] = new HockeyistPosition(state.pos[state.myIndex].hockeyist, me.x, me.y,
                                                             state.me().speedX, state.me().speedY, Vec.direction(puckOwner).angle());
                cur.pos[enemyGoalie] = new HockeyistPosition(state.pos[enemyGoalie].hockeyist, gp.x, max(min(y, 530), 390),
                                                             gp.speedX, gp.speedY, gp.angle);
                float p = (float) MakeTurn.probabilityToScore(cur, 1);
                g.setColor(new Color(p, 0f, 1 - p));
                g.fillRect(x - 2, y - 2, 5, 5);
            }
        }
        restore();
    }

    private static int findEnemyGoalie(@NotNull State state) {
        for (int i = 0; i < state.pos.length; i++) {
            Hockeyist hockeyist = state.pos[i].hockeyist;
            if (hockeyist.getType() == HockeyistType.GOALIE && hockeyist.getPlayerId() == Players.opponent.getId()) return i;
        }
        throw new AssertionError();
    }

    @NotNull
    private static Point findFarCorner(@NotNull Point me) {
        double y = me.y < Static.CENTER.y
                   ? Const.goalNetTop + Const.goalNetHeight - Static.PUCK_RADIUS
                   : Const.goalNetTop + Static.PUCK_RADIUS;
        return Point.of(Const.rinkRight, y);
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

