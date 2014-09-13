public class Decision {
    public final long id;
    public final Role role;
    public final Point defensePoint;

    public Decision(long id, @NotNull Role role, @NotNull Point defensePoint) {
        this.id = id;
        this.role = role;
        this.defensePoint = defensePoint;
    }

    public enum Role {
        DEFENSE,
        ATTACK
    }
}
