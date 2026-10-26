package meteordevelopment.meteorclient.zenith;

/** Continuous seeded volume noise: a cloud dome has no longitude seam or texture downloads. */
public final class SkyPattern {
    private SkyPattern() {}
    private static double hash(int x,int y,int z) {
        int n=x*0x1f123bb5^y*0x5f356495^z*0x6c8e9cf5;
        n=(n^(n>>>16))*0x7feb352d;n=(n^(n>>>15))*0x846ca68b;
        return Integer.toUnsignedLong(n^(n>>>16))/4294967295.0;
    }
    private static double smooth(double x){return x*x*(3-2*x);}
    private static double mix(double a,double b,double t){return a+(b-a)*t;}
    public static double noise(double x,double y,double z) {
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y),iz=(int)Math.floor(z);
        double u=smooth(x-ix),v=smooth(y-iy),w=smooth(z-iz);
        return mix(mix(mix(hash(ix,iy,iz),hash(ix+1,iy,iz),u),mix(hash(ix,iy+1,iz),hash(ix+1,iy+1,iz),u),v),
            mix(mix(hash(ix,iy,iz+1),hash(ix+1,iy,iz+1),u),mix(hash(ix,iy+1,iz+1),hash(ix+1,iy+1,iz+1),u),v),w);
    }
    public static double cloud(double x,double y,double z) {
        return noise(x,y,z)*.57+noise(x*2.03+19,y*2.03-7,z*2.03+3)*.29+noise(x*4.1-9,y*4.1+11,z*4.1)*.14;
    }
    public static double fade(double age,double lifetime) {
        return lifetime<=0?0:Math.max(0,Math.min(1,1-age/lifetime));
    }
}
