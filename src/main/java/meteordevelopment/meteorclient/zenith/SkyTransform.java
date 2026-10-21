package meteordevelopment.meteorclient.zenith;

import org.joml.Matrix4f;

/** Sky orientation deliberately has no position, eye height, view bob or hurt transform. */
public final class SkyTransform {
    private SkyTransform() {}
    public static Matrix4f rotation(double yaw,double pitch) {
        return new Matrix4f().rotationX((float)Math.toRadians(pitch))
            .rotateY((float)Math.toRadians(yaw+180));
    }
}
