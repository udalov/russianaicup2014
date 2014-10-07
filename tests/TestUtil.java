import junit.framework.ComparisonFailure;

import static java.lang.StrictMath.abs;

public class TestUtil {
    public static void assertEq(double expected, double actual, double eps) {
        if (abs(expected - actual) > eps) {
            String e = String.format("%.9f", expected);
            String a = String.format("%.9f", actual);
            throw new ComparisonFailure("expected " + e + ", actual " + a, e, a);
        }
    }
}
