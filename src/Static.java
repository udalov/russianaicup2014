public class Static {
    public static final Point CENTER =
            Point.of((Const.rinkLeft + Const.rinkRight) / 2, (Const.rinkTop + Const.rinkBottom) / 2);

    public static final Point[] CORNERS = {
            Point.of(Const.rinkLeft, Const.rinkTop),
            Point.of(Const.rinkLeft, Const.rinkBottom),
            Point.of(Const.rinkRight, Const.rinkTop),
            Point.of(Const.rinkRight, Const.rinkBottom)
    };
}
