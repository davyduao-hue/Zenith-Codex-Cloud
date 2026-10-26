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
        if(w<=0||h<=0||c.a==0)return;
        radius=Math.max(.5,Math.min(radius,Math.min(w,h)/2));
        var mesh=meteordevelopment.meteorclient.renderer.Renderer2D.COLOR.triangles;
        Color transparent=new Color(c.r,c.g,c.b,0);
        int steps=Math.max(6,Math.min(24,(int)Math.ceil(radius*.8))),count=4*(steps+1);
        mesh.ensureCapacity(1+count*2,count*9);
        int center=mesh.vec2(x+w/2,y+h/2).color(c).next();
        int first=-1,previous=-1;
        for(int corner=0;corner<4;corner++)for(int i=0;i<=steps;i++) {
            double angle=(-Math.PI/2+corner*Math.PI/2)+i*Math.PI/(2*steps);
            double cx=x+(corner==0||corner==1?w-radius:radius);
            double cy=y+(corner==0||corner==3?radius:h-radius);
            double nx=Math.cos(angle),ny=Math.sin(angle);
            int inner=mesh.vec2(cx+nx*radius,cy+ny*radius).color(c).next();
            int outer=mesh.vec2(cx+nx*(radius+1),cy+ny*(radius+1)).color(transparent).next();
            if(first<0)first=inner;
            if(previous>=0){mesh.triangle(center,inner,previous);mesh.quad(previous,inner,outer,previous+1);}
            previous=inner;
        }
        mesh.triangle(center,first,previous);mesh.quad(previous,first,first+1,previous+1);
    }
    public static void line(HudRenderer r,double x1,double y1,double x2,double y2,double width,Color c) {
        double length=Math.hypot(x2-x1,y2-y1);if(length<.001)return;
        double nx=-(y2-y1)/length*width*.5,ny=(x2-x1)/length*width*.5;
        var mesh=meteordevelopment.meteorclient.renderer.Renderer2D.COLOR.triangles;
        Color transparent=new Color(c.r,c.g,c.b,0);
        double ex=-(y2-y1)/length,ey=(x2-x1)/length;
        mesh.ensureCapacity(8,18);
        int a=mesh.vec2(x1+nx+ex,y1+ny+ey).color(transparent).next();
        int b=mesh.vec2(x2+nx+ex,y2+ny+ey).color(transparent).next();
        int d=mesh.vec2(x1+nx,y1+ny).color(c).next();
        int e=mesh.vec2(x2+nx,y2+ny).color(c).next();
        int f=mesh.vec2(x1-nx,y1-ny).color(c).next();
        int g=mesh.vec2(x2-nx,y2-ny).color(c).next();
        int j=mesh.vec2(x1-nx-ex,y1-ny-ey).color(transparent).next();
        int k=mesh.vec2(x2-nx-ex,y2-ny-ey).color(transparent).next();
        mesh.quad(a,b,e,d);mesh.quad(d,e,g,f);mesh.quad(f,g,k,j);
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
