public class Result {
    public static final Result SWING = new Result(Do.SWING, Go.go(0, 0) /* ignored */);

    public final Do action;
    public final Go direction;

    public Result(@NotNull Do action, @NotNull Go direction) {
        this.action = action;
        this.direction = direction;
    }

    @NotNull
    public static Result debugDoNothing() {
        return new Result(Do.NONE, Go.go(0, 0));
    }

    @Override
    public String toString() {
        return action + ", " + direction;
    }
}
