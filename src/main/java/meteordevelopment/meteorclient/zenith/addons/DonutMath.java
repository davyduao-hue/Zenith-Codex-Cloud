package meteordevelopment.meteorclient.zenith.addons;
/** Coordinate math shared by the original Donut tools. */
public final class DonutMath {
    private DonutMath() {}
    public static int regionStart(int block) { return Math.floorDiv(block,512)*512; }
    public static boolean withinOrigin(double x,double z,double radius) { return x*x+z*z<=radius*radius; }
}
