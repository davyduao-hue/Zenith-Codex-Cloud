package meteordevelopment.meteorclient.zenith.smoke;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.zenith.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.*;
import meteordevelopment.meteorclient.zenith.addons.*;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Only loaded by runSmokeClient, in a separate game directory and a uniquely named throwaway world. */
public final class SmokeAddon extends MeteorAddon {
    private int stage, ticks;
    private final List<String> checks=new ArrayList<>();
    private boolean started, finished;
    private long attackBefore;
    private int explosionBefore;
    private int skyBefore;
    private float rainBefore,thunderBefore;
    private FakePlayerEntity radarPlayer;
    private boolean externalAddon;
    private int oldWidth,oldHeight;
    private final Path results=Path.of("../build/smoke");
    @Override public String getPackage(){return "meteordevelopment.meteorclient.zenith.smoke";}
    @Override public void onInitialize(){MeteorClient.EVENT_BUS.subscribe(this);}

    @EventHandler private void tick(TickEvent.Post event) {
        if(finished)return;
        try {
            if(!started) {
                if(!(mc.gui.screen() instanceof ZenithMenuScreen)||mc.gui.overlay()!=null)return;
                started=true;
                GuiThemes.select("Zenith");GuiThemes.get().settings.reset();
                Hud.get().resetToDefaultElements();
                Files.createDirectories(results);
                Files.deleteIfExists(results.resolve("result.json"));
                for(String name:List.of("crystal-aura","kill-aura","aimbot","aim-assist","esp","block-esp","stash-finder","auto-totem","small-totem","zenith-effects","zenith-crystal-aura","zenith-kill-aura"))
                    require(Modules.get().get(name)!=null,"module registered: "+name);
                require(Modules.get().get("zenith-crystal-aura").addon.name.equals("Zenith Addons"),"custom combat modules have Zenith Addons identity");
                require(Hud.get().infos.keySet().stream().filter(n->n.startsWith("zenith-")).count()==8,"eight editable Zenith widgets registered");
                for(String name:List.of("zenith-crystal-aura","zenith-kill-aura")){Modules.get().get(name).disable();Modules.get().get(name).settings.reset();}
                Modules.get().get(ZenithEffects.class).settings.reset();Modules.get().get(ZenithEffects.class).enable();
                require(java.util.Arrays.asList(GuiThemes.getNames()).containsAll(List.of("Meteor","Zenith")),"native Meteor and custom Zenith themes are available");
                externalAddon=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("zenith-example-addon");
                if(externalAddon) {
                    var example=Modules.get().get("example-counter");
                    require(example!=null,"separate Fabric addon registered its module");
                    require(example.addon.name.equals("Zenith Example Addon"),"external addon owns its registered module");
                    require(example.category.name.equals("Example Addon"),"external addon category registered");
                }
            }
            ticks++;
            switch(stage) {
                case 0 -> { if(ticks==25) { shot("01-menu");next(); } }
                case 1 -> {
                    if(ticks==1)mc.gui.setScreen(GuiThemes.get().modulesScreen());
                    if(ticks==30){shot("02-modules");require(mc.gui.screen() instanceof ZenithGuiScreen,"custom animated GUI opened");require(GuiThemes.get().name.equals("Zenith"),"Zenith theme selected");}
                    if(ticks==32) {
                        var aura=Modules.get().get(ZenithCrystalAura.class);
                        click(760,170,0);require(aura.settings.get("zenith-profile").get().toString().equals("Fast"),"custom enum selector changed profile");
                        click(850,215,0);require(aura.settings.get("pause-in-menus").get().equals(false),"custom toggle changed setting");
                        click(800,284,0);require((Integer)aura.settings.get("minimum-fps").get()>80,"custom slider changed setting");
                        aura.settings.get("zenith-profile").reset();aura.settings.get("pause-in-menus").reset();aura.settings.get("minimum-fps").reset();
                        mc.gui.screen().keyPressed(new KeyEvent(70,0,2));
                        for(char c:"zenith effects".toCharArray())mc.gui.screen().charTyped(new CharacterEvent(c));
                        require(((ZenithGuiScreen)mc.gui.screen()).selectedModuleName().equals("zenith-effects"),"live module search selected matching result");
                    }
                    if(ticks==36){click(260,150,0);require(!Modules.get().get(ZenithEffects.class).isActive(),"custom card toggles module off");}
                    if(ticks==38){click(260,150,0);require(Modules.get().get(ZenithEffects.class).isActive(),"custom card toggles module on");}
                    if(ticks==40){click(80,420,0);require(!(mc.gui.screen() instanceof ZenithGuiScreen),"Interface style button opens native GUI theme settings");}
                    if(ticks==45){
                        shot("13-interface-style");oldWidth=mc.getWindow().getWidth();oldHeight=mc.getWindow().getHeight();mc.getWindow().setWindowed(1600,900);
                        GuiThemes.select("Meteor");GuiThemes.get().settings.reset();GuiThemes.get().clearWindowConfigs();Tabs.get().getFirst().openScreen(GuiThemes.get());
                    }
                    if(ticks==60) {
                        require(mc.gui.screen() instanceof ModulesScreen,"Meteor mode uses original category-window screen");
                        var meteor=(MeteorGuiTheme)GuiThemes.get();
                        require(meteor.name.equals("Meteor")&&meteor.scale.get()==1,"Meteor uses original name and default scale");
                        require(meteor.accentColor.get().r==145&&meteor.accentColor.get().g==61&&meteor.accentColor.get().b==226&&meteor.accentColor.get().a==255,"Meteor uses upstream accent color");
                        require(meteor.backgroundColor.get().r==20&&meteor.backgroundColor.get().a==200,"Meteor uses upstream background color");
                        GuiThemes.save();
                        var saved=net.minecraft.nbt.NbtIo.read(new java.io.File(MeteorClient.FOLDER,"gui/gui.nbt").toPath());
                        require(saved!=null&&saved.getStringOr("currentTheme","").equals("Meteor"),"Meteor interface selection persisted to disk");
                        shot("14-meteor-gui");
                        if(externalAddon) {
                            var example=Modules.get().get("example-counter");
                            example.settings.get("counter-step").parse("3");example.enable();mc.gui.setScreen(meteor.moduleScreen(example));
                        }
                    }
                    if(ticks==68&&externalAddon){require(mc.gui.screen() instanceof ModuleScreen,"external addon opens original Meteor settings editor");require(Modules.get().get("example-counter").settings.get("counter-step").get().equals(3),"external addon setting uses Meteor setting API");shot("15-addon-settings");}
                    if(ticks==72){mc.getWindow().setWindowed(oldWidth,oldHeight);GuiThemes.select("Zenith");Tabs.get().getFirst().openScreen(GuiThemes.get());}
                    if(ticks==78){require(mc.gui.screen() instanceof ZenithGuiScreen,"switching back restores Zenith dashboard");require(((MeteorGuiTheme)GuiThemes.get()).scale.get()==.75,"Zenith retains its independent scale");next();}
                }
                case 2 -> {
                    if(ticks==1)mc.gui.setScreen(new HudEditorScreen(GuiThemes.get()));
                    if(ticks==30){shot("03-hud-editor");next();}
                }
                case 3 -> {
                    if(ticks==1) {
                        mc.gui.setScreen(new ZenithMenuScreen());
                        mc.createWorldOpenFlows().createFreshLevel("Zenith-Smoke-"+System.currentTimeMillis(),
                            new LevelSettings("Zenith Visual Smoke",GameType.CREATIVE,
                                new LevelSettings.DifficultySettings(Difficulty.NORMAL,false,false),true,WorldDataConfiguration.DEFAULT),
                            new WorldOptions(42,false,false),
                            provider->provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            new ZenithMenuScreen());
                    }
                    if(mc.player!=null&&mc.level!=null&&ticks>40){require(true,"integrated flat world joined");next();}
                    if(ticks>2400)throw new IllegalStateException("world-load timeout");
                }
                case 4 -> {
                    if(ticks==5) {
                        command("gamerule minecraft:send_command_feedback false");
                        command("gamerule minecraft:show_advancement_messages false");
                        command("time set 12000");command("weather rain");
                        command("give @s minecraft:totem_of_undying");
                        command("give @s minecraft:diamond_sword");
                        command("effect give @s minecraft:strength 60 1 true");
                        command("summon minecraft:end_crystal ~3 ~1 ~4 {ShowBottom:0b}");
                        command("summon minecraft:zombie ~ ~ ~3 {NoAI:1b,PersistenceRequired:1b}");
                        Modules.get().get("auto-totem").settings.get("mode",meteordevelopment.meteorclient.systems.modules.combat.AutoTotem.Mode.class).set(meteordevelopment.meteorclient.systems.modules.combat.AutoTotem.Mode.Strict);
                        Modules.get().get("auto-totem").enable();
                        Modules.get().get("small-totem").enable();
                    }
                    if(ticks==75) {
                        if(externalAddon)require(Long.parseLong(Modules.get().get("example-counter").getInfoString())>0,"separate addon receives live Orbit tick events");
                        require(mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING),"Auto Totem equipped a supplied totem");
                        require(!mc.player.getActiveEffects().isEmpty(),"server potion state received");
                        require(Zenith.INSTANCE.frames.size()>0,"real world frames sampled");
                        for(Entity entity:mc.level.entitiesForRendering())if(entity instanceof Zombie&&mc.player.distanceTo(entity)<8){mc.gameMode.attack(mc.player,entity);break;}
                        radarPlayer=new FakePlayerEntity(mc.player,"Radar Preview",20,false);
                        radarPlayer.setPos(mc.player.getX()-3,mc.player.getY(),mc.player.getZ()+3);radarPlayer.spawn();
                    }
                    if(ticks==90)shot("04-world-hud");
                    if(ticks==100){command("summon minecraft:ender_pearl ~ ~2 ~ {Motion:[0.05d,0.2d,0.15d]}");}
                    if(ticks>=110&&ticks<=116){mc.options.keyUp.setDown(true);mc.options.keyUse.setDown(true);meteordevelopment.meteorclient.utils.misc.input.Input.setButtonState(1,true);}
                    if(ticks>=110&&ticks<=114)MeteorClient.EVENT_BUS.post(meteordevelopment.meteorclient.events.meteor.MouseClickEvent.get(new MouseButtonEvent(0,0,new MouseButtonInfo(1,0)),meteordevelopment.meteorclient.utils.misc.input.KeyAction.Press));
                    if(ticks==115)shot("05-pearl-keys");
                    if(ticks==117){mc.options.keyUp.setDown(false);mc.options.keyUse.setDown(false);meteordevelopment.meteorclient.utils.misc.input.Input.setButtonState(1,false);}
                    if(ticks==120){command("weather clear");command("tp @e[type=minecraft:zombie] ~20 ~ ~");command("fill ~-5 ~-4 ~-1 ~5 ~-1 ~9 minecraft:obsidian");mc.player.setXRot(18);command("summon minecraft:tnt ~ ~ ~4 {fuse:20}");}
                    if(ticks>=120&&ticks<=150){mc.level.setRainLevel(0);mc.level.setThunderLevel(0);}
                    if(ticks==143)shot("06-explosion");
                    if(ticks==160)command("summon minecraft:zombie ~2 ~ ~3 {NoAI:1b,PersistenceRequired:1b}");
                    if(ticks==163)command("kill @e[type=minecraft:zombie,distance=..16]");
                    if(ticks==165)shot("07-death");
                    if(ticks==210){
                        require(Zenith.INSTANCE.memory.size()>0,"heap graph sampled");
                        ZenithEffects.Statistics stats=Modules.get().get(ZenithEffects.class).statistics();
                        require(stats.hits()>0,"damage-confirmed hit effect spawned");
                        require(stats.explosions()>0,"server explosion effect spawned");
                        require(stats.replacedClouds()>0,"vanilla explosion cloud replaced with custom effect");
                        require(stats.deaths()>0,"server death effect spawned");
                        require(stats.pearlSamples()>0,"pearl trail sampled");
                        require(stats.crystalFrames()>0,"crystal corona rendered");
                        require(stats.sparks()<=768,"Balanced particle budget respected");
                        require(stats.lightningFrames()>0,"branching explosion filaments rendered");
                        require(stats.trailImpacts()>0,"removed pearl produced a violet arrival pulse");
                        next();
                    }
                }
                case 5 -> {
                    var crystal=Modules.get().get(ZenithCrystalAura.class);
                    var kill=Modules.get().get(ZenithKillAura.class);
                    if(ticks==1) {
                        crystal.enable();require(!Modules.get().get(CrystalAura.class).isActive(),"custom crystal aura excludes base engine");
                        Modules.get().get(CrystalAura.class).enable();require(!crystal.isActive(),"base crystal aura excludes custom engine");
                        Modules.get().get(CrystalAura.class).disable();
                        if(radarPlayer!=null)radarPlayer.despawn();
                        command("summon minecraft:zombie ~ ~ ~2 {NoAI:1b,PersistenceRequired:1b}");
                        kill.settings.get("entities").parse("minecraft:zombie");kill.settings.get("attack-when-holding").parse("All");
                        attackBefore=Zenith.INSTANCE.lastAttack;kill.enable();
                        require(!Modules.get().get(KillAura.class).isActive(),"custom kill aura excludes base engine");
                    }
                    if(ticks==35){require(Zenith.INSTANCE.lastAttack>attackBefore,"custom kill aura attacked an in-world target");mc.gui.setScreen(GuiThemes.get().modulesScreen());}
                    if(ticks==45){require(!kill.attacking,"custom kill aura pauses while GUI is open");shot("08-world-gui");}
                    if(ticks==48){mc.gui.screen().onClose();}
                    if(ticks==65) {
                        require(mc.gui.screen()==null,"custom GUI animated close completed");
                        Modules.get().get(KillAura.class).enable();require(!kill.isActive(),"base kill aura excludes custom engine");
                        Modules.get().get(KillAura.class).disable();next();
                    }
                }
                case 6 -> {
                    var crystal=Modules.get().get(ZenithCrystalAura.class);
                    if(ticks==1) {
                        crystal.settings.get("entities").parse("minecraft:zombie");crystal.settings.get("min-damage").parse("0");crystal.settings.get("rotate").parse("false");
                        crystal.enable();mc.gui.setScreen(GuiThemes.get().modulesScreen());
                        explosionBefore=Modules.get().get(ZenithEffects.class).statistics().explosions();
                        command("summon minecraft:zombie ~ ~ ~3 {NoAI:1b,PersistenceRequired:1b}");
                        command("summon minecraft:end_crystal ~ ~1 ~3 {ShowBottom:0b}");
                    }
                    if(ticks==20){require(Modules.get().get(ZenithEffects.class).statistics().explosions()==explosionBefore,"custom crystal aura pauses new-crystal attacks in menus");mc.gui.screen().onClose();}
                    if(ticks==60){require(Modules.get().get(ZenithEffects.class).statistics().explosions()>explosionBefore,"custom crystal aura resumes and breaks a local crystal");crystal.disable();next();}
                }
                case 7 -> {
                    ZenithEffects fx=Modules.get().get(ZenithEffects.class);
                    if(ticks==1) {
                        command("weather clear");command("time set 16000");command("fill ~-2 ~ ~15 ~2 ~12 ~19 minecraft:obsidian");
                        mc.player.setYRot(0);mc.player.setXRot(-18);
                        fx.settings.get("sky-style").parse("Nebula");fx.settings.get("sky-mood").parse("Purple");
                    }
                    if(ticks<30){mc.level.setRainLevel(0);mc.level.setThunderLevel(0);}
                    if(ticks==15)shot("09a-sky-occlusion");
                    if(ticks==16)command("fill ~-2 ~ ~15 ~2 ~12 ~19 minecraft:air");
                    if(ticks==25) {
                        var stats=fx.statistics();require(stats.skyFrames()>0,"animated skybox submitted frames");
                        require(stats.skyVertices()==fx.budget().skyVertices(),"Balanced sky geometry follows its budget");
                        require(stats.skyStars()==384,"dense starfield rendered with Balanced limit");shot("09-sky-nebula");
                    }
                    if(ticks==32){fx.settings.get("sky-style").parse("Aurora");fx.settings.get("sky-mood").parse("Aurora");}
                    if(ticks==52)shot("10-sky-aurora");
                    if(ticks==60){fx.settings.get("sky-style").parse("Cloudscape");fx.settings.get("sky-mood").parse("EmberDusk");}
                    if(ticks==80)shot("11-sky-dusk");
                    if(ticks==90){rainBefore=mc.level.getRainLevel(1);thunderBefore=mc.level.getThunderLevel(1);fx.settings.get("sky-mood").parse("RainySilver");fx.settings.get("cinematic-rain").parse("true");mc.player.setXRot(8);}
                    if(ticks==115){require(fx.statistics().rainFrames()>0,"cinematic rain streaks and surface ripples rendered");shot("12-cinematic-rain");fx.settings.get("cinematic-rain").parse("false");}
                    if(ticks==117)require(Math.abs(mc.level.getRainLevel(1)-rainBefore)<.02&&Math.abs(mc.level.getThunderLevel(1)-thunderBefore)<.02,"disabling cinematic rain restores previous local weather");
                    if(ticks==135)for(int i=0;i<8;i++)command("summon minecraft:tnt ~"+(i%3-1)+" ~ ~8 {fuse:8}");
                    if(ticks==145)fx.quality.set(ZenithEffects.Quality.Low);
                    if(ticks==150) {
                        require(fx.statistics().sparks()<=256,"live quality downgrade trims particles to Low budget");
                        require(fx.statistics().skyVertices()==fx.budget().skyVertices(),"sky mesh resized to Low budget");
                        require(fx.statistics().skyStars()==128,"star density follows Low budget");
                        require(fx.statistics().meteorFrames()>0,"shooting-star heads and tails rendered");
                        fx.quality.set(ZenithEffects.Quality.High);
                    }
                    if(ticks==160) {
                        require(fx.statistics().skyVertices()==fx.budget().skyVertices(),"sky mesh resized to High budget");
                        require(fx.statistics().sparks()<=1536,"High particle budget respected");
                        fx.quality.set(ZenithEffects.Quality.Balanced);fx.animatedSky.set(false);skyBefore=fx.statistics().skyFrames();
                    }
                    if(ticks==170){require(fx.statistics().skyFrames()==skyBefore,"skybox toggle stops custom sky rendering");fx.disable();}
                    if(ticks==175){require(fx.statistics().sparks()==0&&fx.statistics().skyFrames()==0,"disabling effects clears particles and sky statistics");fx.enable();next();}
                }
                case 8 -> {
                    if(ticks==1){mc.disconnectWithSavingScreen();}
                    if(ticks==40) {
                        Files.writeString(results.resolve("result.json"),"{\"passed\":true,\"checks\":"+checks.size()+",\"workflow\":\"menu, module GUI, editable HUD, integrated world, totem, potion, pearl, explosion, death\"}\n");
                        System.out.println("ZENITH_SMOKE PASSED: "+checks.size()+" checks");finished=true;mc.stop();
                    }
                }
                default -> throw new IllegalStateException("stage");
            }
        } catch(Throwable error) {
            finished=true;System.out.println("ZENITH_SMOKE FAILED stage "+stage+": "+error);error.printStackTrace();
            try{Files.createDirectories(results);Files.writeString(results.resolve("result.json"),new com.google.gson.Gson().toJson(java.util.Map.of("passed",false,"stage",stage,"error",error.toString())));}catch(Exception ignored){}
            mc.stop();
        }
    }
    private void next(){stage++;ticks=0;}
    private void click(double x,double y,int button) {
        double sw=mc.getWindow().getWidth(),sh=mc.getWindow().getHeight();
        double scale=Math.min(1.4,Math.min((sw-38)/900,(sh-38)/530)),guiScale=mc.getWindow().getGuiScale();
        MouseButtonEvent event=new MouseButtonEvent(((sw-900*scale)/2+x*scale)/guiScale,((sh-530*scale)/2+y*scale)/guiScale,new MouseButtonInfo(button,0));
        mc.gui.screen().mouseClicked(event,false);mc.gui.screen().mouseReleased(event);
    }
    private void command(String command){mc.player.connection.sendCommand(command);}
    private void require(boolean condition,String description){if(!condition)throw new IllegalStateException(description);checks.add(description);System.out.println("ZENITH_SMOKE OK: "+description);}
    private void shot(String name) {
        Screenshot.grab(results.toFile(),name+".png",mc.gameRenderer.mainRenderTarget(),1,_ -> {});
        System.out.println("ZENITH_SMOKE screenshot: "+name);
    }
}
