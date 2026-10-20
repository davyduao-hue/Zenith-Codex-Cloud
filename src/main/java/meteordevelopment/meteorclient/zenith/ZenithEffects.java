package meteordevelopment.meteorclient.zenith;

import com.mojang.math.Axis;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.world.ParticleEvent;
import net.minecraft.core.particles.ParticleTypes;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Local cosmetic geometry, with explicit storage, distance, and tessellation budgets. */
public final class ZenithEffects extends Module {
    public enum Quality { Low, Balanced, High }
    public enum Palette { VideoReference, ZenithPurple }
    public enum SkyMood { EmberDusk, RainySilver, Purple }
    private final SettingGroup general = settings.getDefaultGroup();
    private final SettingGroup visuals = settings.createGroup("Effects");
    public final Setting<Quality> quality = general.add(new EnumSetting.Builder<Quality>().name("quality")
        .description("Controls ring segments, particle count and active-effect budgets.").defaultValue(Quality.Balanced).build());
    public final Setting<SettingColor> accent = general.add(new ColorSetting.Builder().name("accent")
        .description("Color of rings, crystal coronas, pearl trails, and death effects.")
        .defaultValue(new SettingColor(183, 117, 255)).build());
    private final Setting<Palette> palette=general.add(new EnumSetting.Builder<Palette>().name("effect-palette")
        .description("Video Reference uses icy sword impacts, golden crystal explosions, and violet pearl trails.").defaultValue(Palette.VideoReference).build());
    private final Setting<SkyMood> skyMood=general.add(new EnumSetting.Builder<SkyMood>().name("sky-mood")
        .description("Ember dusk from the crystal clip, silver from the sword clip, or Zenith purple.").defaultValue(SkyMood.EmberDusk).build());
    private final Setting<Double> flashStrength=general.add(new DoubleSetting.Builder().name("explosion-glow")
        .description("Brightness of the explosion's soft white-gold core. Zero removes the flash.").defaultValue(.7).range(0,1).sliderRange(0,1).build());
    public final Setting<Boolean> purpleSky = bool("purple-sky", "Override sky color using the selected sky mood.", true);
    public final Setting<Boolean> starfield = bool("starfield", "Render a subtle procedural constellation skybox.", true);
    public final Setting<Boolean> rain = bool("cinematic-rain", "Local rain; does not change server weather.", false);
    private final Setting<Boolean> hits = bool("hit-effects", "Damage-confirmed shock rings and star sparks.", true);
    private final Setting<Boolean> crystals = bool("crystal-visuals", "Orbiting crystal coronas and rotating wire diamonds.", true);
    private final Setting<Boolean> explosions = bool("explosion-effects", "Layered expanding rings and particle shards for server explosions.", true);
    private final Setting<Boolean> replaceExplosionSmoke = bool("replace-explosion-smoke", "Replace vanilla explosion clouds with the custom golden glow and shards.", true);
    private final Setting<Boolean> pearls = bool("pearl-visuals", "Fading trails and a corona around thrown pearls.", true);
    private final Setting<Boolean> deaths = bool("death-visuals", "Ascending rings, shards and a light column on entity death.", true);
    private final Setting<Boolean> sword = bool("sword-animation", "Adds a smooth sideways slash to first-person sword swings.", true);
    private final Setting<Integer> range = general.add(new IntSetting.Builder().name("render-distance")
        .description("Effects farther away than this distance are discarded.").defaultValue(48).range(8, 96).sliderRange(8, 96).build());
    private final ArrayList<Burst> bursts = new ArrayList<>();
    private final ArrayList<Spark> sparks = new ArrayList<>();
    private final ArrayList<Entity> tracked = new ArrayList<>();
    private final Map<Integer, ArrayDeque<Vec3>> trails = new HashMap<>();
    private final Map<Integer,Integer> trailSeen=new HashMap<>();
    private final Color tint = new Color(), white = new Color(238, 220, 255), clear = new Color(183,117,255,0);
    private int tick;
    private boolean forcedRain;
    private float originalRain, originalThunder;
    private Renderer3D glowRenderer;
    private int hitBursts, explosionBursts, deathBursts, pearlSamples, crystalFrames,replacedClouds;
    public record Statistics(int hits, int explosions, int deaths, int pearlSamples, int crystalFrames, int sparks,int replacedClouds) {}
    public Statistics statistics() { return new Statistics(hitBursts,explosionBursts,deathBursts,pearlSamples,crystalFrames,sparks.size(),replacedClouds); }
    private static final double[][] STARS = new double[80][3];
    static {
        Random random = new Random(0x2e1717);
        for (double[] star : STARS) {
            double a = random.nextDouble() * Math.TAU, height = .2 + random.nextDouble() * .78;
            double horizontal = Math.sqrt(1 - height * height);
            star[0] = Math.cos(a) * horizontal * 190;
            star[1] = height * 190;
            star[2] = Math.sin(a) * horizontal * 190;
        }
    }

    public ZenithEffects() {
        super(Categories.Render, "zenith-effects", "Video-inspired icy hits, golden crystal shockwaves, pearl ribbons, sky and death visuals.");
    }
    public int skyColor(){return switch(skyMood.get()){case EmberDusk->0xff8a344a;case RainySilver->0xff9faebb;case Purple->0xff553475;};}
    public org.joml.Vector4f fogTint(org.joml.Vector4f original) {
        int rgb=skyColor();float amount=skyMood.get()==SkyMood.EmberDusk?.3f:.18f;
        return new org.joml.Vector4f(original.x*(1-amount)+(rgb>>16&255)/255f*amount,
            original.y*(1-amount)+(rgb>>8&255)/255f*amount,original.z*(1-amount)+(rgb&255)/255f*amount,original.w);
    }
    public int cloudTint(int original) {
        int rgb=skyColor();double a=.45;
        int r=(int)((original>>16&255)*(1-a)+(rgb>>16&255)*a);
        int g=(int)((original>>8&255)*(1-a)+(rgb>>8&255)*a),b=(int)((original&255)*(1-a)+(rgb&255)*a);
        return (original&0xff000000)|r<<16|g<<8|b;
    }
    private void colorFor(int kind,int alpha) {
        if(palette.get()==Palette.ZenithPurple)tint.set(accent.get());
        else switch(kind){case 0->tint.set(169,220,255,255);case 1,3->tint.set(255,151,48,255);case 2->tint.set(204,223,255,255);default->tint.set(196,135,255,255);}
        tint.a=Math.max(0,Math.min(255,alpha));
    }

    private Setting<Boolean> bool(String name, String description, boolean value) {
        return visuals.add(new BoolSetting.Builder().name(name).description(description).defaultValue(value).build());
    }

    public void hit(LivingEntity entity) {
        if (hits.get()) burst(entity.position().add(0, entity.getBbHeight() * .55, 0), 0);
    }

    @EventHandler private void onParticle(ParticleEvent event) {
        if(explosions.get()&&replaceExplosionSmoke.get()&&(event.particle.getType()==ParticleTypes.EXPLOSION||event.particle.getType()==ParticleTypes.EXPLOSION_EMITTER)){event.cancel();replacedClouds++;}
    }
    public boolean replaceExplosionAt(Vec3 center) {
        if(!isActive()||!explosions.get()||!replaceExplosionSmoke.get()||mc.player==null||mc.player.distanceToSqr(center)>range.get()*range.get())return false;
        replacedClouds++;return true;
    }

    private void burst(Vec3 pos, int type) {
        if (mc.player == null || mc.player.position().distanceToSqr(pos) > range.get() * range.get()) return;
        int limit = quality.get() == Quality.Low ? 12 : 32;
        if (bursts.size() >= limit) bursts.removeFirst();
        bursts.add(new Burst(pos, type));
        switch(type) { case 0 -> hitBursts++; case 1 -> explosionBursts++; case 2 -> deathBursts++; default -> {} }
        int count = quality.get() == Quality.Low ? 8 : quality.get() == Quality.High ? 44 : 22;
        int budget = quality.get() == Quality.Low ? 128 : quality.get() == Quality.High ? 768 : 384;
        for (int i = 0; i < count && sparks.size() < budget; i++) {
            double angle = Math.random() * Math.TAU, speed = (.04 + Math.random() * .13) * (type == 0 ? 1 : 2);
            sparks.add(new Spark(pos, Math.cos(angle) * speed, .03 + Math.random() * .16, Math.sin(angle) * speed,type));
        }
    }

    @EventHandler private void onPacket(PacketEvent.Receive event) {
        // Network callbacks are marshalled before touching client render state.
        if (event.packet instanceof ClientboundExplodePacket packet && explosions.get()) {
            Vec3 center = packet.center();
            mc.execute(() -> { if (isActive()) burst(center, 1); });
        } else if (event.packet instanceof ClientboundEntityEventPacket packet && deaths.get() && packet.getEventId() == 3) {
            mc.execute(() -> {
                if (!isActive() || mc.level == null) return;
                Entity entity = packet.getEntity(mc.level);
                if (entity instanceof LivingEntity) burst(entity.position().add(0, .25, 0), 2);
            });
        }
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (mc.level == null || mc.player == null) return;
        tick++;
        bursts.removeIf(b -> ++b.age > (b.type == 2 ? 42 : 24));
        sparks.removeIf(p -> ++p.age > 28);
        for (Spark p : sparks) { p.x += p.vx; p.y += p.vy; p.z += p.vz; p.vy -= .004; p.vx *= .96; p.vz *= .96; }
        updateRain();
        if (tick % 4 == 0) {
            tracked.clear();
            int inspected = 0;
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (++inspected > 2048 || tracked.size() >= 32) break;
                if (((crystals.get() && entity instanceof EndCrystal) || (pearls.get() && entity instanceof ThrownEnderpearl))
                    && mc.player.distanceToSqr(entity) < range.get() * range.get()) tracked.add(entity);
            }
            trails.keySet().removeIf(id -> tick-trailSeen.getOrDefault(id,0)>18);
            trailSeen.keySet().retainAll(trails.keySet());
        }
        for (Entity entity : tracked) {
            if (entity instanceof ThrownEnderpearl && !entity.isRemoved()) {
                if(!trails.containsKey(entity.getId())&&trails.size()>=32)continue;
                ArrayDeque<Vec3> trail = trails.computeIfAbsent(entity.getId(), _ -> new ArrayDeque<>());
                if (trail.size() == 32) trail.removeFirst();
                trail.addLast(entity.position());
                trailSeen.put(entity.getId(),tick);
                pearlSamples++;
            }
        }
    }

    private void updateRain() {
        if (rain.get()) {
            if (!forcedRain) {
                originalRain = mc.level.getRainLevel(1); originalThunder = mc.level.getThunderLevel(1); forcedRain = true;
            }
            mc.level.setRainLevel(.75f); mc.level.setThunderLevel(.1f);
        } else restoreRain();
    }

    private void restoreRain() {
        if (forcedRain && mc.level != null) { mc.level.setRainLevel(originalRain); mc.level.setThunderLevel(originalThunder); }
        forcedRain = false;
    }

    @Override public void onDeactivate() { restoreRain(); clear(); }
    @EventHandler private void onLeave(GameLeftEvent event) { clear(); forcedRain = false; }
    private void clear() { bursts.clear(); sparks.clear(); tracked.clear(); trails.clear();trailSeen.clear(); hitBursts=explosionBursts=deathBursts=pearlSamples=crystalFrames=replacedClouds=0; }

    @EventHandler private void onHand(HeldItemRendererEvent event) {
        if (!sword.get() || mc.player == null) return;
        ItemStack stack = mc.player.getItemInHand(event.hand);
        if (!stack.is(ItemTags.SWORDS)) return;
        float progress = mc.player.getAttackAnim(1);
        float wave = (float) Math.sin(progress * Math.PI);
        event.matrix.mulPose(Axis.ZP.rotationDegrees(-26 * wave));
        event.matrix.mulPose(Axis.YP.rotationDegrees(18 * wave));
        event.matrix.translate(-.12 * wave, .04 * wave, -.08 * wave);
    }

    @EventHandler private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (glowRenderer == null) glowRenderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH, MeteorRenderPipelines.ZENITH_GLOW);
        Renderer3D r = glowRenderer;
        r.begin();
        int segments = quality.get() == Quality.Low ? 24 : quality.get() == Quality.High ? 64 : 40;
        double time = (tick + event.tickDelta) / 20.0;
        if (starfield.get()) {
            // A fixed seed and camera-relative dome avoid texture downloads and per-frame allocation.
            tint.set(accent.get()); tint.a = 135;
            Vec3 origin = mc.player.position();
            int count = quality.get() == Quality.Low ? 32 : STARS.length;
            for (int i = 0; i < count; i++) {
                double[] star = STARS[i];
                double x=origin.x+star[0], y=origin.y+star[1], z=origin.z+star[2];
                double size=.08+.05*Math.sin(time*.5+i);
                r.line(x-size,y,z,x+size,y,z,tint);
                r.line(x,y-size,z,x,y+size,z,tint);
            }
        }
        for (Burst burst : bursts) {
            double age = (burst.age + event.tickDelta) / (burst.type == 2 ? 42.0 : 24.0);
            double radius = .25 + (1 - Math.pow(1 - Math.min(1, age), 3)) * (burst.type == 0 ? 1.05 : 3.8);
            colorFor(burst.type,(int)(220*(1-age)));
            Vec3 p = burst.pos;
            if(burst.type==0) {
                // The sword reference has a white camera-facing shock ring and short star flares.
                ring(r,p.x,p.y,p.z,radius,.018+age*.025,tint,segments,true);
                halo(r,p.x,p.y,p.z,radius*.6,(int)(38*(1-age)),0,segments,false);
            } else {
                ring(r, p.x, p.y + (burst.type == 2 ? age * 3 : .02), p.z, radius, .028 + age * .045, tint, segments, false);
                ring(r, p.x, p.y + .45, p.z, radius * .78, .045, tint, segments, true);
            }
            if (burst.type > 0) {
                ring(r, p.x, p.y + age * .7, p.z, radius * .55, .055, tint, segments, false);
                if(burst.type==1) {
                    halo(r,p.x,p.y+.5,p.z,1.7+age*1.2,(int)(155*(1-age)*(1-age)*flashStrength.get()),1,segments,false);
                    halo(r,p.x,p.y+.1,p.z,radius*1.15,(int)(80*(1-age)*flashStrength.get()),1,segments,true);
                    colorFor(1,(int)(180*(1-age)));
                    ring(r,p.x,p.y+.45,p.z,radius*.95,.018,tint,segments,true);
                } else {
                    tint.a/=3;
                    r.quad(p.x-.1,p.y,p.z,p.x+.1,p.y,p.z,p.x+.055,p.y+4*(1-age),p.z,p.x-.055,p.y+4*(1-age),p.z,tint);
                }
            }
        }
        for (Entity entity : tracked) {
            if (entity.isRemoved()) continue;
            Vec3 p = entity.position();
            if (entity instanceof EndCrystal && crystals.get()) {
                crystalFrames++;
                colorFor(1,160);
                double y = p.y + 1.0 + Math.sin(time * 2 + entity.getId()) * .18;
                halo(r,p.x,p.y+.025,p.z,1.13,80,1,segments,true);
                colorFor(1,160);
                ring(r, p.x, p.y + .035, p.z, .86, .025, tint, segments, false);
                ring(r, p.x, p.y + .04, p.z, .65, .014, tint, segments, false);
                ring(r, p.x, y, p.z, .62, .012, tint, segments, true);
                for (int i = 0; i < 8; i++) {
                    colorFor(1,170);
                    double a = time * 1.5 + i * Math.TAU / 4;
                    double b=a+Math.PI*.6;
                    r.line(p.x+Math.cos(a)*.67,p.y+.04,p.z+Math.sin(a)*.67,p.x+Math.cos(b)*.83,p.y+.04,p.z+Math.sin(b)*.83,tint);
                    particle(r,p.x+Math.cos(a+i*.4)*.8,y+Math.sin(time*1.7+i)*.65,p.z+Math.sin(a+i*.4)*.8,.045,tint);
                }
            } else if (entity instanceof ThrownEnderpearl && pearls.get()) {
                colorFor(4,175);
                ring(r, p.x, p.y, p.z, .18, .025, tint, 20, true);
                halo(r,p.x,p.y,p.z,.3,75,4,20,false);
            }
        }
        if(pearls.get())for(var entry:trails.entrySet()) {
            ArrayDeque<Vec3> trail=entry.getValue();Vec3 previous=null;int i=0;
            double fade=1-UiMotion.clamp((tick-trailSeen.getOrDefault(entry.getKey(),tick)+event.tickDelta)/18.0,0,1);
            double yaw=Math.toRadians(mc.player.getYRot());
            for(Vec3 point:trail) {
                double t=++i/(double)trail.size();colorFor(4,(int)(190*t*fade));
                if(previous!=null) {
                    double w=.025*t,dx=Math.cos(yaw)*w,dz=Math.sin(yaw)*w;
                    r.quad(previous.x-dx,previous.y,previous.z-dz,point.x-dx,point.y,point.z-dz,
                        point.x+dx,point.y,point.z+dz,previous.x+dx,previous.y,previous.z+dz,tint);
                    r.line(previous.x,previous.y,previous.z,point.x,point.y,point.z,tint);
                }
                previous=point;
            }
        }
        for (Spark p : sparks) {
            colorFor(p.kind,(int)(235*(1-p.age/28.0)));
            particle(r, p.x, p.y, p.z, .05 + .07 * (1 - p.age / 28.0), tint);
            if(p.kind==1&&p.age<18)shard(r,p.x,p.y,p.z,.08*(1-p.age/28.0),time+p.age,tint);
        }
        r.render(event.matrices);
    }

    /** An annulus has a translucent skirt and a white core, avoiding a full-screen shader pass. */
    private void ring(Renderer3D r, double x, double y, double z, double radius, double width, Color color, int segments, boolean vertical) {
        white.set(color.r>color.g?255:228,245,color.r>color.g?226:255,color.a); clear.set(color); clear.a = 0;
        double yaw = mc.player.getYRot() * Math.PI / 180;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.TAU / segments, b = (i+1) * Math.TAU / segments;
            double ax = Math.cos(a), az = Math.sin(a), bx = Math.cos(b), bz = Math.sin(b);
            for (int band = 0; band < 4; band++) {
                double inner = Math.max(.001,radius + (band-2)*width), outer = Math.max(.002,inner+width);
                Color in = switch (band) { case 0 -> clear; case 1, 3 -> color; default -> white; };
                Color out = switch (band) { case 0, 2 -> color; case 1 -> white; default -> clear; };
                if (vertical) {
                    r.quad(x+ax*inner*Math.cos(yaw),y+az*inner,z+ax*inner*Math.sin(yaw),
                        x+bx*inner*Math.cos(yaw),y+bz*inner,z+bx*inner*Math.sin(yaw),
                        x+bx*outer*Math.cos(yaw),y+bz*outer,z+bx*outer*Math.sin(yaw),
                        x+ax*outer*Math.cos(yaw),y+az*outer,z+ax*outer*Math.sin(yaw),in,out,out,in);
                } else {
                    r.quad(x+ax*inner,y,z+az*inner,x+bx*inner,y,z+bz*inner,
                        x+bx*outer,y,z+bz*outer,x+ax*outer,y,z+az*outer,in,out,out,in);
                }
            }
        }
    }

    private void particle(Renderer3D r,double x,double y,double z,double size,Color color) {
        double yaw=Math.toRadians(mc.player.getYRot()),pitch=Math.toRadians(mc.player.getXRot());
        double rx=Math.cos(yaw)*size,rz=Math.sin(yaw)*size;
        double ux=-Math.sin(yaw)*Math.sin(pitch)*size,uy=Math.cos(pitch)*size,uz=Math.cos(yaw)*Math.sin(pitch)*size;
        clear.set(color);clear.a=0;white.a=color.a;
        for(int i=0;i<4;i++) {
            double a=i*Math.PI/2,b=(i+1)*Math.PI/2;
            r.triangles.ensureTriCapacity();
            int center=r.triangles.vec3(x,y,z).color(white).next();
            int first=r.triangles.vec3(x+rx*Math.cos(a)+ux*Math.sin(a),y+uy*Math.sin(a),z+rz*Math.cos(a)+uz*Math.sin(a)).color(clear).next();
            int second=r.triangles.vec3(x+rx*Math.cos(b)+ux*Math.sin(b),y+uy*Math.sin(b),z+rz*Math.cos(b)+uz*Math.sin(b)).color(clear).next();
            r.triangles.triangle(center,first,second);
        }
    }
    private void halo(Renderer3D r,double x,double y,double z,double radius,int alpha,int kind,int segments,boolean horizontal) {
        colorFor(kind,alpha);clear.set(tint);clear.a=0;white.set(255,245,226,alpha);
        if(kind==0)white.set(224,245,255,alpha);
        double yaw=Math.toRadians(mc.player.getYRot()),pitch=Math.toRadians(mc.player.getXRot());
        for(int i=0;i<segments;i++) {
            double a=i*Math.TAU/segments,b=(i+1)*Math.TAU/segments;
            double ax,bx,ay,by,az,bz;
            if(horizontal){ax=Math.cos(a)*radius;az=Math.sin(a)*radius;ay=0;bx=Math.cos(b)*radius;bz=Math.sin(b)*radius;by=0;}
            else {ax=(Math.cos(yaw)*Math.cos(a)-Math.sin(yaw)*Math.sin(pitch)*Math.sin(a))*radius;
                ay=Math.cos(pitch)*Math.sin(a)*radius;az=(Math.sin(yaw)*Math.cos(a)+Math.cos(yaw)*Math.sin(pitch)*Math.sin(a))*radius;
                bx=(Math.cos(yaw)*Math.cos(b)-Math.sin(yaw)*Math.sin(pitch)*Math.sin(b))*radius;
                by=Math.cos(pitch)*Math.sin(b)*radius;bz=(Math.sin(yaw)*Math.cos(b)+Math.cos(yaw)*Math.sin(pitch)*Math.sin(b))*radius;}
            r.triangles.ensureTriCapacity();
            int c=r.triangles.vec3(x,y,z).color(white).next(),v1=r.triangles.vec3(x+ax,y+ay,z+az).color(clear).next(),v2=r.triangles.vec3(x+bx,y+by,z+bz).color(clear).next();
            r.triangles.triangle(c,v1,v2);
        }
    }
    private void shard(Renderer3D r,double x,double y,double z,double size,double angle,Color color) {
        double yaw=Math.toRadians(mc.player.getYRot()),dx=Math.cos(angle)*size,dy=Math.sin(angle)*size;
        double ex=-dy,ey=dx,rx=Math.cos(yaw),rz=Math.sin(yaw);
        r.quad(x+(dx+ex)*rx,y+dy+ey,z+(dx+ex)*rz,x+(dx-ex)*rx,y+dy-ey,z+(dx-ex)*rz,
            x+(-dx-ex)*rx,y-dy-ey,z+(-dx-ex)*rz,x+(-dx+ex)*rx,y-dy+ey,z+(-dx+ex)*rz,color);
    }

    private static final class Burst {
        final Vec3 pos; final int type; int age;
        Burst(Vec3 pos, int type) { this.pos=pos; this.type=type; }
    }
    private static final class Spark {
        double x,y,z,vx,vy,vz; int age;final int kind;
        Spark(Vec3 p,double vx,double vy,double vz,int kind) { x=p.x; y=p.y; z=p.z; this.vx=vx; this.vy=vy; this.vz=vz;this.kind=kind; }
    }
}
