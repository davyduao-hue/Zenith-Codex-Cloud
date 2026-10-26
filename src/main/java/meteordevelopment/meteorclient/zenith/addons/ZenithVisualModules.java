package meteordevelopment.meteorclient.zenith.addons;

import com.mojang.math.Axis;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.meteor.ActiveModulesChangedEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.zenith.*;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Optional local visuals. One shared overlay pass, bounded histories, no packets or entity scans. */
public final class ZenithVisualModules {
    private final List<Visual> modules = List.of(new JumpRings(), new OrbitCrown(), new TargetFocus(),
        new DamageNumbers(), new ViewmodelMotion(), new CrosshairPulse(), new LowHealthGlow(),
        new ArmorWarning(), new ModuleToasts(), new WaypointBeacon());
    private HudRenderer overlay;
    private final EffectVisibility visibility = new EffectVisibility();
    private static final ZenithVisualModules INSTANCE = new ZenithVisualModules();
    public static List<Visual> all() { return INSTANCE.modules; }
    public static void install() {
        for (Visual module : INSTANCE.modules) Modules.get().add(module);
        MeteorClient.EVENT_BUS.subscribe(INSTANCE);
    }
    private static double now() { return System.nanoTime() / 1e9; }
    private static boolean world() { return mc.player != null && mc.level != null; }
    @EventHandler private void left(GameLeftEvent event) { for (Visual module : modules) module.reset(); }
    @EventHandler private void render2D(Render2DEvent event) {
        if (!world() || mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden) return;
        double time = now();
        boolean started = false;
        for (Visual module : modules) if (module.isActive() && module.hasOverlay(time)) {
            if (!started) {
                if (overlay == null) overlay = HudRenderer.independent();
                overlay.begin(event.graphics); started = true;
            }
            module.overlay(overlay, event, time);
        }
        if (started) overlay.end();
    }
    @EventHandler private void render3D(Render3DEvent event) {
        if (!world()) return;
        boolean any = false;
        for (Visual module : modules) if (module.isActive() && module.worldVisual()) { any = true; break; }
        if (!any) return;
        Vec3 camera = mc.gameRenderer.mainCamera().position();
        visibility.update(meteordevelopment.meteorclient.utils.render.RenderUtils.projection,
            com.mojang.blaze3d.systems.RenderSystem.getModelViewStack(), event.matrices.last().pose(), camera.x, camera.y, camera.z);
        for (Visual module : modules) if (module.isActive() && module.worldVisual()) module.render(event, now(), visibility);
    }
    public abstract static class Visual extends Module {
        protected final SettingGroup general = settings.getDefaultGroup();
        protected final Setting<SettingColor> accent = general.add(new ColorSetting.Builder().name("accent")
            .description("Animation accent color.").defaultValue(new SettingColor(183,117,255,220)).build());
        protected final Color tint = new Color();
        private int submissions;
        Visual(String name, String description) { super(ZenithAddons.CATEGORY, "zenith-" + name, description); }
        protected Setting<Double> number(String name, String description, double value, double min, double max) {
            return general.add(new DoubleSetting.Builder().name(name).description(description).defaultValue(value).range(min,max).sliderRange(min,max).build());
        }
        protected Setting<Boolean> flag(String name, String description, boolean value) {
            return general.add(new BoolSetting.Builder().name(name).description(description).defaultValue(value).build());
        }
        protected Color color(double opacity) { tint.set(accent.get()); tint.a = (int)(tint.a * UiMotion.clamp(opacity,0,1)); return tint; }
        protected void submitted() { submissions++; }
        public int submissions() { return submissions; }
        public int retained() { return 0; }
        @Override public void onActivate() { reset(); }
        @Override public void onDeactivate() { reset(); }
        protected void reset() { submissions = 0; }
        protected boolean hasOverlay(double time) { return false; }
        protected void overlay(HudRenderer renderer, Render2DEvent event, double time) {}
        protected boolean worldVisual() { return false; }
        protected void render(Render3DEvent event, double time, EffectVisibility visibility) {}
        protected boolean visible(EffectVisibility view, Vec3 pos, double radius) {
            return mc.player.distanceToSqr(pos) <= 96 * 96 && view.visible(pos.x,pos.y,pos.z,radius);
        }
    }
    private record Pulse(Vec3 position, double born, double strength) {}
    private static void ring(Renderer3D r, Vec3 p, double radius, double width, Color color, double phase, double fraction) {
        int count = Math.min(48, Modules.get().get(ZenithEffects.class).budget().segments());
        for (int i=0;i<count;i++) {
            double a=phase+i*Math.TAU/count*fraction, b=phase+(i+1)*Math.TAU/count*fraction;
            r.quad(p.x+Math.cos(a)*radius,p.y,p.z+Math.sin(a)*radius,
                p.x+Math.cos(b)*radius,p.y,p.z+Math.sin(b)*radius,
                p.x+Math.cos(b)*(radius+width),p.y,p.z+Math.sin(b)*(radius+width),
                p.x+Math.cos(a)*(radius+width),p.y,p.z+Math.sin(a)*(radius+width),color);
        }
    }
    private static void arc(HudRenderer r, double x, double y, double radius, double fraction, Color color) {
        for (int i=0;i<48;i++) {
            double a=-Math.PI/2+i*Math.TAU/48*fraction,b=-Math.PI/2+(i+1)*Math.TAU/48*fraction;
            ZenithDraw.line(r,x+Math.cos(a)*radius,y+Math.sin(a)*radius,x+Math.cos(b)*radius,y+Math.sin(b)*radius,2,color);
        }
    }
    private static void label(HudRenderer r, String text, double x, double y, Color color) {
        r.text(text,x-r.textWidth(text,true,1)/2,y,color,true,1);
    }
    public static final class JumpRings extends Visual {
        private final Setting<Double> radius=number("radius","Maximum expanding ring radius.",1.8,.4,4);
        private final Setting<Double> duration=number("duration","Seconds before a ring fades.",.8,.2,2);
        private final Setting<Boolean> landings=flag("landing-rings","Also emit on landing.",true);
        private final Deque<Pulse> pulses=new ArrayDeque<>();
        private boolean grounded,initialized;
        JumpRings(){super("jump-rings","Expanding violet launch and landing rings. Stores at most eight rings.");}
        @EventHandler private void tick(TickEvent.Post event) {
            if(!world())return;
            boolean ground=mc.player.onGround();
            if(initialized&&ground!=grounded&&((!ground&&mc.player.getDeltaMovement().y>.02)||(ground&&landings.get()))) {
                if(pulses.size()==8)pulses.removeFirst();
                pulses.addLast(new Pulse(mc.player.position().add(0,.025,0),now(),ground?.7:1));
            }
            grounded=ground;initialized=true;pulses.removeIf(p->now()-p.born>duration.get());
        }
        @Override protected boolean worldVisual(){return true;}
        @Override protected void render(Render3DEvent event,double time,EffectVisibility view) {
            for(Pulse p:pulses)if(visible(view,p.position,radius.get()+.2)) {
                double progress=UiMotion.clamp((time-p.born)/duration.get(),0,1);
                ring(event.depthRenderer,p.position,.15+(1-Math.pow(1-progress,3))*radius.get(),.025+.03*(1-progress),color((1-progress)*p.strength),0,1);
                submitted();
            }
        }
        @Override public int retained(){return pulses.size();}
        @Override protected void reset(){super.reset();pulses.clear();initialized=false;}
    }
    public static final class OrbitCrown extends Visual {
        private final Setting<Double> radius=number("radius","Orbit width above your head.",.55,.2,1.5);
        private final Setting<Double> speed=number("speed","Orbit turns per second.",.2,0,2);
        private final Setting<Boolean> firstPerson=flag("first-person","Show your crown in first person too.",false);
        OrbitCrown(){super("orbit-crown","A rotating double crescent and four orbiting stars above your player.");}
        @Override protected boolean worldVisual(){return true;}
        @Override protected void render(Render3DEvent event,double time,EffectVisibility view) {
            if(!firstPerson.get()&&mc.options.getCameraType().isFirstPerson())return;
            Vec3 p=mc.player.getPosition(event.tickDelta).add(0,mc.player.getBbHeight()+.28,0);
            if(!visible(view,p,radius.get()+.2))return;
            double a=time*speed.get()*Math.TAU;
            ring(event.depthRenderer,p,radius.get(),.025,color(.8),a,.38);
            ring(event.depthRenderer,p,radius.get(),.025,color(.8),a+Math.PI,.38);
            for(int i=0;i<4;i++) {
                double b=a+i*Math.PI/2,x=p.x+Math.cos(b)*radius.get(),z=p.z+Math.sin(b)*radius.get();
                event.depthRenderer.line(x-.06,p.y,z,x+.06,p.y,z,color(1));
                event.depthRenderer.line(x,p.y-.08,z,x,p.y+.08,z,color(1));
            }
            submitted();
        }
    }
    public static final class TargetFocus extends Visual {
        private final Setting<Double> speed=number("speed","Reticle rotation speed.",.35,0,2);
        TargetFocus(){super("target-focus","Animated segmented focus rings around the target you last attacked.");}
        @Override protected boolean worldVisual(){return true;}
        @Override protected void render(Render3DEvent event,double time,EffectVisibility view) {
            LivingEntity target=Zenith.INSTANCE.target;if(target==null||!target.isAlive())return;
            Vec3 p=target.getPosition(event.tickDelta).add(0,.04,0);double radius=target.getBbWidth()*.7+.2;
            if(!visible(view,p,radius+2))return;
            for(int i=0;i<3;i++)ring(event.depthRenderer,p,radius,.035,color(.9),time*speed.get()*Math.TAU+i*Math.TAU/3,.2);
            ring(event.depthRenderer,p.add(0,.08+(.5+.5*Math.sin(time*2))*target.getBbHeight(),0),radius,.018,color(.35),0,1);
            submitted();
        }
    }
    public static final class DamageNumbers extends Visual {
        private record Number(Vec3 position,double born,String label) {}
        private final Deque<Number> numbers=new ArrayDeque<>();
        private final Vector3d projected=new Vector3d();
        private final Setting<Double> duration=number("duration","Seconds that confirmed damage remains visible.",1.1,.3,3);
        private LivingEntity watched;private float health;
        DamageNumbers(){super("damage-numbers","Floating numbers for confirmed health loss on your last attacked target.");}
        @EventHandler(priority=EventPriority.LOWEST) private void attack(AttackEntityEvent event) {
            if(!event.isCancelled()&&event.entity instanceof LivingEntity target){watched=target;health=target.getHealth()+target.getAbsorptionAmount();}
        }
        @EventHandler private void tick(TickEvent.Post event) {
            if(watched!=null) {
                float current=watched.getHealth()+watched.getAbsorptionAmount();
                if(current<health){if(numbers.size()==24)numbers.removeFirst();numbers.addLast(new Number(watched.position().add(0,watched.getBbHeight()+.15,0),now(),String.format(Locale.ROOT,"−%.1f",health-current)));}
                health=current;if(watched.isRemoved()||watched!=Zenith.INSTANCE.target)watched=null;
            }
            numbers.removeIf(n->now()-n.born>duration.get());
        }
        @Override protected boolean hasOverlay(double time){return !numbers.isEmpty();}
        @Override protected void overlay(HudRenderer r,Render2DEvent event,double time) {
            for(Number n:numbers) {
                double age=UiMotion.clamp((time-n.born)/duration.get(),0,1);
                projected.set(n.position.x,n.position.y+age*.9,n.position.z);
                if(NametagUtils.to2D(projected,1,false)){label(r,n.label,projected.x,projected.y,color(1-age*age));submitted();}
            }
        }
        @Override public int retained(){return numbers.size();}
        @Override protected void reset(){super.reset();numbers.clear();watched=null;}
    }
    public static final class ViewmodelMotion extends Visual {
        private final Setting<Double> strength=number("strength","Subtle first-person sway and breathing strength.",.35,0,1.5);
        private final Setting<Double> roll=number("roll","Maximum idle roll in degrees.",4,0,15);
        private final Setting<Boolean> pauseUsing=flag("pause-while-using","Keep food, blocking and bow use steady.",true);
        ViewmodelMotion(){super("viewmodel-motion","Smooth breathing, walking sway and roll for held items. Does not change attack timing.");}
        @EventHandler private void hand(HeldItemRendererEvent event) {
            if(!world()||(pauseUsing.get()&&mc.player.isUsingItem()))return;
            double t=now(),s=strength.get(),side=event.hand==InteractionHand.MAIN_HAND?1:-1;
            double moving=Math.min(1,mc.player.getDeltaMovement().horizontalDistance()*5);
            event.matrix.translate(Math.sin(t*1.7)*.018*s*side,Math.sin(t*2.2)*.025*s+Math.sin(t*9)*moving*.018*s,0);
            event.matrix.mulPose(Axis.ZP.rotationDegrees((float)(Math.sin(t*1.6)*roll.get()*s*side)));
            submitted();
        }
    }
    public static final class CrosshairPulse extends Visual {
        private final Setting<Double> radius=number("radius","Cooldown ring radius in screen pixels.",15,6,45);
        private final Setting<Boolean> cooldown=flag("cooldown","Show the actual vanilla attack cooldown.",true);
        private double attacked=-100;
        CrosshairPulse(){super("crosshair-pulse","Smooth attack cooldown ring and an expanding attack-input ripple.");}
        @EventHandler(priority=EventPriority.LOWEST) private void attack(AttackEntityEvent event){if(!event.isCancelled())attacked=now();}
        @Override protected boolean hasOverlay(double time){return mc.gui.screen()==null;}
        @Override protected void overlay(HudRenderer r,Render2DEvent event,double time) {
            double x=mc.getWindow().getWidth()/2.0,y=mc.getWindow().getHeight()/2.0;
            if(cooldown.get())arc(r,x,y,radius.get(),mc.player.getAttackStrengthScale(event.tickDelta),color(.7));
            double age=(time-attacked)/.4;if(age>=0&&age<1)arc(r,x,y,radius.get()+age*15,1,color(1-age));
            submitted();
        }
        @Override protected void reset(){super.reset();attacked=-100;}
    }
    public static final class LowHealthGlow extends Visual {
        private final Setting<Double> threshold=number("health-threshold","Start the edge pulse below this many health points.",8,1,20);
        private final Setting<Double> opacity=number("opacity","Maximum edge opacity.",.35,0,.8);
        private double fade;
        LowHealthGlow(){super("low-health-glow","A soft pulsing violet edge warning that fades as health recovers.");}
        @Override protected boolean hasOverlay(double time){return mc.player.getHealth()<threshold.get()||fade>.005;}
        @Override protected void overlay(HudRenderer r,Render2DEvent event,double time) {
            double target=UiMotion.clamp(1-mc.player.getHealth()/threshold.get(),0,1);
            fade=UiMotion.approach(fade,target,8,event.frameTime);
            double alpha=fade*opacity.get()*(.7+.3*Math.sin(time*4));
            for(int i=0;i<24;i++) {
                Color c=color(alpha*Math.pow(1-i/24.0,2));double inset=i*2;
                r.quad(inset,0,2,mc.getWindow().getHeight(),c);r.quad(mc.getWindow().getWidth()-inset-2,0,2,mc.getWindow().getHeight(),c);
                r.quad(48,inset,mc.getWindow().getWidth()-96,2,c);r.quad(48,mc.getWindow().getHeight()-inset-2,mc.getWindow().getWidth()-96,2,c);
            }
            submitted();
        }
        @Override protected void reset(){super.reset();fade=0;}
    }
    public static final class ArmorWarning extends Visual {
        private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        private final Setting<Double> threshold=number("durability-percent","Warn when equipped armor drops below this percentage.",20,1,75);
        private String warning="";private double fade;
        ArmorWarning(){super("armor-warning","Animated low-durability warning for equipped armor, sampled once per tick.");}
        @EventHandler private void tick(TickEvent.Post event) {
            if(!world())return;warning="";double lowest=101;
            for(EquipmentSlot slot:SLOTS){var item=mc.player.getItemBySlot(slot);if(item.isDamageableItem()){
                double remaining=100.0*(item.getMaxDamage()-item.getDamageValue())/item.getMaxDamage();
                if(remaining<threshold.get()&&remaining<lowest){lowest=remaining;warning=item.getHoverName().getString()+" · "+(int)remaining+"%";}
            }}
        }
        @Override protected boolean hasOverlay(double time){return !warning.isEmpty();}
        @Override protected void overlay(HudRenderer r,Render2DEvent event,double time) {
            fade=UiMotion.approach(fade,1,12,event.frameTime);double width=r.textWidth(warning,true,1)+32,x=(mc.getWindow().getWidth()-width)/2,y=mc.getWindow().getHeight()*.74;
            ZenithDraw.rounded(r,x,y+8*(1-fade),width,34,9,new Color(25,14,35,(int)(210*fade)));
            label(r,warning,mc.getWindow().getWidth()/2.0,y+8,color(fade*(.75+.25*Math.sin(time*3))));submitted();
        }
        @Override protected void reset(){super.reset();warning="";fade=0;}
    }
    public static final class ModuleToasts extends Visual {
        private record Toast(String text,double born,boolean enabled) {}
        private final Set<Module> previous=new HashSet<>();
        private final Deque<Toast> toasts=new ArrayDeque<>();
        private final Setting<Double> duration=number("duration","Seconds before module notifications disappear.",2,.5,5);
        ModuleToasts(){super("module-toasts","Up to four animated toggle notifications, updated only on module changes.");}
        @Override public void onActivate(){super.onActivate();previous.addAll(Modules.get().getActive());}
        @EventHandler private void changed(ActiveModulesChangedEvent event) {
            var active=Modules.get().getActive();
            for(Module module:active)if(module!=this&&!previous.contains(module))add(module,true);
            for(Module module:previous)if(module!=this&&!active.contains(module))add(module,false);
            previous.clear();previous.addAll(active);
        }
        private void add(Module module,boolean enabled){if(toasts.size()==4)toasts.removeFirst();toasts.addLast(new Toast(module.title,now(),enabled));}
        @Override protected boolean hasOverlay(double time){toasts.removeIf(t->time-t.born>duration.get());return !toasts.isEmpty();}
        @Override protected void overlay(HudRenderer r,Render2DEvent event,double time) {
            int i=0;for(Toast toast:toasts){double age=time-toast.born,fade=Math.min(1,Math.min(age/.18,(duration.get()-age)/.3));
                String text=toast.text+(toast.enabled?"  ON":"  OFF");double width=r.textWidth(text,true,1)+30;
                double x=mc.getWindow().getWidth()-width-20+(1-fade)*35,y=mc.getWindow().getHeight()*.22+i++*44;
                ZenithDraw.rounded(r,x,y,width,35,9,new Color(23,15,34,(int)(225*fade)));
                r.text(text,x+15,y+8,color(fade),true,1);submitted();
            }
        }
        @Override public int retained(){return toasts.size();}
        @Override protected void reset(){super.reset();toasts.clear();previous.clear();}
    }
    public static final class WaypointBeacon extends Visual {
        private final Setting<Double> x=number("x","Destination X coordinate.",0,-30000000,30000000);
        private final Setting<Double> y=number("y","Destination Y coordinate.",64,-64,320);
        private final Setting<Double> z=number("z","Destination Z coordinate.",0,-30000000,30000000);
        private final Setting<Boolean> capture=flag("capture-on-enable","Use your current position when enabling; disable for manual coordinates.",true);
        private final Setting<Double> height=number("height","Beacon height.",4,1,12);
        private String dimension="";
        WaypointBeacon(){super("waypoint-beacon","A local animated destination marker. Captures your position or uses manual coordinates.");}
        @Override public void onActivate(){super.onActivate();if(world()){dimension=mc.level.dimension().identifier().toString();if(capture.get()){x.set(mc.player.getX());y.set(mc.player.getY());z.set(mc.player.getZ());}}}
        @Override protected boolean worldVisual(){return true;}
        @Override protected void render(Render3DEvent event,double time,EffectVisibility view) {
            if(!dimension.isEmpty()&&!dimension.equals(mc.level.dimension().identifier().toString()))return;
            Vec3 p=new Vec3(x.get(),y.get()+.04,z.get());if(!visible(view,p.add(0,height.get()/2,0),height.get()))return;
            ring(event.depthRenderer,p,.6+.08*Math.sin(time*3),.035,color(.85),time*.4,1);
            for(int i=0;i<3;i++){double angle=time+i*Math.TAU/3,dx=Math.cos(angle)*.25,dz=Math.sin(angle)*.25;
                event.depthRenderer.line(p.x+dx,p.y,p.z+dz,p.x+dx,p.y+height.get(),p.z+dz,color(.6));}
            ring(event.depthRenderer,p.add(0,height.get(),0),.3,.025,color(.9),-time,.75);submitted();
        }
        @Override protected void reset(){super.reset();dimension="";}
    }
}
