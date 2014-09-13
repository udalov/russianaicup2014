import model.Hockeyist;
import model.World;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;

@SuppressWarnings("MagicNumber")
public class Debug {
    public static final boolean ENABLED = Thread.currentThread().getName().equals("local-vis");

    private static final JTextArea TEXT_AREA;

    static {
        if (ENABLED) {
            final JFrame window = new JFrame();
            window.setTitle("debug");
            window.setSize(400, 600);
            window.setLocation(1060, 80);
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            TEXT_AREA = new JTextArea();
            TEXT_AREA.setFont(new Font("Consolas", Font.PLAIN, 10));
            TEXT_AREA.setFont(new Font("Menlo", Font.PLAIN, 10));

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

        StringBuilder sb = new StringBuilder();
        for (Hockeyist hockeyist : hockeyists) {
            sb.append(hockeyist.toString());
            sb.append("\n");
            sb.append("speed (");
            sb.append(format(hockeyist.getSpeedX()));
            sb.append(", ");
            sb.append(format(hockeyist.getSpeedY()));
            sb.append("\n");
            sb.append("angle ");
            sb.append(format(hockeyist.getAngle()));
            sb.append("\n");
            sb.append("angular speed ");
            sb.append(format(hockeyist.getAngularSpeed()));
            sb.append("\n");
            sb.append("\n");
        }

        TEXT_AREA.setText(sb.toString());
    }

    private static String format(double x) {
        return String.format("%.3f", x);
    }
}
