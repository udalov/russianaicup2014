public class Decision {
    public final long id;
    public final Role role;
    public final Point dislocation;

    public Decision(long id, @NotNull Role role, @NotNull Point dislocation) {
        this.id = id;
        this.role = role;
        this.dislocation = dislocation;
    }

    public enum Role {
        DEFENSE,
        MIDFIELD,
        ATTACK
    }
}
