package meteordevelopment.meteorclient.zenith;

/** Frame-rate-independent animation and shared camera-relative HUD geometry. */
public final class UiMotion {
    private UiMotion() {}
    public static double approach(double current,double target,double speed,double dt) {
        return current+(target-current)*(1-Math.exp(-speed*Math.max(0,Math.min(.1,dt))));
    }
    public static double clamp(double value,double min,double max) { return Math.max(min,Math.min(max,value)); }
    /** Shortest signed camera turn, including the -180/180 boundary. */
    public static double angleDelta(double previous,double current) {
        return ((current-previous+180)%360+360)%360-180;
    }
    /** Camera motion, not a map: yaw/pitch velocity deflects the dot and stillness recenters it. */
    public static final class LookMotion {
        private boolean initialized;
        private double previousYaw,previousPitch,x,y;
        public void update(double yaw,double pitch,double dt,double sensitivity) {
            if(!initialized||dt<=0||dt>.25) {
                previousYaw=yaw;previousPitch=pitch;initialized=true;
                x=y=0;return;
            }
            double vx=angleDelta(previousYaw,yaw)/(Math.max(.001,dt)*540)*sensitivity;
            double vy=(pitch-previousPitch)/(Math.max(.001,dt)*540)*sensitivity;
            previousYaw=yaw;previousPitch=pitch;
            double length=Math.hypot(vx,vy);
            if(length>1){vx/=length;vy/=length;}
            x=approach(x,vx,19,dt);y=approach(y,vy,19,dt);
        }
        public double x() {return x;}
        public double y() {return y;}
        public void reset() {initialized=false;x=y=0;}
    }
    /** Four bounded press rings; event latching keeps taps shorter than a frame visible. */
    public static final class PressAnimation {
        public static final int RIPPLE_COUNT=4;
        private final double[] ripples={1,1,1,1};
        private int nextRipple;
        private double amount,minimumHold;
        private boolean wasDown;
        public void update(boolean down,boolean pressEvent,double dt) {
            dt=clamp(dt,0,.25);
            for(int i=0;i<ripples.length;i++)ripples[i]=Math.min(1,ripples[i]+dt/.44);
            if(pressEvent||(down&&!wasDown)) {
                ripples[nextRipple]=0;
                nextRipple=(nextRipple+1)%ripples.length;
                minimumHold=.09;
            }
            amount=approach(amount,down||minimumHold>0?1:0,down||minimumHold>0?32:12,dt);
            minimumHold=Math.max(0,minimumHold-dt);
            wasDown=down;
        }
        public double amount() {return amount;}
        public double rippleProgress(int index) {return ripples[index];}
    }
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
            // On small windows, stack above the graphs instead of pushing both widgets
            // into the middle. At larger sizes the pair still sits beside the hotbar.
            double leftSlot=graphEdge+gap, rightSlot=width-graphEdge-gap-widgetWidth;
            boolean room=leftSlot+widgetWidth+gap<=rightSlot;
            x=room?(left?Math.max(x,leftSlot):Math.min(x,rightSlot)):(left?18:width-18-widgetWidth);
            y=room?height-24.0*guiScale-widgetHeight-gap:
                height-18-96*scale-gap-widgetHeight;
        }
        return new Position(clamp(x,8,Math.max(8,width-widgetWidth-8)),clamp(y,8,Math.max(8,height-widgetHeight-8)),raised);
    }
}
