package meteordevelopment.meteorclient.zenith;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Camera-relative sphere tests use the same matrices as the effects draw. */
public final class EffectVisibility {
    private final Matrix4f clip=new Matrix4f();
    private final FrustumIntersection frustum=new FrustumIntersection();
    private double cameraX,cameraY,cameraZ;

    public void update(Matrix4fc projection,Matrix4fc model,Matrix4fc effects,double x,double y,double z) {
        clip.set(projection).mul(model).mul(effects);
        frustum.set(clip);
        cameraX=x;cameraY=y;cameraZ=z;
    }

    public boolean visible(double x,double y,double z,double radius) {
        // Subtract in double precision before converting large world coordinates to floats.
        return frustum.testSphere((float)(x-cameraX),(float)(y-cameraY),(float)(z-cameraZ),(float)radius);
    }
}
