package meteordevelopment.meteorclient.zenith;

import meteordevelopment.meteorclient.mixin.MinecraftAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.gui.components.PlayerFaceExtractor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Each widget is individually draggable, anchorable, configurable and serializable in the HUD editor. */
public final class ZenithHud extends HudElement {
    public enum Kind { Status, Memory, Performance, Radar, Keys, Potions, Target, Compass }
    private static final HudGroup GROUP = new HudGroup("Zenith 2.0");
    private static final Map<Kind, HudElementInfo<ZenithHud>> INFOS = new EnumMap<>(Kind.class);
    private static final Color TEXT = new Color(240, 233, 255), MUTED = new Color(166, 156, 184);
    static {
        for (Kind kind : Kind.values()) INFOS.put(kind, new HudElementInfo<>(GROUP,
            "zenith-" + kind.name().toLowerCase(Locale.ROOT), "Zenith " + kind,
            "Reference-inspired " + kind.name().toLowerCase(Locale.ROOT) + " widget.", () -> new ZenithHud(kind)));
    }
    public static void register(Hud hud) { INFOS.values().forEach(hud::register); }
    public static void defaults(Hud hud) {
        hud.active = true;
        int offset=(int)Math.round(270*initialScale());
        hud.add(INFOS.get(Kind.Status), 18, 18, XAnchor.Left, YAnchor.Top);
        hud.add(INFOS.get(Kind.Compass), 0, 18, XAnchor.Center, YAnchor.Top);
        hud.add(INFOS.get(Kind.Potions), -18, 125, XAnchor.Right, YAnchor.Top);
        hud.add(INFOS.get(Kind.Target), 18, 125, XAnchor.Left, YAnchor.Top);
        hud.add(INFOS.get(Kind.Memory), 18, -18, XAnchor.Left, YAnchor.Bottom);
        hud.add(INFOS.get(Kind.Performance), -18, -18, XAnchor.Right, YAnchor.Bottom);
        hud.add(INFOS.get(Kind.Radar), -offset, -18, XAnchor.Center, YAnchor.Bottom);
        hud.add(INFOS.get(Kind.Keys), offset, -18, XAnchor.Center, YAnchor.Bottom);
    }

    private Kind kind;
    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Double> scale = general.add(new DoubleSetting.Builder().name("scale")
        .description("Widget scale independent of the game GUI.").defaultValue(initialScale()).range(.5, 3).sliderRange(.5, 2).build());
    private final Setting<SettingColor> accent = general.add(new ColorSetting.Builder().name("accent")
        .description("Purple highlights and graph lines.").defaultValue(new SettingColor(183, 117, 255)).build());
    private final Setting<SettingColor> background = general.add(new ColorSetting.Builder().name("background")
        .description("Panel tint and opacity.").defaultValue(new SettingColor(15, 12, 24, 182)).build());
    private final Setting<Boolean> shadow = general.add(new BoolSetting.Builder().name("text-shadow")
        .description("Draw a subtle text shadow.").defaultValue(false).build());
    private final Setting<Integer> radarRange = general.add(new IntSetting.Builder().name("radar-range")
        .description("Radar radius in blocks.").defaultValue(48).range(8, 128).sliderRange(8, 128)
        .visible(() -> kind == Kind.Radar).build());
    private final Setting<Boolean> showDetails = general.add(new BoolSetting.Builder().name("details")
        .description("Show secondary statistics and labels.").defaultValue(true).build());
    private final Setting<Boolean> referenceColors = general.add(new BoolSetting.Builder().name("reference-colors")
        .description("Use the footage's teal graphs/radar, white keys, and pale green health bar.").defaultValue(true).build());
    private final Setting<Boolean> dock = general.add(new BoolSetting.Builder().name("dock-to-hotbar")
        .description("Follow the real hotbar width and GUI scale; dragging in the editor disables docking.")
        .defaultValue(true).visible(()->kind==Kind.Radar||kind==Kind.Keys).build());
    private final Setting<Boolean> heads = general.add(new BoolSetting.Builder().name("player-heads")
        .description("Display actual player faces on the radar.").defaultValue(true).visible(()->kind==Kind.Radar).build());
    private final Map<String, Integer> potionMax = new HashMap<>();
    private final Map<String,KeyAnimation> keyAnimations=new HashMap<>();
    private double hpRatio = 1;
    private static final class KeyAnimation { double amount,pulse=1;boolean down; }

    private ZenithHud(Kind kind) { super(INFOS.get(kind)); this.kind = kind; }
    private static double initialScale() { return Math.min(1,Math.max(.6,meteordevelopment.meteorclient.utils.Utils.getWindowWidth()/1280.0)); }
    private double fontScale(double n) { return Hud.get().hasCustomFont()?Math.sqrt(n*1.35*scale.get()):n*scale.get(); }
    private double s(double n) { return n * scale.get(); }
    private void text(HudRenderer r, String text, double dx, double dy, Color color, double size) {
        r.text(text, x+s(dx), y+s(dy), color, shadow.get(), fontScale(size));
    }
    private void rect(HudRenderer r, double dx, double dy, double w, double h, Color color) {
        r.quad(x+s(dx), y+s(dy), s(w), s(h), color);
    }
    private void line(HudRenderer r, double x1,double y1,double x2,double y2,Color color) {
        double len=Math.hypot(x2-x1,y2-y1);
        if(len<.001)return;
        double nx=-(y2-y1)/len*.6,ny=(x2-x1)/len*.6;
        r.triangle(x+s(x1+nx),y+s(y1+ny),x+s(x2+nx),y+s(y2+ny),x+s(x2-nx),y+s(y2-ny),color);
        r.triangle(x+s(x1+nx),y+s(y1+ny),x+s(x2-nx),y+s(y2-ny),x+s(x1-nx),y+s(y1-ny),color);
    }
    private void panel(HudRenderer r, double w, double h) {
        ZenithDraw.rounded(r,x-s(2),y-s(2),s(w+4),s(h+4),s(8),new Color(0,0,0,24));
        ZenithDraw.rounded(r,x,y,s(w),s(h),s(6),new Color(235,225,255,20));
        ZenithDraw.rounded(r,x+s(1),y+s(1),s(w-2),s(h-2),s(5),background.get());
    }
    private Color dataColor() { return referenceColors.get()?new Color(71,220,180):accent.get(); }
    private void dockToHotbar() {
        if(!dock.get()||(kind!=Kind.Radar&&kind!=Kind.Keys))return;
        UiMotion.Position p=UiMotion.dock(meteordevelopment.meteorclient.utils.Utils.getWindowWidth(),
            meteordevelopment.meteorclient.utils.Utils.getWindowHeight(),mc.getWindow().getGuiScale(),getWidth(),getHeight(),scale.get(),kind==Kind.Radar);
        box.xAnchor=XAnchor.Left;box.yAnchor=YAnchor.Top;box.setPos((int)Math.round(p.x()),(int)Math.round(p.y()));updatePos();
    }
    @Override public void move(int dx,int dy) {if(kind==Kind.Radar||kind==Kind.Keys)dock.set(false);super.move(dx,dy);}
    private void circle(HudRenderer r, double cx,double cy,double radius,double progress,Color color) {
        int steps = Math.max(1,(int)(48*Math.min(1,Math.max(0,progress))));
        for (int i=0;i<steps;i++) {
            double a=-Math.PI/2+i*Math.TAU/48,b=-Math.PI/2+(i+1)*Math.TAU/48;
            line(r,cx+Math.cos(a)*radius,cy+Math.sin(a)*radius,cx+Math.cos(b)*radius,cy+Math.sin(b)*radius,color);
        }
    }

    public static void star(HudRenderer r, double cx,double cy,double radius,double time,Color color) {
        double bob = Math.sin(time*2)*radius*.12;
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4,b=(i+1)*Math.PI/4;
            double ra=i%2==0?radius:radius*.25,rb=i%2==0?radius*.25:radius;
            r.triangle(cx,cy+bob,cx+Math.cos(b)*rb,cy+bob+Math.sin(b)*rb,cx+Math.cos(a)*ra,cy+bob+Math.sin(a)*ra,color);
        }
    }

    @Override public void render(HudRenderer r) {
        switch(kind) {
            case Status -> status(r);
            case Memory -> graph(r,true);
            case Performance -> graph(r,false);
            case Radar -> radar(r);
            case Keys -> keys(r);
            case Potions -> potions(r);
            case Target -> target(r);
            case Compass -> compass(r);
        }
    }

    private void status(HudRenderer r) {
        setSize(s(280),s(52)); panel(r,280,52);
        star(r,x+s(22),y+s(23),s(13),System.nanoTime()/1e9,accent.get());
        text(r,"Zenith",44,7,TEXT,.9); text(r,"2.0",137,11,accent.get(),.55);
        text(r,MinecraftAccessor.meteor$getFps()+" FPS  /  "+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),44,31,MUTED,.5);
    }

    private void graph(HudRenderer r, boolean memory) {
        setSize(s(235),s(96)); panel(r,235,96);
        Zenith data = Zenith.INSTANCE;
        Runtime runtime=Runtime.getRuntime();
        long used=(runtime.totalMemory()-runtime.freeMemory())/1048576,max=runtime.maxMemory()/1048576;
        text(r,memory?"MEMORY":"PERFORMANCE",12,10,MUTED,.45);
        String value=memory?used+" / "+max+" MB":MinecraftAccessor.meteor$getFps()+" FPS";
        double vw=r.textWidth(value,false,fontScale(.55))/scale.get();
        text(r,value,223-vw,8,TEXT,.55);
        FrameHistory history=memory?data.memory:data.frames;
        for (int i=1;i<4;i++) rect(r,12,30+i*12,211,1,new Color(220,210,240,12));
        double maximum=memory?Math.max(1,max):50;
        if(memory) {
            rect(r,12,29,211,3,new Color(50,41,70));
            ZenithDraw.rounded(r,x+s(12),y+s(29),s(211*Math.min(1,used/(double)Math.max(1,max))),s(4),s(2),dataColor());
        }
        for(int i=1;i<history.size();i++) {
            double v1=Math.min(1,history.get(i-1)/maximum),v2=Math.min(1,history.get(i)/maximum);
            double x1=12+(i-1)*211.0/Math.max(1,history.size()-1),x2=12+i*211.0/Math.max(1,history.size()-1);
            Color stroke=!memory&&history.get(i)>25?new Color(238,179,100):dataColor();
            rect(r,x1,74-v1*34,Math.max(.3,x2-x1),Math.max(0,v1*34),new Color(stroke.r,stroke.g,stroke.b,14));
            line(r,x1,74-v1*34,x2,74-v2*34,stroke);
        }
        if(showDetails.get()) text(r,memory?String.format(Locale.ROOT,"HEAP %.0f%%  /  JAVA 25",100.0*used/Math.max(1,max)):
            String.format(Locale.ROOT,"AVG %.0f   1%% LOW %.0f   MS %.1f",data.averageFps,data.lowFps,
                history.size()==0?0:history.get(history.size()-1)),12,81,MUTED,.38);
    }

    private void radar(HudRenderer r) {
        setSize(s(132),s(153));dockToHotbar();panel(r,132,153);
        Color tint=dataColor(),grid=new Color(213,222,229,42);
        int left=Zenith.INSTANCE.cps(false),right=Zenith.INSTANCE.cps(true);
        ZenithDraw.rounded(r,x+s(7),y+s(7),s(55),s(17),s(3),new Color(255,255,255,18));
        ZenithDraw.rounded(r,x+s(68),y+s(7),s(57),s(17),s(3),new Color(tint.r,tint.g,tint.b,58));
        text(r,"L "+left,16,11,MUTED,.42);text(r,"R "+right,77,11,TEXT,.42);
        circle(r,66,88,50,1,grid);circle(r,66,88,25,1,new Color(213,222,229,23));
        line(r,16,88,116,88,new Color(213,222,229,25));line(r,66,38,66,138,new Color(213,222,229,25));
        r.triangle(x+s(66),y+s(88),x+s(83),y+s(41),x+s(49),y+s(41),new Color(tint.r,tint.g,tint.b,14));
        ZenithDraw.disc(r,x+s(66),y+s(88),s(17),new Color(tint.r,tint.g,tint.b,100),new Color(tint.r,tint.g,tint.b,0));
        ZenithDraw.disc(r,x+s(66),y+s(88),s(4.2),new Color(233,255,249),new Color(tint.r,tint.g,tint.b,120));
        if(mc.player==null||mc.level==null)return;
        double yaw=mc.player.getYRot();
        String[] cardinals={"N","E","S","W"};double[][] vectors={{0,-1},{1,0},{0,1},{-1,0}};
        for(int i=0;i<4;i++) {
            double[] projected=UiMotion.radar(vectors[i][0],vectors[i][1],yaw);
            text(r,cardinals[i],63+projected[0]*56,84+projected[1]*56,MUTED,.33);
        }
        for(Player p:mc.level.players()) {
            if(p==mc.player)continue;
            double dx=p.getX()-mc.player.getX(),dz=p.getZ()-mc.player.getZ();
            if(dx*dx+dz*dz>radarRange.get()*radarRange.get())continue;
            double[] projected=UiMotion.radar(dx,dz,yaw);
            double px=66+projected[0]*43/radarRange.get(),py=88+projected[1]*43/radarRange.get();
            if(heads.get()&&p instanceof AbstractClientPlayer player) {
                int hx=(int)(x+s(px-4)),hy=(int)(y+s(py-4)),size=(int)Math.max(5,s(9));
                var skin=player.getSkin();r.post(()->PlayerFaceExtractor.extractRenderState(r.graphics,skin,hx,hy,size));
            } else {ZenithDraw.disc(r,x+s(px),y+s(py),s(3),tint,new Color(tint.r,tint.g,tint.b,0));}
        }
    }

    private void key(HudRenderer r,String label,double dx,double dy,double w,double h,boolean down) {
        KeyAnimation anim=keyAnimations.computeIfAbsent(label,_ -> new KeyAnimation());
        if(down&&!anim.down)anim.pulse=0;
        anim.down=down;anim.pulse=Math.min(1,anim.pulse+Math.min(.1,r.delta)*2.8);
        anim.amount=UiMotion.approach(anim.amount,down?1:0,down?28:15,r.delta);
        double bounce=anim.amount*.6;
        for(int i=3;i>0;i--)ZenithDraw.rounded(r,x+s(dx-i),y+s(dy-i+bounce),s(w+i*2),s(h+i*2),s(6+i),new Color(245,244,255,(int)(anim.amount*(11-i*2))));
        ZenithDraw.rounded(r,x+s(dx),y+s(dy+bounce),s(w),s(h),s(5),ZenithDraw.mix(background.get(),new Color(249,248,252,250),anim.amount));
        if(anim.pulse<1) {
            double inset=(1-anim.pulse)*Math.min(w,h)*.45;
            ZenithDraw.rounded(r,x+s(dx+inset),y+s(dy+inset),s(w-inset*2),s(h-inset*2),s(4),new Color(255,255,255,(int)((1-anim.pulse)*48)));
        }
        double width=r.textWidth(label,false,fontScale(.55))/scale.get();
        Color text=ZenithDraw.mix(TEXT,new Color(25,24,29),anim.amount);
        text(r,label,dx+(w-width)/2,dy+(h>35?7:h/2-5)+bounce,text,.55);
        if(showDetails.get()&&(label.equals("LMB")||label.equals("RMB"))) {
            String cps=Zenith.INSTANCE.cps(label.equals("RMB"))+" CPS";
            double tw=r.textWidth(cps,false,fontScale(.44))/scale.get();text(r,cps,dx+(w-tw)/2,dy+23+bounce,ZenithDraw.mix(MUTED,new Color(58,56,62),anim.amount),.44);
        }
    }
    private void keys(HudRenderer r) {
        setSize(s(132),s(146));dockToHotbar();
        boolean game=mc.player!=null&&mc.gui.screen()==null;
        key(r,"W",45,0,40,35,game&&mc.options.keyUp.isDown());
        key(r,"A",2,39,40,35,game&&mc.options.keyLeft.isDown());
        key(r,"S",45,39,40,35,game&&mc.options.keyDown.isDown());
        key(r,"D",88,39,40,35,game&&mc.options.keyRight.isDown());
        key(r,"LMB",2,78,62,43,game&&(mc.options.keyAttack.isDown()||meteordevelopment.meteorclient.utils.misc.input.Input.isButtonPressed(0)));
        key(r,"RMB",67,78,61,43,game&&(mc.options.keyUse.isDown()||meteordevelopment.meteorclient.utils.misc.input.Input.isButtonPressed(1)));
        ZenithDraw.rounded(r,x+s(2),y+s(126),s(126),s(17),s(6),new Color(248,247,251,235));
        ZenithDraw.rounded(r,x+s(44),y+s(133),s(42),s(2),s(1),new Color(39,37,43));
        if(game&&mc.options.keyJump.isDown())ZenithDraw.rounded(r,x,y+s(124),s(132),s(21),s(7),new Color(255,255,255,42));
    }

    private void potions(HudRenderer r) {
        Collection<MobEffectInstance> effects=mc.player==null?List.of():mc.player.getActiveEffects();
        setSize(s(230),s(Math.max(40,Math.min(6,effects.size())*48)));
        if(effects.isEmpty()) {
            if(isInEditor()) {panel(r,230,40);text(r,"Potion effects appear here",12,14,MUTED,.5);}
            return;
        }
        int row=0;
        Set<String> active=new HashSet<>();
        for(MobEffectInstance effect:effects) {
            if(row==6)break;
            String key=effect.getEffect().value().getDescriptionId(); active.add(key);
            int max=potionMax.merge(key,Math.max(1,effect.getDuration()),Math::max);
            double dy=row++*48;
            circle(r,20,dy+20,15,1,new Color(75,57,90,150));
            circle(r,20,dy+20,15,effect.isInfiniteDuration()?1:effect.getDuration()/(double)max,accent.get());
            text(r,Integer.toString(effect.getAmplifier()+1),16,dy+14,TEXT,.5);
            String name=net.minecraft.network.chat.Component.translatable(key).getString();
            text(r,name,44,dy+6,TEXT,.55);
            int seconds=effect.getDuration()/20;
            text(r,effect.isInfiniteDuration()?"infinite":String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60),44,dy+24,MUTED,.45);
        }
        potionMax.keySet().retainAll(active);
    }

    private void target(HudRenderer r) {
        setSize(s(250),s(168));
        LivingEntity target=Zenith.INSTANCE.target;
        if(target==null&&!isInEditor())return;
        panel(r,250,168);
        text(r,"TARGET",12,10,accent.get(),.4);
        text(r,target==null?"Target preview":target.getName().getString(),12,27,TEXT,.7);
        float health=target==null?16:target.getHealth(),max=target==null?20:target.getMaxHealth();
        double ratio=Math.min(1,Math.max(0,health/Math.max(1,max)));
        hpRatio+=(ratio-hpRatio)*(1-Math.exp(-Math.min(.1,r.delta)*12));
        ZenithDraw.rounded(r,x+s(12),y+s(51),s(226),s(6),s(3),new Color(55,43,70));
        ZenithDraw.rounded(r,x+s(12),y+s(51),s(226*hpRatio),s(6),s(3),referenceColors.get()?new Color(183,220,158):accent.get());
        text(r,String.format(Locale.ROOT,"%.1f / %.1f HP",health,max),12,64,TEXT,.5);
        text(r,"Armor "+(target==null?20:target.getArmorValue()),12,84,MUTED,.45);
        text(r,String.format(Locale.ROOT,"Distance %.1fm",target==null||mc.player==null?0:mc.player.distanceTo(target)),12,102,MUTED,.45);
        if(showDetails.get()) {
            text(r,String.format(Locale.ROOT,"Damage +%.1f",Zenith.INSTANCE.damage),12,122,accent.get(),.45);
            text(r,"Hits +"+Zenith.INSTANCE.hits+"   Combo +"+Zenith.INSTANCE.combo,12,141,accent.get(),.45);
        }
    }

    private void compass(HudRenderer r) {
        setSize(s(360),s(40));
        double yaw=mc.player==null?0:(mc.player.getYRot()+360)%360;
        String[] labels={"S","SW","W","NW","N","NE","E","SE"};
        for(int i=-4;i<=4;i++) {
            double heading=Math.floor(yaw/15)*15+i*15;
            double dx=180+(heading-yaw)*2;
            line(r,dx,18,dx,25,new Color(220,208,242,110));
            String label=((int)heading%45+45)%45==0?labels[((int)Math.round(heading/45)%8+8)%8]:Integer.toString(((int)heading%360+360)%360);
            text(r,label,dx-5,2,MUTED,.4);
        }
        rect(r,179,28,2,7,accent.get());
    }
}
