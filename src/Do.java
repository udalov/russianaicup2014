import model.ActionType;

public class Do {
    public static final Do NOTHING = new Do(ActionType.NONE);
    public static final Do TAKE_PUCK = new Do(ActionType.TAKE_PUCK);
    public static final Do SWING = new Do(ActionType.SWING);
    public static final Do STRIKE = new Do(ActionType.STRIKE);
    public static final Do CANCEL_STRIKE = new Do(ActionType.CANCEL_STRIKE);

    public final ActionType type;
    public final double passPower;
    public final double passAngle;
    public final int teammateIndex;

    private Do(@NotNull ActionType type, double passPower, double passAngle, int teammateIndex) {
        this.type = type;
        this.passPower = passPower;
        this.passAngle = passAngle;
        this.teammateIndex = teammateIndex;
    }

    private Do(@NotNull ActionType type) {
        this(type, 0, 0, -1);
    }

    @NotNull
    public static Do pass(double passPower, double passAngle) {
        return new Do(ActionType.PASS, passPower, passAngle, -1);
    }

    @NotNull
    public static Do substitute(int teammateIndex) {
        return new Do(ActionType.SUBSTITUTE, 0, 0, teammateIndex);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
