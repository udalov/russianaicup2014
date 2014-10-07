import javax.swing.event.MouseInputListener;
import java.awt.event.MouseEvent;

public class MyMouseListener implements MouseInputListener {
    private final double scale;

    public volatile Point pressed = null;
    public volatile Point current = null;

    public MyMouseListener(double scale) {
        this.scale = scale;
    }

    @Override
    public void mouseClicked(@NotNull MouseEvent e) {
        pressed = null;
    }

    @Override
    public void mousePressed(@NotNull MouseEvent e) {
        pressed = point(e);
        current = pressed;
    }

    @Override
    public void mouseReleased(@NotNull MouseEvent e) {
        MakeTurn.debugTarget = pressed;
        MakeTurn.debugDirection = current;
        pressed = null;
        current = null;
    }

    @Override
    public void mouseEntered(@NotNull MouseEvent e) {
    }

    @Override
    public void mouseExited(@NotNull MouseEvent e) {
    }

    @Override
    public void mouseDragged(@NotNull MouseEvent e) {
        current = point(e);
    }

    @Override
    public void mouseMoved(@NotNull MouseEvent e) {
    }

    @NotNull
    private Point point(@NotNull MouseEvent e) {
        return Point.of(e.getX() / scale, e.getY() / scale);
    }
}
