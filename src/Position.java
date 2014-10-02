public abstract class Position {
    public final double x;
    public final double y;
    public final double speedX;
    public final double speedY;
    public final double angle;

    public Position(double x, double y, double speedX, double speedY, double angle) {
        this.x = x;
        this.y = y;
        this.speedX = speedX;
        this.speedY = speedY;
        this.angle = angle;
    }

    @NotNull
    public Point point() {
        return Point.of(x, y);
    }

    @NotNull
    public Vec velocity() {
        return Vec.of(speedX, speedY);
    }

    @NotNull
    public Vec direction() {
        return Vec.of(angle);
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f) speed (%.3f, %.3f) angle %.3f", x, y, speedX, speedY, angle);
    }
}
