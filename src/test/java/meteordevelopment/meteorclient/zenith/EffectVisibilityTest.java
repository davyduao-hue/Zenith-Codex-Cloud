package meteordevelopment.meteorclient.zenith;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EffectVisibilityTest {
    private final Matrix4f projection=new Matrix4f().perspective((float)Math.PI/2,1,.05f,128);
    private EffectVisibility view(Matrix4f model,double x,double y,double z) {
        EffectVisibility result=new EffectVisibility();
        result.update(projection,model,new Matrix4f(),x,y,z);return result;
    }
    @Test void rejectsBehindCameraButRetainsFrontEffects() {
        var v=view(new Matrix4f(),0,0,0);
        assertTrue(v.visible(0,0,-20,1));assertFalse(v.visible(0,0,20,1));
    }
    @Test void keepsGlowsCrossingScreenEdgeEvenWhenCenterIsOutside() {
        var v=view(new Matrix4f(),0,0,0);
        assertFalse(v.visible(12,0,-10,.25));assertTrue(v.visible(12,0,-10,3));
    }
    @Test void effectContainingCameraIsNotCulled() {
        assertTrue(view(new Matrix4f(),0,0,0).visible(0,0,0,5));
    }
    @Test void turningCameraReversesVisibility() {
        var v=view(new Matrix4f().rotateY((float)Math.PI),0,0,0);
        assertTrue(v.visible(0,0,20,1));assertFalse(v.visible(0,0,-20,1));
    }
    @Test void largeWorldCoordinatesRetainSmallRelativeOffsets() {
        var v=view(new Matrix4f(),29_000_000.125,80,29_000_000.125);
        assertTrue(v.visible(29_000_000.125,80,28_999_980.125,1));
        assertFalse(v.visible(29_000_020.125,80,28_999_990.125,.25));
    }
    @Test void effectTransformMatchesRenderedCameraTilt() {
        var v=new EffectVisibility();
        v.update(projection,new Matrix4f(),new Matrix4f().rotateY((float)Math.PI),0,0,0);
        assertTrue(v.visible(0,0,20,1));assertFalse(v.visible(0,0,-20,1));
    }
}
