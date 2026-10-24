package meteordevelopment.meteorclient.zenith;

import com.mojang.math.Axis;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.world.ParticleEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
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
    public enum VisualPreset { ReferenceCrystal, ReferenceSword, ZenithPurple, Custom }
    public enum Palette { VideoReference, ZenithPurple }
    public enum SkySource { Zenith, Vanilla, ResourcePack, CustomPanorama }
    public enum SkyMood { EmberDusk, RainySilver, Purple, Aurora }
    private final SettingGroup general = settings.getDefaultGroup();
    private final SettingGroup visuals = settings.createGroup("Effects");
    private final SettingGroup accents = settings.createGroup("Extra Effects");
    private final SettingGroup skySettings=settings.createGroup("Skybox");
    public final Setting<VisualPreset> visualPreset = general.add(new EnumSetting.Builder<VisualPreset>().name("visual-preset")
        .description("Apply the crystal video, silver sword video, or purple Zenith look. Individual controls stay editable.")
        .defaultValue(VisualPreset.ReferenceCrystal).onChanged(this::applyPreset).build());
    public final Setting<Quality> quality = general.add(new EnumSetting.Builder<Quality>().name("quality")
        .description("Controls ring segments, particle count and active-effect budgets.").defaultValue(Quality.Balanced).build());
    public final Setting<Boolean> offscreenCulling=general.add(new BoolSetting.Builder().name("offscreen-culling")
        .description("Skip effects outside the camera view; generous bounds keep glows and trails visible at the edges.")
        .defaultValue(true).build());
    private final EffectVisibility visibility=new EffectVisibility();
    private int culledGroups,visibleCrystals;
    public int culledGroups(){return culledGroups;}
    public int visibleCrystals(){return visibleCrystals;}
    public int skyMeshBuilds(){return skybox==null?0:skybox.meshBuilds();}
    private boolean visible(double x,double y,double z,double radius) {
        if(!offscreenCulling.get()||visibility.visible(x,y,z,radius))return true;
        culledGroups++;return false;
    }
    public final Setting<Boolean> economyMode=general.add(new BoolSetting.Builder().name("economy-mode")
        .description("Smaller effect, sky and entity budgets. Preserves your quality selection; does not cap system CPU/GPU usage.")
        .defaultValue(false).build());
    public final Setting<SettingColor> accent = general.add(new ColorSetting.Builder().name("accent")
        .description("Color of rings, crystal coronas, pearl trails, and death effects.")
        .defaultValue(new SettingColor(183, 117, 255)).build());
    private final Setting<Palette> palette=general.add(new EnumSetting.Builder<Palette>().name("effect-palette")
        .description("Video Reference uses pale-gold sword flashes, golden crystal explosions, and white-violet pearl trails.").defaultValue(Palette.VideoReference).build());
    private final Setting<SkyMood> skyMood=general.add(new EnumSetting.Builder<SkyMood>().name("sky-mood")
        .description("Warm dusk, storm silver, deep purple or teal aurora atmosphere.").defaultValue(SkyMood.EmberDusk).build());
    private final Setting<Double> intensity=general.add(new DoubleSetting.Builder().name("effect-density")
        .description("Particle emission multiplier; quality caps still apply.").defaultValue(1.5).range(.25,3).sliderRange(.25,3).build());
    private final Setting<Double> effectSize=general.add(new DoubleSetting.Builder().name("effect-size")
        .description("Scale of shockwaves, slashes, coronas and death spirals.").defaultValue(1.15).range(.5,2).sliderRange(.5,2).build());
    public final Setting<SkySource> skySource=skySettings.add(new EnumSetting.Builder<SkySource>().name("sky-source")
        .description("Zenith atmosphere, native sky, resource-pack renderer, or a custom 2:1 panorama from an enabled pack.")
        .defaultValue(SkySource.Zenith).build());
    public final Setting<String> panoramaTexture=skySettings.add(new StringSetting.Builder().name("panorama-texture")
        .description("Resource location of a 2:1 equirectangular PNG. Missing textures fall back to the native sky.")
        .defaultValue("zenith:textures/sky/panorama.png").visible(()->skySource.get()==SkySource.CustomPanorama).build());
    public final Setting<Double> skyRotation=skySettings.add(new DoubleSetting.Builder().name("sky-rotation")
        .description("Rotate the custom panorama around the horizon.").defaultValue(0).range(0,360).sliderRange(0,360)
        .visible(()->skySource.get()==SkySource.CustomPanorama).build());
    public final Setting<Boolean> animatedSky=skySettings.add(new BoolSetting.Builder().name("animated-skybox")
        .description("Red dusk, silver storm, or purple procedural sky rendered behind terrain.").defaultValue(true).build());
    public final Setting<Boolean> replaceClouds=skySettings.add(new BoolSetting.Builder().name("replace-vanilla-clouds")
        .description("Hide block clouds. Keep this off for the cloud silhouettes in the reference recordings.").defaultValue(false).build());
    private final Setting<ZenithSkybox.Style> skyStyle=skySettings.add(new EnumSetting.Builder<ZenithSkybox.Style>().name("sky-style")
        .description("Reference Dusk matches the red horizon; Reference Storm gives the silver overcast sword scene.").defaultValue(ZenithSkybox.Style.ReferenceDusk).build());
    private final Setting<Double> skyBrightness=skySettings.add(new DoubleSetting.Builder().name("sky-brightness")
        .description("Brightness of the procedural sky, independent of world lighting.").defaultValue(1.0).range(.25,1.5).sliderRange(.25,1.5).build());
    private final Setting<Double> cloudDensity=skySettings.add(new DoubleSetting.Builder().name("cloud-density")
        .description("Strength of the layered cloud and nebula field.").defaultValue(1.2).range(0,2).sliderRange(0,2).build());
    private final Setting<Double> skySpeed=skySettings.add(new DoubleSetting.Builder().name("sky-speed")
        .description("Speed of drifting clouds, aurora curtains and shooting stars.").defaultValue(.8).range(0,2).sliderRange(0,2).build());
    private final Setting<Boolean> auroras=skySettings.add(new BoolSetting.Builder().name("aurora-ribbons").description("Animated translucent light curtains across the sky.").defaultValue(false).build());
    private final Setting<Boolean> meteors=skySettings.add(new BoolSetting.Builder().name("shooting-stars").description("Fading meteor heads and tails across the sky.").defaultValue(false).build());
    private final Setting<Boolean> celestial=skySettings.add(new BoolSetting.Builder().name("celestial-halo").description("A soft celestial orb with an inclined luminous ring.").defaultValue(false).build());
    private final Setting<Double> flashStrength=general.add(new DoubleSetting.Builder().name("explosion-glow")
        .description("Brightness of the explosion's soft white-gold core. Zero removes the flash.").defaultValue(.7).range(0,1).sliderRange(0,1).build());
    public final Setting<Boolean> purpleSky = bool("purple-sky", "Override sky color using the selected sky mood.", true);
    public final Setting<Boolean> starfield = bool("starfield", "Optional seeded stars; reference presets leave them off.", false);
    public final Setting<Boolean> rain = bool("cinematic-rain", "Local rain; does not change server weather.", false);
    private final Setting<Boolean> hits = bool("hit-effects", "Damage-confirmed shock rings and star sparks.", true);
    private final Setting<Boolean> crystals = bool("crystal-visuals", "Golden crystal glow, concentric base rings, and optional orbitals.", true);
    private final Setting<Boolean> explosions = bool("explosion-effects", "Layered expanding rings and particle shards for server explosions.", true);
    private final Setting<Boolean> replaceExplosionSmoke = bool("replace-explosion-smoke", "Replace vanilla explosion clouds with the custom golden glow and shards.", true);
    private final Setting<Boolean> pearls = bool("pearl-visuals", "Fading trails and a corona around thrown pearls.", true);
    private final Setting<Boolean> deaths = bool("death-visuals", "Ascending rings, shards and a light column on entity death.", true);
    private final Setting<Boolean> sword = bool("sword-animation", "Adds a smooth sideways slash to first-person sword swings.", true);
    private final Setting<Boolean> slashes=bool("slash-arcs","Optional crossed sword arcs around the short reference impact rings.",false);
    private final Setting<Boolean> orbitals=bool("crystal-orbitals","Optional tilted energy rings and a spiraling core around crystals.",false);
    private final Setting<Boolean> lightning=bool("blast-lightning","Optional branching filaments around the golden explosion dome.",false);
    private final Setting<Boolean> particleTrails=bool("particle-trails","Soft velocity streaks behind burst particles and pearl ribbons.",true);
    private final Setting<Boolean> deathSpiral=bool("death-spiral","Ascending double helix, expanding rings and a tapered light column.",true);
    private final Setting<Boolean> ambientMotes=bool("ambient-motes","Soft drifting ember lights around the player, with world occlusion.",true);
    private final Setting<Boolean> rainStreaks=bool("rain-streaks","Silver streaks and ground ripples; replaces blue rain columns when cinematic rain is enabled.",true);
    private final Setting<Double> extraStrength=accents.add(new DoubleSetting.Builder().name("extra-effects-strength")
        .description("Brightness of the additional motion layers. Zero disables all extra layers.")
        .defaultValue(1.0).range(0,2).sliderRange(0,2).build());
    private final Setting<Boolean> impactCuts=extra("impact-crescents","Crossed luminous crescents and delayed impact echoes.");
    private final Setting<Boolean> blastComets=extra("blast-comets","Curved fragment trails, star tips and delayed shockwaves.");
    private final Setting<Boolean> crystalStreams=extra("crystal-energy-streams","Rising energy funnels with orbiting comet heads around crystals.");
    private final Setting<Boolean> deathStars=extra("death-starburst","Ascending star shower with a spreading crown of curved trails.");
    private final Setting<Boolean> pearlSpirals=extra("pearl-spirals","Twin spirals around flying pearls and a swirling arrival bloom.");
    private final Setting<Boolean> energyBlooms=extra("energy-blooms","Rotating crystal sigils, blast ripples, pearl portals and death crowns.");
    private final Setting<Boolean> crystalComets=extra("crystal-comet-trails","Counter-rotating comet heads with fading inclined trails around crystals.");
    private final Setting<Boolean> blastShower=extra("blast-spark-shower","Falling embers and small landing ripples after an explosion.");
    private final Setting<Boolean> pearlBloom=extra("pearl-arrival-burst","Curved petal trails and faceted lights when a pearl arrives.");
    private final Setting<Boolean> softenBlasts=extra("soften-overlapping-blasts","Reduce overlapping explosion glare while keeping a single blast at full brightness.");
    private final long[] flourishRendered=new long[3];
    public long flourishRendered(int layer){return flourishRendered[layer];}
    private double burstOpacity=1,explosionOpacity=1;
    public double explosionOpacity(){return explosionOpacity;}
    private int extraPrimitives;
    private final long[] extraRendered=new long[5];
    public int extraPrimitiveCount(){return extraPrimitives;}
    public int extraPrimitiveLimit(){return budget().emission()*8;}
    public long extraRendered(int family){return extraRendered[family];}
    private Setting<Boolean> extra(String name,String description) {
        return accents.add(new BoolSetting.Builder().name(name).description(description).defaultValue(true).build());
    }
    private final Setting<Integer> range = general.add(new IntSetting.Builder().name("render-distance")
        .description("Effects farther away than this distance are discarded.").defaultValue(48).range(8, 96).sliderRange(8, 96).build());
    private final ArrayList<Burst> bursts = new ArrayList<>();
    private final ArrayList<Spark> sparks = new ArrayList<>();
    private final ArrayList<Entity> tracked = new ArrayList<>();
    private final Map<Integer, ArrayDeque<Vec3>> trails = new HashMap<>();
    private final Map<Integer,Integer> trailSeen=new HashMap<>();
    private final Color tint = new Color(), white = new Color(238, 220, 255), clear = new Color(183,117,255,0);
    private final Color soft=new Color(),ribbonEnd=new Color();
    // MeshBuilder copies channels into native memory immediately, so these scratch colors
    // can be reused across primitives without allocating a Color for every quad/streak.
    private final Color ribbonClear=new Color(),spriteColor=new Color();
    private double renderCameraX,renderCameraY,renderCameraZ;
    private int circleSegments;
    private final double[] circleX=new double[65],circleZ=new double[65];
    private int tick;
    private boolean loadingSettings;
    private double cameraRightX, cameraRightZ, cameraUpX, cameraUpY, cameraUpZ;
    private boolean forcedRain;
    private float originalRain, originalThunder;
    private Renderer3D glowRenderer;
    private meteordevelopment.meteorclient.renderer.MeshBuilder lightSprites;
    private int softLightCount,facetedLights,bloomLights;
    public int facetedLightCount(){return facetedLights;}
    public int bloomLightCount(){return bloomLights;}
    public int softLightCount(){return softLightCount;}
    private final Setting<Double> glowSpread=visuals.add(new DoubleSetting.Builder().name("soft-glow-size")
        .description("Smooth fragment-shaded glow around impacts, crystals and particles.")
        .defaultValue(1.65).range(.5,3).sliderRange(.5,3).build());
    private final Setting<Double> glowOpacity=visuals.add(new DoubleSetting.Builder().name("soft-glow-strength")
        .description("Brightness of soft glows; terrain still occludes them.")
        .defaultValue(1.2).range(0,2).sliderRange(0,2).build());
    private ZenithSkybox skybox;
    private boolean outdoors;
    private final double[][] rainGround=new double[16][3];
    private int rainRippleCount;
    private int hitBursts, explosionBursts, deathBursts, pearlSamples, crystalFrames,replacedClouds;
    private int lightningFrames,rainFrames,trailImpacts,nativeRainReplacements;
    public boolean replacesNativeRain(){return isActive()&&rain.get()&&rainStreaks.get();}
    public void onNativeRainReplaced(){nativeRainReplacements++;}
    public int nativeRainReplacements(){return nativeRainReplacements;}
    public record Statistics(int hits,int explosions,int deaths,int pearlSamples,int crystalFrames,int sparks,int replacedClouds,
        int skyFrames,int skyVertices,int skyStars,int meteorFrames,int lightningFrames,int rainFrames,int trailImpacts) {}
    public Statistics statistics() {
        ZenithSkybox.Statistics sky=skybox==null?new ZenithSkybox.Statistics(0,0,0,0):skybox.statistics();
        return new Statistics(hitBursts,explosionBursts,deathBursts,pearlSamples,crystalFrames,sparks.size(),replacedClouds,
            sky.frames(),sky.vertices(),sky.stars(),sky.meteorFrames(),lightningFrames,rainFrames,trailImpacts);
    }
    public VisualBudget budget(){return economyMode.get()?VisualBudget.economy():VisualBudget.level(quality.get().ordinal());}
    private Quality effectiveQuality(){return economyMode.get()?Quality.Low:quality.get();}
    private int effectRange(){return economyMode.get()?Math.min(24,range.get()):range.get();}
    private int trackedLimit(){return economyMode.get()?8:32;}
    private int trailLength(){return economyMode.get()?16:32;}
    public boolean tintsSky(){return isActive()&&skySource.get()==SkySource.Zenith&&purpleSky.get();}
    private boolean hasPanorama(){
        if(skySource.get()!=SkySource.CustomPanorama)return false;
        var id=net.minecraft.resources.Identifier.tryParse(panoramaTexture.get());
        return id!=null&&mc.getResourceManager().getResource(id).isPresent();
    }
    public boolean hasSkybox(){return isActive()&&(hasPanorama()||tintsSky()&&animatedSky.get())&&mc.level!=null&&mc.level.dimensionType().hasSkyLight();}
    public boolean renderSky(com.mojang.blaze3d.vertex.PoseStack matrices,float partialTick) {
        if(!hasSkybox())return false;
        if(skybox==null)skybox=new ZenithSkybox();
        if(skySource.get()==SkySource.CustomPanorama)return skybox.renderPanorama(budget(),panoramaTexture.get(),skyRotation.get());
        skybox.render(matrices,budget(),(tick+partialTick)/20.0*skySpeed.get(),skyStyle.get(),effectiveSkyMood().ordinal(),skyBrightness.get(),cloudDensity.get(),starfield.get(),auroras.get(),meteors.get(),celestial.get());
        return true;
    }

    public ZenithEffects() {
        super(Categories.Render, "zenith-effects", "Short impact flashes, golden crystal domes, pearl ribbons, reference skies and death visuals.");
    }
    /** A preset is applied only by an explicit selector change, never on each frame/tick. */
    private void applyPreset(VisualPreset preset) {
        if (loadingSettings || preset == VisualPreset.Custom || skyStyle == null) return;
        boolean purple = preset == VisualPreset.ZenithPurple;
        boolean storm = preset == VisualPreset.ReferenceSword;
        palette.set(purple ? Palette.ZenithPurple : Palette.VideoReference);
        skyStyle.set(purple ? ZenithSkybox.Style.Nebula : storm ? ZenithSkybox.Style.ReferenceStorm : ZenithSkybox.Style.ReferenceDusk);
        skyMood.set(purple ? SkyMood.Purple : storm ? SkyMood.RainySilver : SkyMood.EmberDusk);
        skySource.set(SkySource.Zenith); animatedSky.set(true); purpleSky.set(true); replaceClouds.set(purple);
        rain.set(storm); starfield.set(purple); auroras.set(purple); meteors.set(purple); celestial.set(false);
        slashes.set(purple); orbitals.set(purple); lightning.set(purple);
        ambientMotes.set(!storm); intensity.set(storm ? 1.15 : 1.5); effectSize.set(1.15);
        flashStrength.set(storm ? .55 : .7); cloudDensity.set(storm ? 1.45 : 1.2); skyBrightness.set(1.0);
    }

    @Override public Module fromTag(net.minecraft.nbt.CompoundTag tag) {
        // Loading saved sliders must not trigger a preset and overwrite later/earlier groups.
        loadingSettings = true;
        try { return super.fromTag(tag); }
        finally { loadingSettings = false; }
    }

    private SkyMood effectiveSkyMood() {
        return switch (skyStyle.get()) {
            case ReferenceDusk -> SkyMood.EmberDusk;
            case ReferenceStorm -> SkyMood.RainySilver;
            default -> skyMood.get();
        };
    }
    public int skyColor(){return switch(effectiveSkyMood()){case EmberDusk->0xff9a2815;case RainySilver->0xff9ba1ae;case Purple->0xff553475;case Aurora->0xff245466;};}
    public org.joml.Vector4f fogTint(org.joml.Vector4f original) {
        int rgb=skyColor();float amount=effectiveSkyMood()==SkyMood.EmberDusk?.32f:effectiveSkyMood()==SkyMood.RainySilver?.38f:.18f;
        return new org.joml.Vector4f(original.x*(1-amount)+(rgb>>16&255)/255f*amount,
            original.y*(1-amount)+(rgb>>8&255)/255f*amount,original.z*(1-amount)+(rgb&255)/255f*amount,original.w);
    }
    public int cloudTint(int original) {
        int rgb=effectiveSkyMood()==SkyMood.EmberDusk?0xff714846:skyColor();double a=.58;
        int r=(int)((original>>16&255)*(1-a)+(rgb>>16&255)*a);
        int g=(int)((original>>8&255)*(1-a)+(rgb>>8&255)*a),b=(int)((original&255)*(1-a)+(rgb&255)*a);
        return (original&0xff000000)|r<<16|g<<8|b;
    }
    private void colorFor(int kind,int alpha) {
        if(palette.get()==Palette.ZenithPurple)tint.set(accent.get());
        else switch(kind){case 0->tint.set(255,226,207,255);case 1->tint.set(255,151,48,255);case 2->tint.set(204,223,255,255);default->tint.set(196,135,255,255);}
        tint.a=Math.max(0,Math.min(255,(int)(alpha*burstOpacity)));
    }

    private Setting<Boolean> bool(String name, String description, boolean value) {
        return visuals.add(new BoolSetting.Builder().name(name).description(description).defaultValue(value).build());
    }

    public void hit(LivingEntity entity) {
        if (hits.get()) burst(entity.position().add(0, entity.getBbHeight() * .55, 0), 0);
    }

    @EventHandler private void onParticle(ParticleEvent event) {
        if (replacesNativeRain() && event.particle.getType()==ParticleTypes.RAIN) event.cancel();
        if(explosions.get()&&replaceExplosionSmoke.get()&&(event.particle.getType()==ParticleTypes.EXPLOSION||event.particle.getType()==ParticleTypes.EXPLOSION_EMITTER)){event.cancel();replacedClouds++;}
    }
    public boolean replaceExplosionAt(Vec3 center) {
        if(!isActive()||!explosions.get()||!replaceExplosionSmoke.get()||mc.player==null||mc.player.distanceToSqr(center)>effectRange()*effectRange())return false;
        replacedClouds++;return true;
    }

    private void burst(Vec3 pos, int type) {
        if (mc.player == null || mc.player.position().distanceToSqr(pos) > effectRange() * effectRange()) return;
        int limit = budget().bursts();
        if (bursts.size() >= limit) bursts.removeFirst();
        bursts.add(new Burst(pos, type));
        switch(type) { case 0 -> hitBursts++; case 1 -> explosionBursts++; case 2 -> deathBursts++; default -> {} }
        int count = (int)(budget().emission()*intensity.get()*(type==1?1.8:type==2?1.2:.65));
        int budget = budget().particles();
        for (int i = 0; i < count && sparks.size() < budget; i++) {
            double angle = Math.random() * Math.TAU, speed = (.04 + Math.random() * .17) * (type == 0 ? 1 : 2)*effectSize.get();
            sparks.add(new Spark(pos, Math.cos(angle) * speed, .04 + Math.random() * (type==2?.3:.21), Math.sin(angle) * speed,type));
        }
    }

    @EventHandler private void onEntityRemoved(EntityRemovedEvent event) {
        if(pearls.get()&&event.entity instanceof ThrownEnderpearl&&trails.containsKey(event.entity.getId())) {
            var trail=trails.get(event.entity.getId());
            if(!trail.isEmpty()){burst(trail.getLast(),3);trailSeen.put(event.entity.getId(),tick);trailImpacts++;}
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
        bursts.removeIf(b -> ++b.age > lifetime(b.type));
        sparks.removeIf(p -> ++p.age > lifetime(p.kind));
        while(sparks.size()>budget().particles())sparks.removeLast();
        while(bursts.size()>budget().bursts())bursts.removeFirst();
        for (Spark p : sparks) {
            p.px=p.x;p.py=p.y;p.pz=p.z;
            p.x+=p.vx;p.y+=p.vy;p.z+=p.vz;p.vy+=p.kind==2?.0012:-.004;p.vx*=.965;p.vz*=.965;
        }
        if(tick%10==0) {
            outdoors=rainStreaks.get()&&(rain.get()||mc.level.getRainLevel(1)>.1)&&mc.level.canSeeSky(mc.player.blockPosition());
            rainRippleCount=outdoors?Math.min(16,budget().motes()/3):0;
            for(int i=0;i<rainRippleCount;i++) {
                int x=(int)Math.floor(mc.player.getX()+Math.sin(i*18.32)*7),z=(int)Math.floor(mc.player.getZ()+Math.cos(i*7.31)*7);
                rainGround[i][0]=x+.5;rainGround[i][2]=z+.5;
                rainGround[i][1]=mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z)+.025;
            }
        }
        updateRain();
        if (tick % 4 == 0) {
            tracked.clear();
            int inspected = 0;
            if(crystals.get()||pearls.get())for (Entity entity : mc.level.entitiesForRendering()) {
                if (++inspected > 2048 || tracked.size() >= trackedLimit()) break;
                if (((crystals.get() && entity instanceof EndCrystal) || (pearls.get() && entity instanceof ThrownEnderpearl))
                    && mc.player.distanceToSqr(entity) < effectRange() * effectRange()) tracked.add(entity);
            }
            trails.keySet().removeIf(id -> tick-trailSeen.getOrDefault(id,0)>18);
            trailSeen.keySet().retainAll(trails.keySet());
        }
        for (Entity entity : tracked) {
            if (entity instanceof ThrownEnderpearl && !entity.isRemoved()) {
                if(!pearls.get()||!trails.containsKey(entity.getId())&&trails.size()>=trackedLimit())continue;
                ArrayDeque<Vec3> trail = trails.computeIfAbsent(entity.getId(), _ -> new ArrayDeque<>());
                while (trail.size() >= trailLength()) trail.removeFirst();
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
    private void clear() { bursts.clear(); sparks.clear(); tracked.clear(); trails.clear();trailSeen.clear(); hitBursts=explosionBursts=deathBursts=pearlSamples=crystalFrames=replacedClouds=lightningFrames=rainFrames=trailImpacts=nativeRainReplacements=0;softLightCount=0;extraPrimitives=0;facetedLights=0;bloomLights=0;culledGroups=visibleCrystals=0;Arrays.fill(extraRendered,0);Arrays.fill(flourishRendered,0);burstOpacity=explosionOpacity=1;if(skybox!=null)skybox.clear(); }
    private static int lifetime(int type){return type==2?44:type==1?26:type==0?10:18;}

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
        Vec3 renderCamera=mc.gameRenderer.mainCamera().position();
        renderCameraX=renderCamera.x;renderCameraY=renderCamera.y;renderCameraZ=renderCamera.z;
        visibility.update(meteordevelopment.meteorclient.utils.render.RenderUtils.projection,
            com.mojang.blaze3d.systems.RenderSystem.getModelViewStack(),event.matrices.last().pose(),renderCameraX,renderCameraY,renderCameraZ);
        culledGroups=visibleCrystals=0;
        double rangeSquared=(double)effectRange()*effectRange();
        double time=(tick+event.tickDelta)/20.0;
        double yaw=Math.toRadians(mc.gameRenderer.mainCamera().yRot()),pitch=Math.toRadians(mc.gameRenderer.mainCamera().xRot());
        cameraRightX=Math.cos(yaw);cameraRightZ=Math.sin(yaw);
        cameraUpX=-Math.sin(yaw)*Math.sin(pitch);cameraUpY=Math.cos(pitch);cameraUpZ=Math.cos(yaw)*Math.sin(pitch);
        VisualBudget budget=budget();
        if(glowRenderer==null)glowRenderer=new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH,MeteorRenderPipelines.ZENITH_GLOW);
        Renderer3D r=glowRenderer;r.begin();
        if(lightSprites==null)lightSprites=new meteordevelopment.meteorclient.renderer.MeshBuilder(MeteorRenderPipelines.ZENITH_SPRITE);
        lightSprites.begin();softLightCount=0;extraPrimitives=0;facetedLights=0;bloomLights=0;
        int overlapping=0;
        for(Burst burst:bursts)if(burst.type==1&&mc.player.distanceToSqr(burst.pos)<=rangeSquared)overlapping++;
        explosionOpacity=softenBlasts.get()?1/Math.sqrt(Math.max(1,overlapping)):1;
        for(Burst burst:bursts)if(mc.player.distanceToSqr(burst.pos)<=rangeSquared) {
            double bound=(burst.type==0?4:burst.type==1?Math.max(7,6*glowSpread.get()):7)*effectSize.get();
            if(!visible(burst.pos.x,burst.pos.y+(burst.type==2?2:0),burst.pos.z,bound))continue;
            burstOpacity=burst.type==1?explosionOpacity:1;
            renderBurst(r,burst,time,event.tickDelta,budget.segments());
        }
        burstOpacity=1;
        for(Entity entity:tracked) {
            if(entity.isRemoved()||mc.player.distanceToSqr(entity)>rangeSquared)continue;
            Vec3 previous=entity.oldPosition(),position=entity.position();
            double x=previous.x+(position.x-previous.x)*event.tickDelta,y=previous.y+(position.y-previous.y)*event.tickDelta,z=previous.z+(position.z-previous.z)*event.tickDelta;
            if(!visible(x,y+1,z,3*effectSize.get()*Math.max(1,glowSpread.get())))continue;
            if(entity instanceof EndCrystal&&crystals.get()){visibleCrystals++;renderCrystal(r,x,y,z,time,entity.getId(),budget.segments());}
            else if(entity instanceof ThrownEnderpearl&&pearls.get()) {
                colorFor(4,210);ring(r,x,y,z,.16,.012,tint,24,true);halo(r,x,y,z,.36,85,4,16,false);
                halo(r,x,y,z,.09,220,4,12,false);
                if(pearlSpirals.get()&&extraStrength.get()>0)energySpirals(r,x,y-.22,z,time*5, .24*effectSize.get(),.44*effectSize.get(),2,4);
            }
        }
        if(pearls.get())for(var entry:trails.entrySet()) {
            ArrayDeque<Vec3> trail=entry.getValue();Vec3 previous=null;int i=0;
            if(trail.isEmpty()||mc.player.distanceToSqr(trail.getLast())>rangeSquared)continue;
            double fade=SkyPattern.fade(tick-trailSeen.getOrDefault(entry.getKey(),tick)+event.tickDelta,18);
            for(Vec3 point:trail) {
                double t=++i/(double)trail.size();colorFor(4,(int)(205*t*fade));
                if(previous!=null) {
                    double radius=previous.distanceTo(point)*.5+.7;
                    if(!visible((previous.x+point.x)*.5,(previous.y+point.y)*.5,(previous.z+point.z)*.5,radius)){previous=point;continue;}
                    soft.set(tint);soft.a/=4;ribbon(r,previous.x,previous.y,previous.z,point.x,point.y,point.z,.085*t,soft,false);
                    white.set(255,246,255,(int)(225*t*fade));
                    ribbon(r,previous.x,previous.y,previous.z,point.x,point.y,point.z,.016*t,white,false);
                    if(particleTrails.get()) {
                        double wave=time*6+i*.64,dx=Math.cos(yaw)*Math.cos(wave)*.065*t,dz=Math.sin(yaw)*Math.cos(wave)*.065*t;
                        particle(r,point.x+dx,point.y+Math.sin(wave)*.065*t,point.z+dz,.025*t,tint);
                        if(i%3==0)particle(r,point.x-dx,point.y-Math.sin(wave)*.065*t,point.z-dz,.024*t,tint);
                    }
                }
                previous=point;
            }
        }
        for(Spark p:sparks) {
            double fade=SkyPattern.fade(p.age+event.tickDelta,lifetime(p.kind)),dt=event.tickDelta;
            double x=p.px+(p.x-p.px)*dt,y=p.py+(p.y-p.py)*dt,z=p.pz+(p.z-p.pz)*dt;
            if(mc.player.distanceToSqr(x,y,z)>rangeSquared||!visible(x,y,z,2*effectSize.get()*Math.max(1,glowSpread.get())))continue;
            colorFor(p.kind,(int)(230*fade*(p.kind==1?explosionOpacity:1)));white.set(p.kind==1?255:230,243,p.kind==1?207:255,tint.a);
            if(p.kind==0)starFlash(r,x,y,z,(.05+.11*fade)*effectSize.get(),tint);
            else particle(r,x,y,z,(.035+.07*fade)*effectSize.get(),tint);
            if(particleTrails.get()) {
                soft.set(tint);soft.a/=3;ribbon(r,x,y,z,x-p.vx*3,y-p.vy*3,z-p.vz*3,.035*fade,soft,true);
                ribbon(r,x,y,z,x-p.vx*2.4,y-p.vy*2.4,z-p.vz*2.4,.008*fade,tint,true);
            }
            if(p.kind==1&&p.age<22) {
                soft.set(tint);soft.a=(int)(soft.a*.7);
                shard(r,x,y,z,.09*fade,time*.8+p.phase,soft);
            }
        }
        atmosphere(r,event,time,budget);
        r.render(event.matrices);
        meteordevelopment.meteorclient.renderer.MeshRenderer.begin().attachments(mc.gameRenderer.mainRenderTarget())
            .pipeline(MeteorRenderPipelines.ZENITH_SPRITE).mesh(lightSprites,event.matrices).end();
    }

    private void renderBurst(Renderer3D r,Burst burst,double time,double delta,int segments) {
        double age=Math.min(1,(burst.age+delta)/lifetime(burst.type)),fade=1-age,size=effectSize.get();
        if ((burst.type==0&&!hits.get())||(burst.type==1&&!explosions.get())
            ||(burst.type==2&&!deaths.get())||(burst.type==3&&!pearls.get()))return;
        Vec3 p=burst.pos;double radius=(.2+(1-Math.pow(fade,3))*(burst.type==0?1.5:4.7))*size;
        colorFor(burst.type,(int)(220*fade));
        if(burst.type==0) {
            // Fast concentric discs stay perpendicular to the CAMERA, including pitch/freecam.
            double wave=(.22+(1-Math.pow(fade,2))*1.15)*size;
            colorFor(0,(int)(210*fade));ring(r,p.x,p.y,p.z,wave,.014+age*.014,tint,segments,true);
            if(age>.09) {
                colorFor(0,(int)(140*fade));ring(r,p.x,p.y,p.z,wave*.73,.011,tint,segments,true);
            }
            halo(r,p.x,p.y,p.z,wave*.8,(int)(85*fade*fade),0,segments,false);
            int rays=effectiveQuality()==Quality.Low?6:10;
            for(int i=0;i<rays;i++) {
                double a=burst.phase+i*Math.TAU/rays,reach=(.18+age*(.65+(i%3)*.18))*size;
                double px=p.x+(cameraRightX*Math.cos(a)+cameraUpX*Math.sin(a))*reach;
                double py=p.y+cameraUpY*Math.sin(a)*reach;
                double pz=p.z+(cameraRightZ*Math.cos(a)+cameraUpZ*Math.sin(a))*reach;
                colorFor(0,(int)(225*fade));starFlash(r,px,py,pz,(.10+.08*(i%2))*fade*size,tint);
            }
            if(slashes.get()) {
                colorFor(0,(int)(190*fade));
                orbital(r,p.x,p.y,p.z,(.5+age*1.3)*size,.025*fade,1.1,burst.phase,age*.8,Math.PI*1.4+age*.8,tint,segments);
            }
        } else if(burst.type==1) {
            double centerY=p.y+1.05*size;
            double domeRadius=(.35+(1-Math.pow(fade,3))*2.6)*size;
            // The video blast has a luminous rising hemisphere and upright golden rings.
            dome(r,p.x,p.y+.035,p.z,domeRadius,domeRadius*1.18,(int)(24*fade*flashStrength.get()),segments);
            for(int layer=0;layer<3;layer++) {
                double t=age-layer*.055;if(t<0)continue;
                double wave=(.25+(1-Math.pow(1-t,3))*(2.45-layer*.22))*size;
                colorFor(1,(int)((210-layer*26)*(1-t)));
                ring(r,p.x,centerY,p.z,wave,.022+t*.02,tint,segments,true);
            }
            colorFor(1,(int)(190*fade));ring(r,p.x,p.y+.035,p.z,radius,.035+age*.025,tint,segments,false);
            colorFor(1,(int)(115*fade));ring(r,p.x,p.y+.065,p.z,radius*.72,.022,tint,segments,false);
            halo(r,p.x,centerY,p.z,(.85+age*1.55)*size,(int)(175*fade*fade*flashStrength.get()),1,segments,false);
            halo(r,p.x,p.y+.055,p.z,radius*1.13,(int)(115*fade*flashStrength.get()),1,segments,true);
            halo(r,p.x,centerY,p.z,.36*size,(int)(240*fade*fade*flashStrength.get()),1,16,false);
            if(age<.65) {
                colorFor(1,(int)(170*(1-age/.65)));
                int rays=effectiveQuality()==Quality.Low?8:16;
                for(int i=0;i<rays;i++) {
                    double a=burst.phase+i*Math.TAU/rays,extent=(.7+age*4+(i%3)*.2)*size;
                    ribbon(r,p.x+Math.cos(a)*.4,centerY,p.z+Math.sin(a)*.4,
                        p.x+Math.cos(a)*extent,centerY+Math.sin(i*4.7)*extent*.4,p.z+Math.sin(a)*extent,.022*fade,tint,true);
                }
                if(lightning.get()){filaments(r,p,burst.phase,time,radius,fade);lightningFrames++;}
            }
        } else if(burst.type==2) {
            for(int i=0;i<3;i++) {
                colorFor(2,(int)(170*fade));ring(r,p.x,p.y+age*3+i*.45,p.z,(.55+age*(1.2+i*.35))*size,.018+age*.02,tint,segments,false);
            }
            if(deathSpiral.get()) {
                colorFor(2,(int)(150*fade));ribbon(r,p.x,p.y,p.z,p.x,p.y+4.7*size,p.z,.28*fade,tint,true);
                halo(r,p.x,p.y+.05,p.z,1.3*size,(int)(95*fade),2,segments,true);
                for(int strand=0;strand<2;strand++)for(int i=0;i<24;i++) {
                    double a=i*.29+time*3+strand*Math.PI,b=(i+1)*.29+time*3+strand*Math.PI;
                    double y=i/24.0*3.9*size+age*.65,radiusA=(.28+i/24.0*.45)*size;
                    colorFor(2,(int)(155*fade*(1-i/29.0)));
                    ribbon(r,p.x+Math.cos(a)*radiusA,p.y+y,p.z+Math.sin(a)*radiusA,
                        p.x+Math.cos(b)*radiusA,p.y+y+.163*size,p.z+Math.sin(b)*radiusA,.022,tint,false);
                    if(i%6==0)particle(r,p.x+Math.cos(a)*radiusA,p.y+y,p.z+Math.sin(a)*radiusA,.085*fade,tint);
                }
            }
        } else {
            colorFor(4,(int)(210*fade));ring(r,p.x,p.y+.04,p.z,(.3+age*1.9)*size,.025,tint,segments,false);
            ring(r,p.x,p.y+.4,p.z,(.25+age*.8)*size,.022,tint,segments,true);
            halo(r,p.x,p.y+.2,p.z,.7*size,(int)(95*fade*fade),4,segments,false);
        }
        renderExtraBurst(r,burst,age,fade,size);
        if(extraStrength.get()>0&&extraPrimitives<extraPrimitiveLimit()) {
            if(burst.type==1&&blastShower.get())renderSparkShower(r,burst,age,fade,size);
            else if(burst.type==3&&pearlBloom.get())renderArrivalBloom(r,burst,age,fade,size);
        }
    }

    private void renderCrystal(Renderer3D r,double x,double baseY,double z,double time,int id,int segments) {
        crystalFrames++;double size=effectSize.get(),y=baseY+1+Math.sin(time*2+id)*.18;
        if(crystalStreams.get()&&extraStrength.get()>0) {
            energySpirals(r,x,baseY+.08,z,time*2.3+id,.93*size,1.7*size,effectiveQuality()==Quality.Low?2:3,2);
            if(energyBlooms.get())extraLight(2,1,x,baseY+.06,z,1.4*size,95,4,true,time*.38+id);
        }
        if(crystalComets.get()&&extraStrength.get()>0)renderCrystalComets(r,x,y,z,time,id,size);
        halo(r,x,baseY+.025,z,1.2*size,75,1,segments,true);colorFor(1,170);
        ring(r,x,baseY+.04,z,.94*size,.024,tint,segments,false);
        ring(r,x,baseY+.045,z,.71*size,.014,tint,segments,false);
        ring(r,x,y,z,.62*size,.012,tint,segments,true);
        halo(r,x,y,z,.67*size,45,1,segments,false);
        halo(r,x,y,z,.22*size,105,1,16,false);
        int count=effectiveQuality()==Quality.Low?6:12;
        for(int i=0;i<count;i++) {
            double a=time*1.2+i*Math.TAU/count,b=a+.35;
            colorFor(1,180);ribbon(r,x+Math.cos(a)*.76*size,baseY+.05,z+Math.sin(a)*.76*size,x+Math.cos(b)*.91*size,baseY+.05,z+Math.sin(b)*.91*size,.018,tint,false);
            if(orbitals.get())starFlash(r,x+Math.cos(a)*.86*size,y+Math.sin(time*1.7+i)*.55,z+Math.sin(a)*.86*size,.075,tint);
        }
        if(orbitals.get()) {
            colorFor(1,155);orbital(r,x,y,z,.78*size,.015,1.05,time*.45+id,0,Math.TAU,tint,segments);
            orbital(r,x,y,z,.84*size,.011,-.85,-time*.32,0,Math.TAU,tint,segments);
            for(int i=0;i<20;i++) {
                double a=time*2.4+i*.55,b=a+.55,py=baseY+i*.075;
                colorFor(1,(int)(85*Math.sin((i+1)*Math.PI/22)));
                ribbon(r,x+Math.cos(a)*.21,py,z+Math.sin(a)*.21,x+Math.cos(b)*.21,py+.075,z+Math.sin(b)*.21,.018,tint,false);
            }
            colorFor(1,55);ribbon(r,x,baseY+.05,z,x,y+.7,z,.035,tint,true);
        }
    }

    // Each added ribbon or sprite consumes one shared per-frame allowance. No additional
    // particles, entity scans, render passes or persistent trail histories are allocated.
    private boolean extraSlot(int family) {
        if(extraStrength.get()<=0||extraPrimitives>=extraPrimitiveLimit())return false;
        extraPrimitives++;extraRendered[family]++;return true;
    }
    private void extraLine(Renderer3D r,int family,int kind,double ax,double ay,double az,
                           double bx,double by,double bz,double width,double alpha) {
        if(alpha<=0||!extraSlot(family))return;
        colorFor(kind,(int)Math.min(255,alpha*extraStrength.get()));
        ribbon(r,ax,ay,az,bx,by,bz,width,tint,false);
    }
    private void extraLight(int family,int kind,double x,double y,double z,double size,double alpha,int shape,boolean ground) {
        extraLight(family,kind,x,y,z,size,alpha,shape,ground,0);
    }
    private void extraLight(int family,int kind,double x,double y,double z,double size,double alpha,int shape,boolean ground,double rotation) {
        if(alpha<=0||!extraSlot(family))return;
        colorFor(kind,(int)Math.min(255,alpha*extraStrength.get()));
        softSprite(x,y,z,size,tint,ground,shape,rotation);
    }

    /** Tapered, rising corkscrews: crystal energy is world anchored, pearl energy follows the projectile. */
    private void energySpirals(Renderer3D r,double x,double y,double z,double phase,double radius,double height,int strands,int family) {
        int steps=effectiveQuality()==Quality.Low?8:effectiveQuality()==Quality.High?18:12;
        int kind=family==2?1:4;
        for(int strand=0;strand<strands&&extraPrimitives<extraPrimitiveLimit();strand++) {
            double px=0,py=0,pz=0;
            for(int i=0;i<=steps;i++) {
                double t=i/(double)steps,a=phase+strand*Math.TAU/strands+t*Math.PI*1.65;
                double reach=radius*(1-.65*t);
                double nx=x+Math.cos(a)*reach,ny=y+t*height,nz=z+Math.sin(a)*reach;
                if(i>0)extraLine(r,family,kind,px,py,pz,nx,ny,nz,.012+.012*t,25+110*t);
                px=nx;py=ny;pz=nz;
            }
            extraLight(family,kind,px,py,pz,.15*effectSize.get(),175,1,false);
        }
    }

    private void renderExtraBurst(Renderer3D r,Burst b,double age,double fade,double size) {
        if(extraStrength.get()<=0||extraPrimitives>=extraPrimitiveLimit())return;
        Vec3 p=b.pos;
        if(b.type==0&&impactCuts.get()) {
            int steps=effectiveQuality()==Quality.Low?10:18;
            for(int cut=0;cut<2;cut++) {
                double tilt=b.phase+cut*Math.PI*.5+age*.5,px=0,py=0,pz=0;
                double ct=Math.cos(tilt),st=Math.sin(tilt);
                for(int i=0;i<=steps;i++) {
                    double t=i/(double)steps,a=(t-.5)*Math.PI*1.5;
                    double radius=(.45+age*1.7)*size;
                    double ellipseX=Math.cos(a)*radius,ellipseY=Math.sin(a)*radius*.4;
                    double u=ellipseX*ct-ellipseY*st,v=ellipseX*st+ellipseY*ct;
                    double nx=p.x+cameraRightX*u+cameraUpX*v,ny=p.y+cameraUpY*v,nz=p.z+cameraRightZ*u+cameraUpZ*v;
                    if(i>0)extraLine(r,0,0,px,py,pz,nx,ny,nz,.055*size*Math.sin(t*Math.PI),185*fade*Math.sin(t*Math.PI));
                    px=nx;py=ny;pz=nz;
                }
            }
            double echo=(age-.25)/.75;
            if(echo>0)extraLight(0,0,p.x,p.y,p.z,(.25+echo*1.7)*size*1.2,100*(1-echo),2,false);
        } else if((b.type==1&&blastComets.get())||(b.type==2&&deathStars.get())) {
            boolean death=b.type==2;int family=death?3:1,kind=death?2:1;
            int count=effectiveQuality()==Quality.Low?5:effectiveQuality()==Quality.High?14:9;
            int steps=effectiveQuality()==Quality.Low?5:9;
            for(int ray=0;ray<count&&extraPrimitives<extraPrimitiveLimit();ray++) {
                double px=0,py=0,pz=0;
                for(int j=0;j<=steps;j++) {
                    double t=Math.max(0,age-.24)+Math.min(.24,age)*j/steps;
                    double a=b.phase+ray*Math.TAU/count+t*(death?1.6:.8);
                    double reach=(death?(.25+t*t*2.2):(.2+t*(3.2+ray%3*.5)))*size;
                    double lift=death?(.2+4.8*t):(.45+(2.8+ray%3*.6)*t-2.5*t*t);
                    double nx=p.x+Math.cos(a)*reach,ny=p.y+lift*size,nz=p.z+Math.sin(a)*reach;
                    if(j>0)extraLine(r,family,kind,px,py,pz,nx,ny,nz,(.012+.022*j/steps)*size,175*fade*j/steps);
                    px=nx;py=ny;pz=nz;
                }
                extraLight(family,kind,px,py,pz,(.13+.06*Math.sin(ray*4.7+age*12))*size,215*fade,1,false);
            }
            // The echo begins after the core flash, keeping the impact readable as it fades.
            double echo=(age-.22)/.78;
            if(echo>0)extraLight(family,kind,p.x,p.y+.075,p.z,(.35+echo*(death?2.3:5.4))*size*1.2,140*(1-echo),2,true);
            if(energyBlooms.get()) {
                extraLight(family,kind,p.x,p.y+.095,p.z,(.5+age*(death?2.4:4.8))*size,125*fade,4,true,b.phase+age*(death?1.3:-.7));
                if(death)extraLight(family,kind,p.x,p.y+(1+age*4)*size,p.z,(.2+Math.sin(age*Math.PI)*.6)*size,200*fade,1,false);
            }
        } else if(b.type==3&&pearlSpirals.get()) {
            if(energyBlooms.get())extraLight(4,4,p.x,p.y+.065,p.z,(.4+age*1.9)*size,180*fade,4,true,b.phase-age*2);
            int steps=effectiveQuality()==Quality.Low?10:20;
            for(int strand=0;strand<2;strand++) {
                double px=0,py=0,pz=0;
                for(int i=0;i<=steps;i++) {
                    double t=i/(double)steps,a=b.phase+strand*Math.PI+t*Math.TAU+age*3;
                    double reach=(.12+t*(.4+age*1.6))*size;
                    double nx=p.x+Math.cos(a)*reach,ny=p.y+.15+t*.8*fade*size,nz=p.z+Math.sin(a)*reach;
                    if(i>0)extraLine(r,4,4,px,py,pz,nx,ny,nz,.024*size,160*fade*t);
                    px=nx;py=ny;pz=nz;
                }
                extraLight(4,4,px,py,pz,.18*size,210*fade,1,false);
            }
        }
    }

    /** New motion layers share the same primitive allowance as the existing extra effects. */
    private void renderCrystalComets(Renderer3D r,double x,double y,double z,double time,int id,double size) {
        int before=extraPrimitives,steps=effectiveQuality()==Quality.Low?6:12;
        int count=effectiveQuality()==Quality.Low?1:2;
        for(int strand=0;strand<count&&extraPrimitives<extraPrimitiveLimit();strand++) {
            double tilt=strand==0?.8:-.65,heading=id*.31+time*.16;
            double ct=Math.cos(tilt),st=Math.sin(tilt),ch=Math.cos(heading),sh=Math.sin(heading);
            double px=0,py=0,pz=0;
            for(int i=0;i<=steps;i++) {
                double t=i/(double)steps,a=id+time*(strand==0?1.9:-1.5)-(1-t)*1.65;
                double hx=Math.cos(a)*1.08*size,hz=Math.sin(a)*ct*1.08*size;
                double nx=x+hx*ch-hz*sh,ny=y+Math.sin(a)*st*1.08*size,nz=z+hx*sh+hz*ch;
                if(i>0)extraLine(r,2,1,px,py,pz,nx,ny,nz,(.007+t*.018)*size,175*t*t);
                px=nx;py=ny;pz=nz;
            }
            extraLight(2,1,px,py,pz,.13*size,200,1,false);
        }
        flourishRendered[0]+=extraPrimitives-before;
    }

    private void renderSparkShower(Renderer3D r,Burst b,double age,double fade,double size) {
        if(age<.23)return;
        int before=extraPrimitives,count=effectiveQuality()==Quality.Low?6:effectiveQuality()==Quality.High?22:14;
        Vec3 p=b.pos;
        for(int i=0;i<count&&extraPrimitives<extraPrimitiveLimit();i++) {
            double t=(age-.23-(i%4)*.035)/.665;
            if(t<=0||t>=1)continue;
            double a=b.phase+i*2.399963,reach=(.6+Math.sqrt((i+.5)/count)*2.4+t*.35)*size;
            double height=2.4+Math.sin(i*4.7)*.65-4.1*t*t;
            double x=p.x+Math.cos(a)*reach,z=p.z+Math.sin(a)*reach,y=p.y+Math.max(.065,height)*size;
            double alpha=175*Math.sin(t*Math.PI)*Math.sqrt(fade);
            if(height>.065) {
                extraLine(r,1,1,x,y,z,x-Math.cos(a)*.05*size,y+(.08+t*.35)*size,z-Math.sin(a)*.05*size,.012*size,alpha*.65);
                extraLight(1,1,x,y,z,(.065+(i%3)*.014)*size,alpha,1,false);
            } else {
                extraLight(1,1,x,y,z,(.10+(t-.7)*.6)*size*1.2,alpha*.5,2,true);
            }
        }
        flourishRendered[1]+=extraPrimitives-before;
    }

    private void renderArrivalBloom(Renderer3D r,Burst b,double age,double fade,double size) {
        int before=extraPrimitives,count=effectiveQuality()==Quality.Low?5:8,steps=effectiveQuality()==Quality.Low?3:5;
        Vec3 p=b.pos;
        for(int petal=0;petal<count&&extraPrimitives<extraPrimitiveLimit();petal++) {
            double px=0,py=0,pz=0;
            for(int i=0;i<=steps;i++) {
                double t=Math.max(0,age-.22)+Math.min(.22,age)*i/steps;
                double a=b.phase+petal*Math.TAU/count+t*1.25,reach=(.12+(1-Math.pow(1-t,3))*1.8)*size;
                double nx=p.x+Math.cos(a)*reach,ny=p.y+(.15+Math.sin(t*Math.PI)*.8)*size,nz=p.z+Math.sin(a)*reach;
                if(i>0)extraLine(r,4,4,px,py,pz,nx,ny,nz,.022*size,165*fade*i/steps);
                px=nx;py=ny;pz=nz;
            }
            extraLight(4,4,px,py,pz,.17*size,190*fade,3,false,b.phase+petal+age*2);
        }
        flourishRendered[2]+=extraPrimitives-before;
    }

    private void filaments(Renderer3D r,Vec3 p,double seed,double time,double radius,double fade) {
        int arms=effectiveQuality()==Quality.Low?3:effectiveQuality()==Quality.High?8:5;
        for(int arm=0;arm<arms;arm++) {
            double a=seed+arm*Math.TAU/arms,px=p.x,py=p.y+.5,pz=p.z;
            for(int step=1;step<=6;step++) {
                double distance=radius*step/6,jitter=Math.sin(seed*11+step*12.43+arm*7+Math.floor(time*14))*.23;
                double nx=p.x+Math.cos(a)*distance-Math.sin(a)*jitter,ny=p.y+.5+Math.sin(step*1.6+arm)*.23+distance*.13,nz=p.z+Math.sin(a)*distance+Math.cos(a)*jitter;
                colorFor(1,(int)(170*fade));ribbon(r,px,py,pz,nx,ny,nz,.012*fade,tint,false);
                px=nx;py=ny;pz=nz;
            }
        }
    }

    private void atmosphere(Renderer3D r,Render3DEvent event,double time,VisualBudget budget) {
        if(ambientMotes.get())for(int i=0;i<budget.motes();i++) {
            double x=event.offsetX+Math.sin(i*17.31)*7+Math.sin(time*.2+i)*.4,z=event.offsetZ+Math.cos(i*9.72)*7;
            double y=event.offsetY-1+(time*.15+i*.618)%4.5;
            if(!visible(x,y,z,.5*glowSpread.get()))continue;
            int kind=palette.get()==Palette.ZenithPurple?4:1;
            double pulse=.65+.35*Math.sin(time*.9+i),radius=.10+(i%5)*.025;
            halo(r,x,y,z,radius,(int)(45*pulse),kind,12,false);
            halo(r,x,y,z,radius*.35,(int)(160*pulse),kind,8,false);
            if(i%5==0){colorFor(kind,(int)(95*pulse));ribbon(r,x,y,z,x-.035,y-.16,z,.012,tint,true);}
        }
        if(rainStreaks.get()&&outdoors&&mc.level.getRainLevel(1)>.1) {
            double amount=mc.level.getRainLevel(1);
            for(int i=0;i<budget.motes()*4;i++) {
                double x=event.offsetX+Math.sin(i*12.92)*8,z=event.offsetZ+Math.cos(i*7.32)*8;
                double y=event.offsetY+5-((time*1.3+i*.618)%1)*11;
                if(!visible(x,y-.475,z,1.1))continue;
                tint.set(224,231,240,(int)(105*amount));ribbon(r,x,y,z,x-.06,y-.95,z+.025,.008,tint,true);
            }
            for(int i=0;i<rainRippleCount;i++) {
                double phase=(time*.8+i*.618)%1;tint.set(192,211,255,(int)(50*amount*(1-phase)));
                double[] p=rainGround[i];if(!visible(p[0],p[1],p[2],.5))continue;ring(r,p[0],p[1],p[2],.05+phase*.4,.008,tint,16,false);
            }
            rainFrames++;
        }
    }

    /** Camera-facing soft line, also used for velocity streaks and pearl cores. */
    private void ribbon(Renderer3D r,double ax,double ay,double az,double bx,double by,double bz,double width,Color color,boolean fade) {
        if(width<=0||color.a<=0)return;
        double dx=bx-ax,dy=by-ay,dz=bz-az,cx=renderCameraX-(ax+bx)*.5,cy=renderCameraY-(ay+by)*.5,cz=renderCameraZ-(az+bz)*.5;
        double nx=dy*cz-dz*cy,ny=dz*cx-dx*cz,nz=dx*cy-dy*cx,length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(length<.000001)return;nx*=width/length;ny*=width/length;nz*=width/length;
        ribbonEnd.set(color);if(fade)ribbonEnd.a=0;
        Color transparent=ribbonClear.set(color.r,color.g,color.b,0);
        // Across the streak: transparent edge -> luminous center -> transparent edge.
        r.quad(ax-nx*2.5,ay-ny*2.5,az-nz*2.5,bx-nx*2.5,by-ny*2.5,bz-nz*2.5,
            bx,by,bz,ax,ay,az,transparent,ribbonEnd,color,transparent);
        r.quad(ax,ay,az,bx,by,bz,bx+nx*2.5,by+ny*2.5,bz+nz*2.5,
            ax+nx*2.5,ay+ny*2.5,az+nz*2.5,ribbonEnd,transparent,transparent,color);
    }

    private void orbital(Renderer3D r,double x,double y,double z,double radius,double width,double tilt,double yaw,double start,double end,Color color,int segments) {
        double px=0,py=0,pz=0;int count=Math.max(4,(int)(segments*(end-start)/Math.TAU));
        double ct=Math.cos(tilt),st=Math.sin(tilt),cy=Math.cos(yaw),sy=Math.sin(yaw);
        for(int i=0;i<=count;i++) {
            double a=start+(end-start)*i/count,sa=Math.sin(a),hx=Math.cos(a)*radius,hz=sa*ct*radius;
            double nx=x+hx*cy-hz*sy,ny=y+sa*st*radius,nz=z+hx*sy+hz*cy;
            if(i>0){soft.set(color);soft.a/=4;ribbon(r,px,py,pz,nx,ny,nz,width*3,soft,false);ribbon(r,px,py,pz,nx,ny,nz,width,color,false);}
            px=nx;py=ny;pz=nz;
        }
    }

    /** Fragment-shaded annulus with a white core and smooth halo; one quad per ring. */
    private void ring(Renderer3D r, double x, double y, double z, double radius, double width, Color color, int segments, boolean vertical) {
        // Analytic ring profile: smooth at any distance without polygon edges or segment seams.
        softSprite(x,y,z,radius*1.2,color,!vertical,2);
    }

    /** Four slim tapered rays and a compact glow form the recording's moving star glints. */
    private void starFlash(Renderer3D r,double x,double y,double z,double size,Color color) {
        softSprite(x,y,z,size*2,color,false,1);
        soft.set(color);soft.a=color.a/3;
        particle(r,x,y,z,size*.65,soft);
        for(int i=0;i<4;i++) {
            double a=i*Math.PI/2,dx=(cameraRightX*Math.cos(a)+cameraUpX*Math.sin(a))*size;
            double dy=cameraUpY*Math.sin(a)*size,dz=(cameraRightZ*Math.cos(a)+cameraUpZ*Math.sin(a))*size;
            ribbon(r,x,y,z,x+dx,y+dy,z+dz,size*.08,color,true);
        }
    }

    private void particle(Renderer3D r,double x,double y,double z,double size,Color color) {
        softSprite(x,y,z,size*2.4,color,false,0);
    }

    /** One batched quad per light. Radial falloff and star rays are evaluated per fragment. */
    private void softSprite(double x,double y,double z,double size,Color color,boolean ground,int shape) {
        softSprite(x,y,z,size,color,ground,shape,0);
    }
    private void softSprite(double x,double y,double z,double size,Color color,boolean ground,int shape,double rotation) {
        if(lightSprites==null||!lightSprites.isBuilding()||size<=0||color.a<=0)return;
        if(shape<2)size*=glowSpread.get();
        var mesh=lightSprites;mesh.ensureQuadCapacity();
        Color c=spriteColor.set(color.r,color.g,color.b,(int)Math.min(255,color.a*glowOpacity.get()));
        int start=-1;double cos=Math.cos(rotation),sin=Math.sin(rotation);
        for(int i=0;i<4;i++) {
            double u=i==0||i==1?0:1,v=i==0||i==3?0:1;
            double localX=(u*2-1)*size,localY=(v*2-1)*size;
            double rx=localX*cos-localY*sin,ry=localX*sin+localY*cos;
            double dx=ground?rx:cameraRightX*rx+cameraUpX*ry;
            double dy=ground?0:cameraUpY*ry,dz=ground?ry:cameraRightZ*rx+cameraUpZ*ry;
            int vertex=mesh.vec3(x+dx,y+dy,z+dz).vec2(u+shape*2,v).color(c).next();
            if(start<0)start=vertex;
        }
        mesh.quad(start,start+1,start+2,start+3);softLightCount++;
        if(shape==3)facetedLights++;else if(shape==4)bloomLights++;
    }

    /** A low-alpha hemispherical shell gives crystal blasts volume without a fullscreen pass. */
    private void dome(Renderer3D r,double x,double y,double z,double radius,double height,int alpha,int segments) {
        if(alpha<=0)return;
        if(circleSegments!=segments) {
            for(int i=0;i<=segments;i++){double a=i*Math.TAU/segments;circleX[i]=Math.cos(a);circleZ[i]=Math.sin(a);}
            circleSegments=segments;
        }
        int layers=effectiveQuality()==Quality.Low?3:5;
        for(int row=0;row<layers;row++) {
            double low=row*Math.PI/(layers*2),high=(row+1)*Math.PI/(layers*2);
            double lowerRadius=Math.cos(low)*radius,upperRadius=Math.cos(high)*radius;
            double lowerY=y+Math.sin(low)*height,upperY=y+Math.sin(high)*height;
            colorFor(1,alpha);soft.set(tint);soft.a=(int)(alpha*(.45+.55*Math.sin(high)));
            for(int i=0;i<segments;i++) {
                r.quad(x+circleX[i]*lowerRadius,lowerY,z+circleZ[i]*lowerRadius,
                    x+circleX[i+1]*lowerRadius,lowerY,z+circleZ[i+1]*lowerRadius,
                    x+circleX[i+1]*upperRadius,upperY,z+circleZ[i+1]*upperRadius,
                    x+circleX[i]*upperRadius,upperY,z+circleZ[i]*upperRadius,tint,soft,soft,tint);
            }
        }
    }
    private void halo(Renderer3D r,double x,double y,double z,double radius,int alpha,int kind,int segments,boolean horizontal) {
        colorFor(kind,alpha);
        softSprite(x,y,z,radius,tint,horizontal,0);
    }

    private void shard(Renderer3D r,double x,double y,double z,double size,double angle,Color color) {
        // Analytic facets retain a bright edge and soft halo without an opaque diamond quad.
        softSprite(x,y,z,size*1.65,color,false,3,angle);
    }

    private static final class Burst {
        final Vec3 pos; final int type; int age;final double phase=Math.random()*Math.TAU;
        Burst(Vec3 pos, int type) { this.pos=pos; this.type=type; }
    }
    private static final class Spark {
        double x,y,z,px,py,pz,vx,vy,vz; int age;final int kind;final double phase=Math.random()*Math.TAU;
        Spark(Vec3 p,double vx,double vy,double vz,int kind) { x=px=p.x; y=py=p.y; z=pz=p.z; this.vx=vx; this.vy=vy; this.vz=vz;this.kind=kind; }
    }
}
