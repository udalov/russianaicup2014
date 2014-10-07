import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class MyKeyboardListener implements KeyListener {
    public volatile boolean isActionPressed = false;

    @Override
    public void keyTyped(@NotNull KeyEvent e) {
    }

    @Override
    public void keyPressed(@NotNull KeyEvent e) {
        if (isActionKey(e)) {
            isActionPressed = true;
            MakeTurn.goToPuck = true;
        }
    }

    @Override
    public void keyReleased(@NotNull KeyEvent e) {
        if (isActionKey(e)) {
            isActionPressed = false;
            MakeTurn.goToPuck = false;
        }
    }

    private static boolean isActionKey(@NotNull KeyEvent e) {
        return e.getKeyChar() == 'z';
    }
}
