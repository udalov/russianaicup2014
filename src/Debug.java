import model.Hockeyist;
import model.Puck;
import model.Unit;
import model.World;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;

@SuppressWarnings("MagicNumber")
public class Debug {
    public static final boolean ENABLED = Thread.currentThread().getName().equals("local-vis");

    private static final JTextPane TEXT_AREA;

    static {
        if (ENABLED) {
            final JFrame window = new JFrame();
            window.setTitle("debug");
            window.setSize(400, 600);
            window.setLocation(1060, 80);
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            TEXT_AREA = new JTextPane();
            TEXT_AREA.setContentType("text/html");

            window.add(TEXT_AREA);
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    window.setVisible(true);
                    for (Frame frame : Frame.getFrames()) {
                        if (frame != window) {
                            frame.toFront();
                        }
                    }
                }
            });
        } else {
            TEXT_AREA = null;
        }
    }

    public static void update(@NotNull World world) {
        if (!ENABLED) return;

        Hockeyist[] hockeyists = world.getHockeyists();
        Arrays.sort(hockeyists, new Comparator<Hockeyist>() {
            @Override
            public int compare(@NotNull Hockeyist o1, @NotNull Hockeyist o2) {
                if (o1.isTeammate() != o2.isTeammate()) return o1.isTeammate() ? -1 : 1;
                return o1.getOriginalPositionIndex() - o2.getOriginalPositionIndex();
            }
        });

        class DebugTextBuilder {
            private final StringBuilder sb = new StringBuilder();

            {
                sb.append("<div style='font-family: Menlo, Consolas; font-size: 10'>");
            }

            void str(@NotNull String s) {
                sb.append(s);
                newLine();
            }

            void color(@NotNull String color) {
                sb.append("<div style='color: #").append(color).append("'>");
            }

            void endColor() {
                sb.append("</div>");
            }

            @NotNull
            private String format(double value) {
                return String.format("%.3f", value);
            }

            void unit(@NotNull Unit unit) {
                double vx = unit.getSpeedX();
                double vy = unit.getSpeedY();
                str("speed (" + format(vx) + ", " + format(vy) + ") value " + format(Math.hypot(vx, vy)));
                str("angle " + format(unit.getAngle()) + ", speed " + format(unit.getAngularSpeed()));
            }

            void newLine() {
                sb.append("<br/>");
            }

            @Override
            public String toString() {
                return sb.toString() + "</div>";
            }
        }

        DebugTextBuilder b = new DebugTextBuilder();

        Puck puck = world.getPuck();
        b.str("puck at " + puck);
        b.unit(puck);
        b.newLine();
        for (Hockeyist hockeyist : hockeyists) {
            b.color(hockeyist.isTeammate() ? "007700" : "770000");
            b.str(hockeyist.toString());
            b.endColor();
            b.unit(hockeyist);
            b.newLine();
        }

        TEXT_AREA.setText(b.toString());
    }
}
