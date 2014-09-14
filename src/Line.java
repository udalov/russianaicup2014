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

    @NotNull
    public Point at(double x) {
        //noinspection MagicNumber
        if (Math.abs(B) < 1e-6) {
            System.out.println("warning: at() called on a vertical line " + this);
        }
        double y = (C - A * x) / B;
        return Point.of(x, y);
    }

    @Override
    public String toString() {
        return String.format("(A=%.3f, B=%.3f, C=%.3f)", A, B, C);
    }
}
