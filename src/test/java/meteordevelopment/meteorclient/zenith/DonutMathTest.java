package meteordevelopment.meteorclient.zenith;
import meteordevelopment.meteorclient.zenith.addons.DonutMath;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DonutMathTest {
    @Test void regionBoundariesUseFloorForNegativeCoordinates(){assertEquals(-512,DonutMath.regionStart(-1));assertEquals(-512,DonutMath.regionStart(-512));assertEquals(-1024,DonutMath.regionStart(-513));assertEquals(0,DonutMath.regionStart(511));assertEquals(512,DonutMath.regionStart(512));}
    @Test void originTargetIsCircularAndIncludesBoundary(){assertTrue(DonutMath.withinOrigin(3,4,5));assertTrue(DonutMath.withinOrigin(-3,-4,5));assertFalse(DonutMath.withinOrigin(4,4,5));assertTrue(DonutMath.withinOrigin(0,0,0));}
}
