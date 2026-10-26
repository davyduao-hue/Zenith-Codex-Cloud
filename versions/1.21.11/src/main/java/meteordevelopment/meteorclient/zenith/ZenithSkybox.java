package meteordevelopment.meteorclient.zenith;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.Random;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Camera-centered far-depth sky: bounded fragment cloud detail with optional celestial geometry. */
public final class ZenithSkybox {
    public enum Style { ReferenceDusk, ReferenceStorm, Nebula, Aurora, Cloudscape }
    private final Renderer3D renderer=new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH,MeteorRenderPipelines.ZENITH_SKY);
    private static final double RADIUS=160;
    private static final double[][] STARS=new double[768][5];
    static {
        Random random=new Random(0x2e17172);
        for(double[] star:STARS) {
            double angle=random.nextDouble()*Math.TAU,height=.025+random.nextDouble()*.96,horizontal=Math.sqrt(1-height*height);
            star[0]=Math.cos(angle)*horizontal;star[1]=height;star[2]=Math.sin(angle)*horizontal;
            star[3]=.13+random.nextDouble()*.23;star[4]=random.nextDouble()*Math.TAU;
        }
    }
    private double[][] directions;
    private Color[] colors;
    private int columns,rows,lastMood=-1,lastStyle=-1;
    private double lastTime=-100,lastDensity,lastBrightness;
    private final Color color=new Color(),edge=new Color(),other=new Color();
    private final Color fourth=new Color();
    private final double[] pointA=new double[3],pointB=new double[3],pointC=new double[3],pointD=new double[3];
    private double cx,cy,cz;
    private int frames,meteorFrames,vertices,starCount;
    private SkyUniforms uniforms;
    private final CachedSkyMesh domeCache=new CachedSkyMesh(),panoramaCache=new CachedSkyMesh();
    private final meteordevelopment.meteorclient.renderer.MeshBuilder cachedBuilder=new meteordevelopment.meteorclient.renderer.MeshBuilder(MeteorRenderPipelines.ZENITH_SKY);
    private int panoramaColumns,panoramaRows;
    public int meshBuilds(){return domeCache.builds()+panoramaCache.builds();}
    public record Statistics(int frames,int vertices,int stars,int meteorFrames) {}
    public Statistics statistics(){return new Statistics(frames,vertices,starCount,meteorFrames);}
    public void clear(){
        frames=meteorFrames=vertices=starCount=0;lastTime=-100;
        domeCache.close();panoramaCache.close();
        if (uniforms != null) { uniforms.close(); uniforms = null; }
    }

    public void render(PoseStack matrices,VisualBudget budget,double time,Style style,int mood,double brightness,double density,boolean stars,boolean aurora,boolean meteors,boolean celestial) {
        boolean was3D=Utils.rendering3D;
        Utils.rendering3D=false;
        RenderSystem.getModelViewStack().pushMatrix().identity();
        PoseStack rotation=new PoseStack();
        rotation.last().pose().set(SkyTransform.rotation(mc.gameRenderer.getMainCamera().yRot(),mc.gameRenderer.getMainCamera().xRot()));
        try {renderDome(rotation,budget,time,style,mood,brightness,density,stars,aurora,meteors,celestial);}
        finally {RenderSystem.getModelViewStack().popMatrix();Utils.rendering3D=was3D;}
    }
    private void renderDome(PoseStack matrices,VisualBudget budget,double time,Style style,int mood,double brightness,double density,boolean stars,boolean aurora,boolean meteors,boolean celestial) {
        cx=cy=cz=0;
        boolean topologyChanged=columns!=budget.skyColumns()||rows!=budget.skyRows();
        if(topologyChanged) {
            columns=budget.skyColumns();rows=budget.skyRows();directions=new double[(columns+1)*(rows+1)][3];colors=new Color[directions.length];
            for(int row=0;row<=rows;row++)for(int col=0;col<=columns;col++) {
                double angle=col*Math.TAU/columns,elevation=-.055+(Math.PI/2+.055)*row/rows;
                int index=row*(columns+1)+col;
                directions[index]=direction(angle,elevation);colors[index]=new Color();
            }
            lastTime=-100;
        }
        // Reference skies animate entirely in the fragment shader. Other styles update
        // their vertex tint at five Hz, while camera motion only changes the draw transform.
        boolean reference=style==Style.ReferenceDusk||style==Style.ReferenceStorm;
        boolean recolor=topologyChanged||!domeCache.ready()||style.ordinal()!=lastStyle
            ||(!reference&&(Math.abs(time-lastTime)>.2||mood!=lastMood||density!=lastDensity));
        if(recolor) {
            for(int i=0;i<directions.length;i++) {
                if (style == Style.ReferenceDusk || style == Style.ReferenceStorm) colors[i].set(255,255,255,255);
                else shade(colors[i],directions[i],time,style,mood,1,density);
            }
            lastTime=time;lastMood=mood;lastStyle=style.ordinal();lastDensity=density;lastBrightness=brightness;
        }
        if(recolor) {
            var mesh=cachedBuilder;mesh.begin();
            mesh.ensureCapacity(directions.length,columns*rows*6);
            for(int i=0;i<directions.length;i++) {
                double[] d=directions[i];mesh.vec3(d[0]*RADIUS,d[1]*RADIUS,d[2]*RADIUS).color(colors[i]).next();
            }
            for(int row=0;row<rows;row++)for(int col=0;col<columns;col++) {
                int a=row*(columns+1)+col;mesh.quad(a,a+1,a+columns+2,a+columns+1);
            }
            mesh.end();domeCache.upload(mesh,topologyChanged);
        }
        if (uniforms == null) uniforms = new SkyUniforms();
        domeCache.bind(MeshRenderer.begin()).attachments(mc.getMainRenderTarget()).pipeline(MeteorRenderPipelines.ZENITH_ATMOSPHERE)
            .transform(matrices).uniform("Projection",RenderSystem.getProjectionMatrixBuffer())
            .uniform("Atmosphere",uniforms.write(time,style.ordinal(),brightness,density,
                budget.skyColumns() < 64 ? 3 : budget.skyColumns() < 120 ? 4 : 5)).end();
        Renderer3D r=renderer;r.begin();
        if(aurora)aurora(r,time,style==Style.Aurora?1.0:.48,budget,mood);
        // The storm recording has no visible stars; the optional other styles retain the starfield.
        stars = stars && style != Style.ReferenceStorm;
        starCount=stars?budget.stars():0;
        if(stars)for(int i=0;i<starCount;i++) {
            double[] s=STARS[i];double pulse=.72+.28*Math.sin(time*.8+s[4]);
            color.set(219+(i%3)*12,214+(i%4)*9,255,(int)(245*pulse));
            disc(r,s[0]*151,s[1]*151,s[2]*151,s[3]*(i%29==0?2.8:1.4),color,4);
            if(i%29==0){color.a=(int)(55*pulse);disc(r,s[0]*151,s[1]*151,s[2]*151,s[3]*5,color,8);}
        }
        if(celestial) {
            double[] n=direction(1.13,.58);double x=n[0]*145,y=n[1]*145,z=n[2]*145;
            color.set(mood==0?255:157,mood==1?207:119,mood==0?139:255,35);disc(r,x,y,z,21,color,48);
            color.set(223,208,255,220);disc(r,x,y,z,7.2,color,48);
            // The thin tilted halo is detached from the planet's soft core.
            for(int i=0;i<64;i++) {
                double a=i*Math.TAU/64,b=(i+1)*Math.TAU/64;
                color.set(195,151,255,145);
                segment(r,x+Math.cos(a)*12,y+Math.sin(a)*3.7,z-Math.cos(a)*3,
                    x+Math.cos(b)*12,y+Math.sin(b)*3.7,z-Math.cos(b)*3,.08,color,color);
            }
        }
        if(meteors)for(int i=0;i<4;i++) {
            double phase=time*.065+i*.25;phase-=Math.floor(phase);if(phase>.18)continue;
            double t=phase/.18,a=.35+i*1.7+t*.24,e=.95-i*.12-t*.22;
            double[] head=direction(a,e),tail=direction(a-.12,e+.1);
            color.set(235,220,255,(int)(220*Math.sin(t*Math.PI)));edge.set(165,109,255,0);
            segment(r,head[0]*149,head[1]*149,head[2]*149,tail[0]*149,tail[1]*149,tail[2]*149,.13,color,edge);
            disc(r,head[0]*149,head[1]*149,head[2]*149,.4,color,4);meteorFrames++;
        }
        r.lines.end();
        MeshRenderer.begin().attachments(mc.getMainRenderTarget()).pipeline(MeteorRenderPipelines.ZENITH_SKY)
            .mesh(r.triangles,matrices).uniform("Projection",RenderSystem.getProjectionMatrixBuffer()).end();
        frames++;vertices=directions.length;
    }


    /** Packs supply a normal ResourceManager texture; no native files or pack-specific code is executed. */
    public boolean renderPanorama(VisualBudget budget,String location,double heading) {
        var id=net.minecraft.resources.Identifier.tryParse(location);
        if(id==null||mc.getResourceManager().getResource(id).isEmpty())return false;
        var texture=mc.getTextureManager().getTexture(id);
        boolean was3D=Utils.rendering3D;Utils.rendering3D=false;
        RenderSystem.getModelViewStack().pushMatrix().identity();
        try {
            int cols=budget.skyColumns(),rows=budget.skyRows();
            if(!panoramaCache.ready()||panoramaColumns!=cols||panoramaRows!=rows) {
                var mesh=cachedBuilder;mesh.begin();mesh.ensureCapacity((cols+1)*(rows+1),cols*rows*6);
                for(int row=0;row<=rows;row++)for(int col=0;col<=cols;col++) {
                    direction(pointA,col*Math.TAU/cols,-Math.PI/2+Math.PI*row/rows);
                    mesh.vec3(pointA[0]*RADIUS,pointA[1]*RADIUS,pointA[2]*RADIUS).color(Color.WHITE).next();
                }
                for(int row=0;row<rows;row++)for(int col=0;col<cols;col++) {
                    int a=row*(cols+1)+col;mesh.quad(a,a+1,a+cols+2,a+cols+1);
                }
                mesh.end();panoramaCache.upload(mesh,true);panoramaColumns=cols;panoramaRows=rows;
            }
            var rotation=SkyTransform.rotation(mc.gameRenderer.getMainCamera().yRot(),mc.gameRenderer.getMainCamera().xRot()).rotateY((float)Math.toRadians(heading));
            panoramaCache.bind(MeshRenderer.begin()).attachments(mc.getMainRenderTarget()).pipeline(MeteorRenderPipelines.ZENITH_PANORAMA)
                .transform(rotation).uniform("Projection",RenderSystem.getProjectionMatrixBuffer())
                .sampler("u_Texture",texture.getTextureView(),RenderSystem.getSamplerCache().getSampler(
                    com.mojang.blaze3d.textures.AddressMode.REPEAT,com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE,
                    com.mojang.blaze3d.textures.FilterMode.LINEAR,com.mojang.blaze3d.textures.FilterMode.LINEAR,false)).end();
            frames++;vertices=(cols+1)*(rows+1);starCount=0;
            return true;
        } finally {RenderSystem.getModelViewStack().popMatrix();Utils.rendering3D=was3D;}
    }

    private void shade(Color out,double[] d,double time,Style style,int mood,double brightness,double density) {
        double height=Math.max(0,d[1]),cloud=SkyPattern.cloud(d[0]*4+time*.012,d[1]*3.6,d[2]*4-time*.008);
        double ridge=1-Math.abs(SkyPattern.cloud(d[0]*2.8-3,d[1]*3+time*.008,d[2]*2.8+7)*2-1);
        double band=Math.exp(-Math.pow((d[1]-.42-d[0]*.32-d[2]*.18)*3.4,2));
        double nebula=Math.pow(Math.max(0,(cloud-.25)/.65),1.1)*band*density;
        double clouds=Math.pow(Math.max(0,(cloud-.30)/.70),1.3)*density;
        double strength=style==Style.Cloudscape?clouds:nebula;
        double horizon=Math.exp(-height*4.2);
        double red,green,blue;
        if(mood==0){red=21+111*horizon+143*strength;green=10+35*horizon+64*strength;blue=38+43*horizon+87*strength;}
        else if(mood==1){red=34+82*horizon+119*strength;green=44+83*horizon+125*strength;blue=65+88*horizon+132*strength;}
        else if(mood==3){red=9+35*horizon+61*strength;green=22+39*horizon+103*strength;blue=44+62*horizon+142*strength;}
        else {red=14+62*horizon+123*strength;green=7+23*horizon+49*strength;blue=35+78*horizon+164*strength;}
        if(style==Style.Nebula){double veins=Math.pow(Math.max(0,(ridge-.7)/.3),2)*band*35*density;red+=veins;green+=veins*.62;blue+=veins*1.2;}
        out.set(channel(red*brightness),channel(green*brightness),channel(blue*brightness),(int)(255*UiMotion.clamp((d[1]+.055)/.09,0,1)));
    }
    private static int channel(double x){return (int)UiMotion.clamp(x,0,255);}
    private static double[] direction(double angle,double elevation) {
        double[] out=new double[3];direction(out,angle,elevation);return out;
    }
    private static void direction(double[] out,double angle,double elevation) {
        double horizontal=Math.cos(elevation);out[0]=Math.cos(angle)*horizontal;out[1]=Math.sin(elevation);out[2]=Math.sin(angle)*horizontal;
    }
    private void aurora(Renderer3D r,double time,double strength,VisualBudget budget,int mood) {
        int steps=budget.skyColumns();
        for(int band=0;band<3;band++)for(int i=0;i<steps;i++) {
            double a=i*Math.TAU/steps,b=(i+1)*Math.TAU/steps;
            double ea=.27+band*.2+Math.sin(a*2+time*.12+band)*.08+Math.sin(a*9-time*.08)*.025;
            double eb=.27+band*.2+Math.sin(b*2+time*.12+band)*.08+Math.sin(b*9-time*.08)*.025;
            double alphaA=(.18+.82*Math.pow(.5+.5*Math.sin(a*4+time*.16+band),2))*strength;
            double alphaB=(.18+.82*Math.pow(.5+.5*Math.sin(b*4+time*.16+band),2))*strength;
            for(int layer=0;layer<8;layer++) {
                double low=layer/8.0,high=(layer+1)/8.0;
                direction(pointA,a,ea+low*.22);direction(pointB,b,eb+low*.22);
                direction(pointC,b,eb+high*.22);direction(pointD,a,ea+high*.22);
                double[] p=pointA,q=pointB,u=pointC,v=pointD;
                double lo=Math.pow(Math.sin(low*Math.PI),1.3),hi=Math.pow(Math.sin(high*Math.PI),1.3);
                color.set(mood==0?255:band==1?104:168,mood==0?119:band==1?226:117,mood==0?127:255,(int)(76*alphaA*lo));
                edge.set(color);edge.a=(int)(76*alphaB*lo);other.set(color);other.a=(int)(76*alphaB*hi);fourth.set(color);fourth.a=(int)(76*alphaA*hi);
                r.quad(cx+p[0]*153,cy+p[1]*153,cz+p[2]*153,cx+q[0]*153,cy+q[1]*153,cz+q[2]*153,
                    cx+u[0]*153,cy+u[1]*153,cz+u[2]*153,cx+v[0]*153,cy+v[1]*153,cz+v[2]*153,edge,other,fourth,color);
            }
        }
    }
    private void disc(Renderer3D r,double x,double y,double z,double radius,Color center,int steps) {
        double length=Math.sqrt(x*x+z*z),rx=z/length,rz=-x/length,normal=Math.sqrt(x*x+y*y+z*z);
        double ux=y*rz/normal,uy=(z*rx-x*rz)/normal,uz=-y*rx/normal;
        edge.set(center);edge.a=0;
        for(int i=0;i<steps;i++) {
            double a=i*Math.TAU/steps,b=(i+1)*Math.TAU/steps;
            r.triangles.ensureTriCapacity();
            int c=r.triangles.vec3(cx+x,cy+y,cz+z).color(center).next();
            int p=r.triangles.vec3(cx+x+(rx*Math.cos(a)+ux*Math.sin(a))*radius,cy+y+uy*Math.sin(a)*radius,cz+z+(rz*Math.cos(a)+uz*Math.sin(a))*radius).color(edge).next();
            int q=r.triangles.vec3(cx+x+(rx*Math.cos(b)+ux*Math.sin(b))*radius,cy+y+uy*Math.sin(b)*radius,cz+z+(rz*Math.cos(b)+uz*Math.sin(b))*radius).color(edge).next();
            r.triangles.triangle(c,p,q);
        }
    }
    private void segment(Renderer3D r,double ax,double ay,double az,double bx,double by,double bz,double width,Color start,Color end) {
        double dx=bx-ax,dy=by-ay,dz=bz-az,nx=dy*az-dz*ay,ny=dz*ax-dx*az,nz=dx*ay-dy*ax;
        double length=Math.sqrt(nx*nx+ny*ny+nz*nz);if(length<.00001)return;nx*=width/length;ny*=width/length;nz*=width/length;
        r.quad(cx+ax-nx,cy+ay-ny,cz+az-nz,cx+bx-nx,cy+by-ny,cz+bz-nz,cx+bx+nx,cy+by+ny,cz+bz+nz,cx+ax+nx,cy+ay+ny,cz+az+nz,end,end,start,start);
    }
}
