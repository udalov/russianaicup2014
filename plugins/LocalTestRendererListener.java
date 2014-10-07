import model.Game;
import model.World;

import java.awt.*;

@SuppressWarnings({"MethodMayBeStatic", "UnusedParameters"})
public final class LocalTestRendererListener {
    private volatile MyMouseListener mouseListener = null;
    private volatile MyKeyboardListener keyboardListener = null;

    public void beforeDrawScene(@NotNull Graphics graphics, @NotNull World world, @NotNull Game game, double scale) throws Exception {
        if (mouseListener == null) {
            mouseListener = new MyMouseListener(scale);
            Component panel = Frame.getFrames()[0].getComponent(0);
            panel.addMouseListener(mouseListener);
            panel.addMouseMotionListener(mouseListener);
        }

        if (keyboardListener == null) {
            keyboardListener = new MyKeyboardListener();
            Frame.getFrames()[0].addKeyListener(keyboardListener);
        }

        Graphics2D g = (Graphics2D) graphics;
        g.scale(scale, scale);
        try {
            new MyRenderer(g, world, mouseListener, keyboardListener).renderBefore();
        } finally {
            g.scale(1 / scale, 1 / scale);
        }
    }

    public void afterDrawScene(@NotNull Graphics graphics, @NotNull World world, @NotNull Game game, double scale) {
        Graphics2D g = (Graphics2D) graphics;
        g.scale(scale, scale);
        try {
            new MyRenderer(g, world, mouseListener, keyboardListener).renderAfter();
        } finally {
            g.scale(1 / scale, 1 / scale);
        }
    }
}
