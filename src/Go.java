public class Go {
    public final double speedup;
    public final double turn;

    private Go(double speedup, double turn) {
        this.speedup = speedup;
        this.turn = turn;
    }

    @NotNull
    public static Go go(double speedup, double turn) {
        return new Go(speedup, turn);
    }

    @Override
    public String toString() {
        return String.format("speedup %.3f turn %.3f", speedup, turn);
    }
}
