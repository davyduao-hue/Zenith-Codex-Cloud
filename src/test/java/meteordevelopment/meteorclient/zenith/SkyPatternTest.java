package meteordevelopment.meteorclient.zenith;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkyPatternTest {
    @Test void noiseIsDeterministicAndBoundedAcrossNegativeCoordinates() {
        for(int i=-60;i<=60;i++) {
            double x=i*.37,y=i*.19-3,z=i*.61;
            double value=SkyPattern.cloud(x,y,z);
            assertEquals(value,SkyPattern.cloud(x,y,z));assertTrue(value>=0&&value<=1);
        }
    }
    @Test void cloudsAreContinuousAcrossLatticeBoundaries() {
        for(int i=-8;i<8;i++)assertEquals(SkyPattern.noise(i-1e-7,.37,-2.8),SkyPattern.noise(i+1e-7,.37,-2.8),1e-5);
    }
    @Test void longitudeSeamSamplesTheSameVolumePoint() {
        double y=.6,h=Math.sqrt(1-y*y);
        assertEquals(SkyPattern.cloud(h,y,0),SkyPattern.cloud(Math.cos(Math.TAU)*h,y,Math.sin(Math.TAU)*h),1e-12);
    }
    @Test void cloudsDriftGraduallyInsteadOfRegenerating() {
        for(int i=0;i<40;i++)assertTrue(Math.abs(SkyPattern.cloud(i*.17,.5,3)-SkyPattern.cloud(i*.17+.0024,.5,2.9984))<.015);
    }
    @Test void lifetimesClampAtBothEnds() {
        assertEquals(1,SkyPattern.fade(-3,36));assertEquals(.5,SkyPattern.fade(18,36));
        assertEquals(0,SkyPattern.fade(37,36));assertEquals(0,SkyPattern.fade(0,0));
    }
    @Test void qualityBudgetsBoundStorageAndSkyGeometry() {
        var low=VisualBudget.level(0);var balanced=VisualBudget.level(1);var high=VisualBudget.level(2);
        assertEquals(256,low.particles());assertEquals(768,balanced.particles());assertEquals(1536,high.particles());
        assertTrue(low.skyVertices()<balanced.skyVertices());assertTrue(balanced.skyVertices()<high.skyVertices());
        assertTrue(high.skyVertices()<5000);assertTrue(high.stars()<=768);assertTrue(high.bursts()<=48);
        assertSame(low,VisualBudget.level(-1));assertSame(high,VisualBudget.level(9));
    }
}
