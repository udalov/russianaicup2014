import model.Game;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class GameConstantReader {
    public static void invoke(Game game) {
        try {
            PrintWriter out = new PrintWriter("game-const.txt");
            for (Field field : Game.class.getDeclaredFields()) {
                String name = field.getName();
                if (name.equals("randomSeed")) continue;
                Method getter = Game.class.getDeclaredMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
                out.println(name + " " + getter.invoke(game));
            }
            out.close();
            System.out.println("0 OK, 0:1");
            System.exit(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
