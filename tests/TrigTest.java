import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

import static java.lang.StrictMath.*;

public class TrigTest extends TestCase {
    public void testSinCos() {
        int range = 1000;
        for (int i = -range; i <= range; i++) {
            double angle = i * PI / range;
            double expectedSin = sin(angle);
            double actualSin = Util.fastSin(angle);
            assertEq(angle, expectedSin, actualSin);

            double expectedCos = cos(angle);
            double actualCos = Util.fastCos(angle);
            assertEq(angle, expectedCos, actualCos);
        }
    }

    private static void assertEq(double angle, double expectedSin, double actualSin) {
        if (abs(expectedSin - actualSin) > 1e-8) {
            String e = String.format("%.9f", expectedSin);
            String a = String.format("%.9f", actualSin);
            throw new ComparisonFailure(
                    String.format("angle %.9f, expected %s, actual %s", angle, e, a),
                    e, a
            );
        }
    }
}
