import model.Hockeyist;
import model.HockeyistState;
import model.HockeyistType;
import model.World;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import static java.lang.Math.*;

@SuppressWarnings("MagicNumber")
public class MyRenderer {
    private final Graphics2D g;
    private final World world;
    private final MyMouseListener mouse;
    private final MyKeyboardListener keyboard;

    private final Stack<Color> savedColors = new Stack<>();
    private final Stack<Stroke> savedStrokes = new Stack<>();

    public MyRenderer(@NotNull Graphics2D g, @NotNull World world, @NotNull MyMouseListener mouse, @NotNull MyKeyboardListener keyboard) {
        this.g = g;
        this.world = world;
        this.mouse = mouse;
        this.keyboard = keyboard;
    }

    public void renderBefore() {
        renderPuckSpeedAndGoaliePosition();
        renderPuckOwnerDirection();
        // renderProbabilityToScore();
        // renderFuture();
        renderLining();
        renderRolesAndInfo();
        renderMouseCommandSegment();
        renderKeyboardActionFlag();
    }

    public void renderAfter() {
    }

    // -------------------------------------------------------------------------------------------------------------

    private void renderPuckSpeedAndGoaliePosition() {
        g.drawString(String.format("puck speed %.3f", Util.speed(world.getPuck())), 220, 30);

        Hockeyist puckOwner = findPuckOwner();
        if (puckOwner != null) {
            g.drawString(String.format("puck owner speed %.3f", Util.speed(puckOwner)), 220, 48);
        }

        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getType() == HockeyistType.GOALIE) {
                g.drawString(String.format("goalie position %.3f", hockeyist.getY()), 220, 66);
                return;
            }
        }
    }

    private void renderPuckOwnerDirection() {
        Hockeyist puckOwner = findPuckOwner();
        if (puckOwner == null) return;

        Point puck = Point.of(world.getPuck());
        Point target = Point.of(puckOwner).shift(Vec.direction(puckOwner).multiply(700));

        save();
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{10f}, 0f));
        g.setColor(new Color(0, 0, 0, 0x40));
        drawLine(puck.x, puck.y, target.x, target.y);
        restore();
    }

    private static final Double PROBABILITY_TO_SCORE_THRESHOLD = null; // MakeTurn.ACCEPTABLE_PROBABILITY_TO_SCORE;

    private void renderProbabilityToScore() {
        Hockeyist puckOwner = findPuckOwner();
        if (puckOwner == null || puckOwner.getPlayerId() != Players.me.getId()) return;

        HockeyistPosition me = State.of(puckOwner, world).me;

        save();
        for (int x = (int) Static.CENTER.x + 2; x <= Const.rinkRight; x += 4) {
            for (int y = (int) Const.rinkTop + 2; y <= Const.rinkBottom; y += 4) {
                Point puck = Point.of(x, y);
                Vec direction = Vec.of(puck, Players.opponentDistantGoalPoint(puck)).normalize();
                Point myLocation = puck.shift(direction.multiply(-Const.puckBindingRange));
                HockeyistPosition attacker =
                        new HockeyistPosition(me.hockeyist, myLocation, me.velocity, 0, direction.angle(), 0);
                float p = (float) Solution.probabilityToScore(1, attacker.direction(), max(min(y, 530), 390), puck, attacker);
                //noinspection ConstantConditions
                if (PROBABILITY_TO_SCORE_THRESHOLD != null) {
                    p = p > PROBABILITY_TO_SCORE_THRESHOLD ? 1 : 0;
                }
                g.setColor(new Color(p, 0f, 1 - p));
                g.fillRect(x - 2, y - 2, 5, 5);
            }
        }
        restore();
    }

    private static final int FUTURE_STEPS_TO_APPLY = 1;

    private void renderFuture() {
        Hockeyist puckOwner = findPuckOwner();
        State current = State.of(puckOwner != null ? puckOwner : world.getHockeyists()[0], world);
        State state = current;
        for (int i = 0; i < FUTURE_STEPS_TO_APPLY; i++) {
            state = state.apply(Go.NOWHERE);
        }
        System.out.println(world + " real " + current.puck + " forecast " + state.puck);

        save();
        g.setColor(new Color(128, 128, 128));
        for (HockeyistPosition position : state.pos) {
            int x = (int) round(position.point.x);
            int y = (int) round(position.point.y);
            int r = (int) Static.HOCKEYIST_RADIUS;
            g.drawArc(x - r, y - r, 2 * r, 2 * r, 0, 360);
            drawLine(x, y, x + r * Util.fastCos(position.angle), y + r * Util.fastSin(position.angle));
        }
        {
            int x = (int) round(state.puck.point.x);
            int y = (int) round(state.puck.point.y);
            int r = (int) Static.PUCK_RADIUS;
            g.setColor(new Color(200, 200, 200));
            g.fillArc(x - r, y - r, 2 * r, 2 * r, 0, 360);
        }
        restore();
    }

    private void renderLining() {
        Point c = Static.CENTER;

        save();
        g.setColor(new Color(200, 200, 200));
        g.setStroke(new BasicStroke(2f));
        drawLine(c.x, Const.rinkTop, c.x, Const.rinkBottom);
        drawLine(Const.rinkLeft - Const.goalNetWidth, c.y, Const.rinkRight + Const.goalNetWidth, c.y);
        restore();

        save();
        g.setColor(new Color(200, 200, 200));
        for (int half = -1; half <= 1; half += 2) {
            for (int line = 0; line <= 500; line += 100) {
                double x = c.x + line * half;
                drawLine(x, Const.rinkTop, x, Const.rinkBottom);
            }
        }
        for (int half = -1; half <= 1; half += 2) {
            for (int line = 100; line <= 300; line += 100) {
                double y = c.y + line * half;
                drawLine(Const.rinkLeft, y, Const.rinkRight, y);
            }
        }
        restore();
    }

    private void renderRolesAndInfo() {
        Team team = MyStrategy.TEAM;

        ArrayList<String> info = new ArrayList<>(3);
        for (Hockeyist ally : world.getHockeyists()) {
            if (ally.getType() == HockeyistType.GOALIE || ally.getState() == HockeyistState.RESTING) continue;
            if (ally.getPlayerId() != Players.me.getId()) continue;
            Decision decision = team.getDecision(ally.getId());
            info.add((ally.getTeammateIndex() + 1) + " " + decision.role + " " + ally + "#" + MyStrategy.LAST_RESULT.get(ally.getId()));
        }
        Collections.sort(info);

        int y = 30;
        for (String s : info) {
            g.drawString(s.substring(0, s.indexOf('#')), 380, y);
            y += 18;
            g.drawString(s.substring(s.indexOf('#') + 1), 380, y);
            y += 18;
        }
    }

    private void renderMouseCommandSegment() {
        Point p = mouse.pressed;
        if (p != null) {
            Point q = mouse.current;
            drawLine(p.x, p.y, q.x, q.y);
        }
    }

    private void renderKeyboardActionFlag() {
        if (keyboard.isActionPressed) {
            save();
            g.setColor(new Color(100, 0, 0));
            g.drawString("ACTION", 24, 100);
            restore();
        }
    }

    @Nullable
    private Hockeyist findPuckOwner() {
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getId() == world.getPuck().getOwnerHockeyistId()) return hockeyist;
        }
        return null;
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

