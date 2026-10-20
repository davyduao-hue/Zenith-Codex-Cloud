package meteordevelopment.meteorclient.zenith;

/** Frame-rate-independent animation and shared camera-relative HUD geometry. */
public final class UiMotion {
    private UiMotion() {}
    public static double approach(double current,double target,double speed,double dt) {
        return current+(target-current)*(1-Math.exp(-speed*Math.max(0,Math.min(.1,dt))));
    }
    public static double clamp(double value,double min,double max) { return Math.max(min,Math.min(max,value)); }
    public static double[] radar(double dx,double dz,double yawDegrees) {
        double yaw=Math.toRadians(yawDegrees);
        return new double[]{-dx*Math.cos(yaw)-dz*Math.sin(yaw),dx*Math.sin(yaw)-dz*Math.cos(yaw)};
    }
    public record Position(double x,double y,boolean raised) {}
    public static Position dock(int width,int height,int guiScale,double widgetWidth,double widgetHeight,double scale,boolean left) {
        double gap=12*scale,halfHotbar=91.0*guiScale;
        double x=left?width/2.0-halfHotbar-widgetWidth-gap:width/2.0+halfHotbar+gap;
        double y=height-widgetHeight-8*scale;
        double graphEdge=18+235*scale;
        boolean raised=left?x<graphEdge+gap:x+widgetWidth>width-graphEdge-gap;
        if(raised) {
            x=left?Math.max(x,graphEdge+gap):Math.min(x,width-graphEdge-gap-widgetWidth);
            y=height-24.0*guiScale-widgetHeight-gap;
        }
        return new Position(clamp(x,8,Math.max(8,width-widgetWidth-8)),clamp(y,8,Math.max(8,height-widgetHeight-8)),raised);
    }
}
