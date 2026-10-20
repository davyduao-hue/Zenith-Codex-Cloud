package meteordevelopment.meteorclient.zenith;

import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Small batched primitives shared by the custom GUI and reference-style HUD. */
public final class ZenithDraw {
    private ZenithDraw() {}
    public static Color rgba(int rgb,int alpha) { return new Color(rgb>>16&255,rgb>>8&255,rgb&255,Math.max(0,Math.min(255,alpha))); }
    public static Color mix(Color a,Color b,double t) {
        t=UiMotion.clamp(t,0,1);
        return new Color((int)(a.r+(b.r-a.r)*t),(int)(a.g+(b.g-a.g)*t),(int)(a.b+(b.b-a.b)*t),(int)(a.a+(b.a-a.a)*t));
    }
    public static void rounded(HudRenderer r,double x,double y,double w,double h,double radius,Color c) {
        radius=Math.min(radius,Math.min(w,h)/2);
        if(radius<.5){r.quad(x,y,w,h,c);return;}
        r.quad(x,y+radius,w,Math.max(0,h-radius*2),c);
        int steps=Math.max(1,(int)Math.ceil(radius));
        double row=radius/steps;
        for(int i=0;i<steps;i++) {
            double d=radius-(i+.5)*row;
            double inset=radius-Math.sqrt(Math.max(0,radius*radius-d*d));
            r.quad(x+inset,y+i*row,w-inset*2,row,c);
            r.quad(x+inset,y+h-(i+1)*row,w-inset*2,row,c);
        }
    }
    public static void line(HudRenderer r,double x1,double y1,double x2,double y2,double width,Color c) {
        double length=Math.hypot(x2-x1,y2-y1);if(length<.001)return;
        double nx=-(y2-y1)/length*width*.5,ny=(x2-x1)/length*width*.5;
        r.triangle(x1+nx,y1+ny,x2+nx,y2+ny,x2-nx,y2-ny,c);
        r.triangle(x1+nx,y1+ny,x2-nx,y2-ny,x1-nx,y1-ny,c);
    }
    public static void disc(HudRenderer r,double x,double y,double radius,Color center,Color edge) {
        var mesh=meteordevelopment.meteorclient.renderer.Renderer2D.COLOR.triangles;
        for(int i=0;i<40;i++) {
            double a=i*Math.TAU/40,b=(i+1)*Math.TAU/40;
            mesh.ensureTriCapacity();
            int v0=mesh.vec2(x,y).color(center).next();
            int v1=mesh.vec2(x+Math.cos(b)*radius,y+Math.sin(b)*radius).color(edge).next();
            int v2=mesh.vec2(x+Math.cos(a)*radius,y+Math.sin(a)*radius).color(edge).next();
            mesh.triangle(v0,v1,v2);
        }
    }
}
