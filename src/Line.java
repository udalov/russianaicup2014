import static java.lang.Math.abs;

public class Line {
    // Ax + By = C
    private final double A;
    private final double B;
    private final double C;

    private Line(double A, double B, double C) {
        this.A = A;
        this.B = B;
        this.C = C;
    }

    @NotNull
    public static Line between(@NotNull Point a, @NotNull Point b) {
        double A = a.y - b.y;
        double B = b.x - a.x;
        double C = A * a.x + B * a.y;
        return new Line(A, B, C);
    }

    public boolean contains(@NotNull Point p) {
        return zero(A * p.x + B * p.y - C);
    }

    @NotNull
    public Point at(double x) {
        if (zero(B)) {
            System.out.println("warning: at() called on a vertical line " + this);
        }
        double y = (C - A * x) / B;
        return Point.of(x, y);
    }

    @NotNull
    public Point when(double y) {
        if (zero(A)) {
            System.out.println("warning: when() called on a horizontal line " + this);
        }
        double x = (C - B * y) / A;
        return Point.of(x, y);
    }

    @NotNull
    public Point project(@NotNull Point p) {
        if (contains(p)) return p;
        double D = B * p.x - A * p.y;
        // Ax + By = C
        // Bx - Ay = D
        double E = A * A + B * B;
        double x = (B * D + A * C) / E;
        double y = (B * C - A * D) / E;
        Point result = Point.of(x, y);
        if (!contains(result)) {
            System.out.println("warning: projection of " + p + " on line " + this + " is " + result + " not on that line");
        }
        return result;
    }

    private static boolean zero(double d) {
        //noinspection MagicNumber
        return abs(d) < 1e-6;
    }

    @Override
    public String toString() {
        return String.format("(A=%.3f, B=%.3f, C=%.3f)", A, B, C);
    }
}
