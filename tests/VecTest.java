import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.abs;

@SuppressWarnings("MagicNumber")
public class VecTest extends TestCase {
    public void testSimple() {
        assertVec(-1, 1, Vec.of(PI / 2).plus(Vec.of(-1, 0)));
        assertVec(-84, 478, Vec.of(42, -239).multiply(-2));
        assertEq(-53, Vec.of(4, 5).innerProduct(Vec.of(-2, -9)));
    }

    public void testProjection() {
        Vec axis = Vec.of(-2, 0);
        assertVec(-2, 0, axis.project(axis));
        assertVec(1, 0, Vec.of(1, 0).project(axis));
        assertVec(0, 0, Vec.of(0, 1).project(axis));
        assertVec(0, 0, Vec.of(0, -1).project(axis));

        assertVec(-1, 1, Vec.of(0, 2).project(Vec.of(3 * PI / 4)));
    }

    public void testAngle() {
        assertEq(0, Vec.of(1, 0).angle());
        assertEq(PI / 2, Vec.of(0, 1).angle());
        assertEq(PI, Vec.of(-1, 0).angle());
        assertEq(-PI / 2, Vec.of(0, -1).angle());

        assertEq(-PI / 4, Vec.of(1, -1).angle());
    }

    public void testAngleTo() {
        assertEq(0, Vec.of(42, 48).angleTo(Vec.of(42, 48)));
        assertEq(-PI / 2, Vec.of(1, 1).angleTo(Vec.of(-1, 1)));
        assertEq(PI / 2, Vec.of(-1, 1).angleTo(Vec.of(1, 1)));
        assertEq(-3 * PI / 4, Vec.of(1, 1).angleTo(Vec.of(-1, 0)));
        assertEq(3 * PI / 4, Vec.of(-1, 0).angleTo(Vec.of(1, 1)));
        assertEq(PI, abs(Vec.of(1, 2).angleTo(Vec.of(-1, -2))));
    }




    private static void assertVec(double expectedX, double expectedY, @NotNull Vec actual) {
        assertEq(expectedX, actual.x);
        assertEq(expectedY, actual.y);
    }

    private static void assertEq(double expected, double actual) {
        if (abs(expected - actual) > 1e-9) {
            String e = String.format("%.9f", expected);
            String a = String.format("%.9f", actual);
            throw new ComparisonFailure("expected " + e + ", actual " + a, e, a);
        }
    }
}
