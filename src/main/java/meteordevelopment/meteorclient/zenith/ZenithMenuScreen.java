package meteordevelopment.meteorclient.zenith;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class ZenithMenuScreen extends TitleScreen {
    private final long opened = System.nanoTime();
    public ZenithMenuScreen() { super(); }

    @Override public Component getTitle() { return Component.literal("Zenith Client 2.0"); }

    @Override protected void init() {
        int w = 204, x = width/2-w/2, y = (int)(height*.43);
        addRenderableWidget(new ZenithButton(x,y,w,25,"Singleplayer", _ -> mc.gui.setScreen(new SelectWorldScreen(this))));
        addRenderableWidget(new ZenithButton(x,y+30,w,25,"Multiplayer", _ -> mc.gui.setScreen(new JoinMultiplayerScreen(this))));
        addRenderableWidget(new ZenithButton(x,y+60,w,25,"Client settings", _ -> Tabs.get().getFirst().openScreen(GuiThemes.get())));
        addRenderableWidget(new ZenithButton(x,y+90,99,22,"Options", _ -> mc.gui.setScreen(new OptionsScreen(this,mc.options,false))));
        addRenderableWidget(new ZenithButton(x+105,y+90,99,22,"Quit game", _ -> mc.stop()));
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        double time=(System.nanoTime()-opened)/1e9;
        background(g,width,height,time);
        floatingStar(g,width/2,(int)(height*.20),Math.max(13,Math.min(29,(int)(height*.065))),time);
        centered(g,"ZENITH",width/2,(int)(height*.31),0xfff0e6ff,2);
        centered(g,"CLIENT 2.0",width/2,(int)(height*.31)+20,0xffac8bcf,.8f);
        g.text(font,"MINECRAFT 26.2 / FABRIC",12,12,0xff867c99,false);
        centered(g,"Built on Meteor / GPL-3.0",width/2,height-18,0xff796c8c,.65f);
        // Render this menu's widgets without invoking TitleScreen's panorama and logo.
        // Keeping the TitleScreen type makes native title checks and addon hooks recognize Zenith.
        for (var child : children()) {
            if (child instanceof Renderable renderable) renderable.extractRenderState(g,mx,my,delta);
        }
    }

    public static void background(GuiGraphicsExtractor g,int width,int height,double time) {
        g.fillGradient(0,0,width,height,0xff120b21,0xff08070f);
        // A softly drifting violet horizon, with no framebuffer blur or shader dependency.
        for(int i=48;i>0;i--) {
            int alpha=(int)((1-i/49.0)*4);
            int half=width*i/70;
            int cy=(int)(height*.55+Math.sin(time*.2)*9);
            g.fill(width/2-half,cy-i*2,width/2+half,cy+i*2,(alpha<<24)|0x8b40d4);
        }
        for(int i=0;i<64;i++) {
            double u=((i*0.61803398875)%1),v=((i*0.41421356237)%1);
            int x=(int)(u*width+Math.sin(time*.12+i)*4),y=(int)(v*height);
            int alpha=(int)(35+30*(.5+.5*Math.sin(time+i)));
            g.fill(x,y,x+1,y+1,(alpha<<24)|0xc5a0ee);
        }
    }

    public static void floatingStar(GuiGraphicsExtractor g,int cx,int cy,int size,double time) {
        cy+=(int)(Math.sin(time*1.6)*5);
        for(int glow=4;glow>=1;glow--) {
            int radius=size+glow*4;
            star(g,cx,cy,radius,((6-glow)<<24)|0xa956f5);
        }
        star(g,cx,cy,size,0xffb47bfa);
        star(g,cx,cy,(int)(size*.65),0xffd1a7ff);
        star(g,cx,cy,(int)(size*.21),0xfff5eaff);
        double a=time*.7;
        star(g,cx+(int)(Math.cos(a)*(size+15)),cy+(int)(Math.sin(a)*(size+9)),3,0xffe0baff);
    }

    private static void star(GuiGraphicsExtractor g,int cx,int cy,int radius,int color) {
        // Four long points and a concave waist; crisp at every GUI scale.
        for(int y=-radius;y<=radius;y++) {
            double distance=Math.abs(y)/(double)Math.max(1,radius);
            int half=Math.max(1,(int)(radius*(1-distance)/(1+distance*5)));
            g.fill(cx-half,cy+y,cx+half,cy+y+1,color);
        }
    }

    public static void centered(GuiGraphicsExtractor g,String text,int cx,int y,int color,float scale) {
        g.pose().pushMatrix();g.pose().translate(cx,y);g.pose().scale(scale);
        g.text(mc.font,text,-mc.font.width(text)/2,0,color,false);
        g.pose().popMatrix();
    }

    public static final class ZenithButton extends Button {
        private double hoverAmount;
        private long previous=System.nanoTime();
        public ZenithButton(int x,int y,int w,int h,String label,OnPress action) {
            super(x,y,w,h,Component.literal(label),action,DEFAULT_NARRATION);
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
            boolean hover=isHoveredOrFocused();
            long now=System.nanoTime();
            hoverAmount=UiMotion.approach(hoverAmount,hover?1:0,14,(now-previous)/1e9);previous=now;
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            int tint=0xc0000000|((int)(29+41*hoverAmount)<<16)|((int)(20+20*hoverAmount)<<8)|(int)(45+50*hoverAmount);
            rounded(g,x-1,y-1,w+2,h+2,6,((int)(65+90*hoverAmount)<<24)|0x9864ce);
            rounded(g,x,y,w,h,5,tint);
            String label=getMessage().getString();
            g.text(mc.font,label,x+(w-mc.font.width(label))/2,y+(h-8)/2,hover?0xffffffff:0xffd5c6e8,false);
        }
        private static void rounded(GuiGraphicsExtractor g,int x,int y,int w,int h,int radius,int color) {
            for(int row=0;row<h;row++) {
                double edge=row<radius?radius-row-.5:row>=h-radius?row-(h-radius)+.5:0;
                int inset=edge==0?0:(int)Math.ceil(radius-Math.sqrt(radius*radius-edge*edge));
                g.fill(x+inset,y+row,x+w-inset,y+row+1,color);
            }
        }
    }
}
