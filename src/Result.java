public class Result {
    public static final Result NOTHING = new Result(Do.NOTHING, Go.NOWHERE);

    public final Do action;
    public final Go direction;

    public Result(@NotNull Do action, @NotNull Go direction) {
        this.action = action;
        this.direction = direction;
    }

    @Override
    public String toString() {
        return action + ", " + direction;
    }
}
