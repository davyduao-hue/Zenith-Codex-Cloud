package meteordevelopment.meteorclient.zenith;
import org.junit.jupiter.api.Test;
import org.joml.Vector4f;
import static org.junit.jupiter.api.Assertions.*;
class SkyTransformTest {
    @Test void skyTransformHasNoTranslationAtEveryPitchAndYaw() {
        for(int yaw=-360;yaw<=360;yaw+=30)for(int pitch=-90;pitch<=90;pitch+=15) {
            var m=SkyTransform.rotation(yaw,pitch);
            assertEquals(0,m.m30());assertEquals(0,m.m31());assertEquals(0,m.m32());
            assertEquals(1,m.determinant(),1e-5);
        }
    }
    @Test void horizontalAndVerticalLookingRotateSkyWithoutChangingRadius() {
        var ahead=new Vector4f(0,0,160,1).mul(SkyTransform.rotation(0,0));
        assertEquals(-160,ahead.z,1e-4);
        var up=new Vector4f(0,160,0,1).mul(SkyTransform.rotation(0,-90));
        assertEquals(-160,up.z,1e-4);
    }
}
