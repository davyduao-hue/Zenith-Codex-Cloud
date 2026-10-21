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
import me.pindour.catppuccin.gui.screens.CatppuccinModulesScreen;
import me.pindour.catppuccin.gui.screens.CatppuccinModuleScreen;
import net.minecraft.client.gui.screens.TitleScreen;
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
    private int blastCaptureTick=-1;
    private int skyBefore;
    private final long[] extraBefore=new long[5];
    private static final String[] EXTRA_TOGGLES={"impact-crescents","blast-comets","crystal-energy-streams","death-starburst","pearl-spirals"};
    private float rainBefore,thunderBefore;
    private boolean externalAddon,glazedAddon;
    private int oldWidth,oldHeight;
    private long hudPressesBefore;
    private double lookMaximum;
    private final Path results=Path.of("../build/smoke");
    @Override public String getPackage(){return "meteordevelopment.meteorclient.zenith.smoke";}
    @Override public void onInitialize(){MeteorClient.EVENT_BUS.subscribe(this);}

    @EventHandler private void tick(TickEvent.Post event) {
        if(finished)return;
        try {
            if(!started) {
                if(!(mc.gui.screen() instanceof ZenithMenuScreen)||mc.gui.overlay()!=null)return;
                started=true;
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                require(meteordevelopment.meteorclient.utils.Utils.canOpenGui(),"Right Shift GUI eligibility recognizes Zenith main menu");
                require(mc.gui.screen() instanceof TitleScreen,"Zenith menu retains TitleScreen compatibility with addons");
                GuiThemes.select("Zenith");GuiThemes.get().settings.reset();
                require(GuiThemes.get().name.equals("Zenith 2.0"),"legacy Zenith theme selection migrates to Zenith 2.0");
                Hud.get().resetToDefaultElements();
                Files.createDirectories(results);
                Files.deleteIfExists(results.resolve("result.json"));
                for(String name:List.of("crystal-aura","kill-aura","aimbot","zenith-aim-assist","esp","block-esp","stash-finder","auto-totem","small-totem","zenith-effects","zenith-crystal-aura","zenith-kill-aura"))
                    require(Modules.get().get(name)!=null,"module registered: "+name);
                require(Modules.get().get("zenith-crystal-aura").addon.name.equals("Zenith Addons"),"custom combat modules have Zenith Addons identity");
                require(Hud.get().infos.keySet().stream().filter(n->n.startsWith("zenith-")).count()==8,"eight editable Zenith widgets registered");
                for(String name:List.of("zenith-crystal-aura","zenith-kill-aura")){Modules.get().get(name).disable();Modules.get().get(name).settings.reset();}
                Modules.get().get(ZenithEffects.class).settings.reset();Modules.get().get(ZenithEffects.class).enable();
                require(java.util.Arrays.asList(GuiThemes.getNames()).containsAll(List.of("Meteor","Zenith 2.0","Catppuccin")),"Meteor, Zenith 2.0 and Catppuccin themes are available");
                require(me.pindour.catppuccin.CatppuccinAddon.class.getClassLoader()!=null,"built-in Catppuccin source loaded");
                require(Modules.get().get("ore-sim")!=null,"built-in Nora Ore Sim registered");
                require(Modules.get().get("ore-sim").addon.name.equals("Nora Tweaks"),"Nora module retains its addon identity");
                require(Modules.get().get("ore-sim").category.name.equals("Nora Tweaks"),"Nora category registered alongside Zenith");
                require(Modules.get().getAll().stream().filter(m->m.addon!=null&&m.addon.name.equals("Nora Tweaks")).count()>=14,"Nora modules register without duplicate inherited modules");
                require(meteordevelopment.meteorclient.commands.Commands.COMMANDS.stream().filter(c->c.getClass().getPackageName().startsWith("me.noramibu.tweaks")).count()==5,"all five Nora commands registered");
                dev.xpple.cubiomes.CubiomesInit.load();
                require(dev.xpple.cubiomes.Cubiomes.Stronghold()>0,"nested Cubiomes native library loads on the test platform");
                Modules.get().get(ZenithEffects.class).settings.get("blast-lightning").parse("true");
                glazedAddon=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("glazed");
                if(glazedAddon) {
                    var glazed=Modules.get().getAll().stream().filter(m->m.getClass().getName().startsWith("com.nnpg.glazed.")).toList();
                    require(glazed.size()>=66,"external Glazed 26.2 modules registered: "+glazed.size());
                    require(Modules.get().get(ZenithAim.AimAssist.class)!=null,"Zenith Aim Assist survives Glazed aim module registration");
                    require(Modules.get().get(me.noramibu.tweaks.modules.DeepslateESP.class)!=null,"Nora Deepslate survives Glazed's identically named module");
                    for(String themeName:List.of("Meteor","Catppuccin","Zenith 2.0")) {
                        GuiThemes.select(themeName);
                        var field=meteordevelopment.meteorclient.gui.GuiTheme.class.getDeclaredField("settingsFactory");field.setAccessible(true);
                        var factory=field.get(GuiThemes.get());
                        var get=meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory.class.getDeclaredMethod("getFactory",Class.class);get.setAccessible(true);
                        int custom=0;
                        for(var module:glazed) {
                            for(var group:module.settings)for(var setting:group)if(setting.getClass().getName().startsWith("com.nnpg.glazed.")) {
                                require(get.invoke(factory,setting.getClass())!=null,themeName+" factory handles "+setting.getClass().getSimpleName());custom++;
                            }
                            // Exercise all native settings screens without enabling server automation.
                            mc.gui.setScreen(GuiThemes.get().moduleScreen(module));
                        }
                        for(String type:List.of("RandomBetweenIntSetting","RandomBetweenDoubleSetting","TextDisplaySetting")) {
                            var clazz=Class.forName("com.nnpg.glazed.settings."+type);
                            require(get.invoke(factory,clazz)!=null,themeName+" registers Glazed "+type+" factory");
                        }
                        require(true,"all "+glazed.size()+" Glazed module settings screens opened in "+themeName);
                    }
                    GuiThemes.select("Zenith 2.0");mc.gui.setScreen(new ZenithMenuScreen());
                }
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
                    if(ticks==18){require(mc.gui.screen() instanceof CatppuccinModulesScreen,"Zenith defaults to rounded native category windows");shot("02-native-zenith");}
                    if(ticks==20){((ZenithGuiTheme)GuiThemes.get()).layout.set(ZenithGuiTheme.Layout.Dashboard);mc.gui.setScreen(GuiThemes.get().modulesScreen());}
                    if(ticks==30){shot("02-modules");require(mc.gui.screen() instanceof ZenithGuiScreen,"custom animated GUI opened");require(GuiThemes.get().name.equals("Zenith 2.0"),"Zenith 2.0 theme selected");}
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
                    if(ticks==68&&externalAddon){require(mc.gui.screen() instanceof ModuleScreen,"external addon opens original Meteor settings editor");require(Modules.get().get("example-counter").settings.get("counter-step").get().equals(3),"external addon setting uses Meteor setting API");shot("15-addon-settings");verifyCategoryButton();}
                    if(ticks==72){GuiThemes.select("Catppuccin");GuiThemes.get().settings.reset();GuiThemes.get().clearWindowConfigs();Tabs.get().getFirst().openScreen(GuiThemes.get());}
                    if(ticks==80){require(mc.gui.screen() instanceof CatppuccinModulesScreen,"Catppuccin source theme opens its native module windows");shot("16-catppuccin-gui");mc.gui.setScreen(GuiThemes.get().moduleScreen(Modules.get().get("ore-sim")));}
                    if(ticks==88){require(mc.gui.screen() instanceof CatppuccinModuleScreen,"Nora module opens in Catppuccin settings editor");shot("17-nora-settings");verifyCategoryButton();GuiThemes.select("Zenith 2.0");((ZenithGuiTheme)GuiThemes.get()).layout.set(ZenithGuiTheme.Layout.Categories);Tabs.get().getFirst().openScreen(GuiThemes.get());}
                    if(ticks==98){
                        require(mc.gui.screen() instanceof CatppuccinModulesScreen,"switching back restores Zenith category layout");
                        require(((ZenithGuiTheme)GuiThemes.get()).scale.get()==.85,"Zenith retains its independent scale");
                        GuiThemes.save();
                        var saved=net.minecraft.nbt.NbtIo.read(new java.io.File(MeteorClient.FOLDER,"gui/gui.nbt").toPath());
                        require(saved!=null&&saved.getStringOr("currentTheme","").equals("Zenith 2.0"),"Zenith 2.0 selection persisted after switching all three themes");
                        shot("18-zenith-theme");
                    }
                    if(ticks==100) {
                        var f=meteordevelopment.meteorclient.gui.WidgetScreen.class.getDeclaredField("root");f.setAccessible(true);
                        var next=findButton((meteordevelopment.meteorclient.gui.widgets.WWidget)f.get(mc.gui.screen()),"Next");
                        require(next!=null,"Zenith category pager is present");next.action.run();
                    }
                    if(ticks==104) {
                        var f=CatppuccinModulesScreen.class.getDeclaredField("controller");f.setAccessible(true);
                        var controller=(meteordevelopment.meteorclient.gui.widgets.containers.WContainer)f.get(mc.gui.screen());
                        var visible=controller.cells.stream().map(c->c.widget()).filter(w->w.visible).toList();
                        require(!visible.isEmpty(),"next category page shows addon windows");
                        for(int i=1;i<visible.size();i++)require(visible.get(i-1).x+visible.get(i-1).width<visible.get(i).x,"paged category windows do not overlap");
                        shot("28-zenith-addon-page");
                    }
                    if(ticks==106)mc.gui.setScreen(new TitleScreen());
                    if(ticks==114){require(mc.gui.screen() instanceof ZenithMenuScreen,"returning to TitleScreen preserves Zenith menu with both built-in addons");next();}
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
                    }
                    if(ticks==78)shot("29-impact-crescents");
                    if(ticks==90)shot("04-world-hud");
                    if(ticks==100){command("summon minecraft:ender_pearl ~ ~2 ~ {Motion:[0.05d,0.2d,0.15d]}");}
                    if(ticks>=110&&ticks<=116){mc.options.keyUp.setDown(true);mc.options.keyUse.setDown(true);meteordevelopment.meteorclient.utils.misc.input.Input.setButtonState(1,true);}
                    if(ticks>=110&&ticks<=114)MeteorClient.EVENT_BUS.post(meteordevelopment.meteorclient.events.meteor.MouseClickEvent.get(new MouseButtonEvent(0,0,new MouseButtonInfo(1,0)),meteordevelopment.meteorclient.utils.misc.input.KeyAction.Press));
                    if(ticks==115)shot("05-pearl-keys");
                    if(ticks==117){mc.options.keyUp.setDown(false);mc.options.keyUse.setDown(false);meteordevelopment.meteorclient.utils.misc.input.Input.setButtonState(1,false);}
                    if(ticks==120){command("weather clear");command("tp @e[type=minecraft:zombie] ~20 ~ ~");command("fill ~-5 ~-4 ~-1 ~5 ~-1 ~9 minecraft:obsidian");mc.player.setXRot(18);command("summon minecraft:tnt ~ ~ ~4 {fuse:20}");}
                    if(ticks>=120&&ticks<=150){mc.level.setRainLevel(0);mc.level.setThunderLevel(0);}
                    if(ticks>=140&&ticks<160&&blastCaptureTick<0&&Modules.get().get(ZenithEffects.class).statistics().explosions()>0)blastCaptureTick=ticks+3;
                    if(ticks==blastCaptureTick)shot("06-explosion");
                    if(blastCaptureTick>0&&ticks==blastCaptureTick+5)shot("31-blast-comets");
                    if(ticks==160)command("summon minecraft:zombie ~2 ~ ~3 {NoAI:1b,PersistenceRequired:1b}");
                    if(ticks==163)command("kill @e[type=minecraft:zombie,distance=..16]");
                    if(ticks==165)shot("07-death");
                    if(ticks==180)shot("30-death-starburst");
                    if(ticks==210){
                        require(Zenith.INSTANCE.memory.size()>0,"heap graph sampled");
                        ZenithEffects.Statistics stats=Modules.get().get(ZenithEffects.class).statistics();
                        ZenithEffects fx=Modules.get().get(ZenithEffects.class);
                        for(int i=0;i<5;i++)require(fx.extraRendered(i)>0,EXTRA_TOGGLES[i]+" submitted extra geometry");
                        require(fx.extraPrimitiveCount()<=fx.extraPrimitiveLimit(),"Balanced extra geometry stays within shared frame allowance");
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
                        command("summon minecraft:end_crystal ~2 ~1 ~4 {ShowBottom:0b}");
                        mc.player.setYRot(0);mc.player.setXRot(-18);
                        fx.settings.get("sky-style").parse("Nebula");fx.settings.get("sky-mood").parse("Purple");
                        fx.settings.get("starfield").parse("true");fx.settings.get("shooting-stars").parse("true");fx.settings.get("aurora-ribbons").parse("true");
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
                    if(ticks==125) {
                        fx.settings.get("extra-effects-strength").parse("0");
                        for(int i=0;i<5;i++)extraBefore[i]=fx.extraRendered(i);
                    }
                    if(ticks==130) {
                        require(fx.extraPrimitiveCount()==0,"zero extra strength stops all additional geometry");
                        for(int i=0;i<5;i++)require(fx.extraRendered(i)==extraBefore[i],EXTRA_TOGGLES[i]+" respects zero extra strength");
                        fx.settings.get("extra-effects-strength").reset();
                    }
                    if(ticks==135)for(int i=0;i<8;i++)command("summon minecraft:tnt ~"+(i%3-1)+" ~ ~8 {fuse:8}");
                    if(ticks==145)fx.quality.set(ZenithEffects.Quality.Low);
                    if(ticks==150) {
                        require(fx.extraPrimitiveCount()>0&&fx.extraPrimitiveCount()<=fx.extraPrimitiveLimit(),"Low extra geometry obeys its frame allowance during overlapping blasts");
                        require(fx.statistics().sparks()<=256,"live quality downgrade trims particles to Low budget");
                        require(fx.statistics().skyVertices()==fx.budget().skyVertices(),"sky mesh resized to Low budget");
                        require(fx.statistics().skyStars()==128,"star density follows Low budget");
                        require(fx.statistics().meteorFrames()>0,"shooting-star heads and tails rendered");
                        fx.quality.set(ZenithEffects.Quality.High);
                    }
                    if(ticks==160) {
                        require(fx.statistics().skyVertices()==fx.budget().skyVertices(),"sky mesh resized to High budget");
                        require(fx.statistics().sparks()<=1536,"High particle budget respected");
                        require(fx.extraPrimitiveCount()<=fx.extraPrimitiveLimit(),"High extra geometry obeys its frame allowance");
                        for(int i=0;i<5;i++){fx.settings.get(EXTRA_TOGGLES[i]).parse("false");extraBefore[i]=fx.extraRendered(i);}
                        fx.quality.set(ZenithEffects.Quality.Balanced);fx.animatedSky.set(false);skyBefore=fx.statistics().skyFrames();
                    }
                    if(ticks==165) {
                        require(fx.extraPrimitiveCount()==0,"extra effect toggles stop their geometry");
                        for(int i=0;i<5;i++)require(fx.extraRendered(i)==extraBefore[i],EXTRA_TOGGLES[i]+" toggle stops submissions");
                    }
                    if(ticks==168)for(String toggle:EXTRA_TOGGLES)fx.settings.get(toggle).reset();
                    if(ticks==170){require(fx.statistics().skyFrames()==skyBefore,"skybox toggle stops custom sky rendering");fx.disable();}
                    if(ticks==175){for(int i=0;i<5;i++)require(fx.extraRendered(i)==0,"disabling effects clears extra family "+i);require(fx.statistics().sparks()==0&&fx.statistics().skyFrames()==0,"disabling effects clears particles and sky statistics");fx.enable();fx.animatedSky.set(true);}
                    if(ticks==180){
                        require(fx.statistics().skyFrames()>0,"sky UBO and renderer recreate after module disable and re-enable");
                        fx.settings.get("visual-preset").parse("ReferenceSword");
                        require(fx.settings.get("sky-style").get().toString().equals("ReferenceStorm")&&fx.rain.get(),"sword reference preset selects silver storm and local rain");
                        mc.player.setXRot(-12);
                    }
                    if(ticks==195){require(fx.nativeRainReplacements()>0,"silver rain replaces native blue rain geometry");require(fx.statistics().skyStars()==0,"reference storm suppresses the starfield");shot("19-reference-storm");fx.settings.get("visual-preset").parse("ReferenceCrystal");}
                    if(ticks==198){
                        require(fx.settings.get("sky-style").get().toString().equals("ReferenceDusk")&&!fx.rain.get(),"crystal reference preset selects red dusk and restores rain");
                        require(!fx.replaceClouds.get(),"reference preset retains the block clouds visible in the recording");
                        fx.settings.get("effect-density").parse("1.7");
                        fx.fromTag(fx.toTag());
                        require(fx.settings.get("effect-density").get().equals(1.7),"loading saved preset does not overwrite customized effect density");
                        command("fill ~-2 ~ ~15 ~2 ~12 ~19 minecraft:obsidian");
                    }
                    if(ticks==210){require(fx.settings.get("effect-density").get().equals(1.7),"preset does not reset manual settings every tick");shot("20-reference-dusk-occlusion");command("fill ~-2 ~ ~15 ~2 ~12 ~19 minecraft:air");}
                    if(ticks==220){
                        shot("21-reference-dusk");require(fx.softLightCount()>0,"soft fragment-shaded light batch submitted");
                        fx.skySource.set(ZenithEffects.SkySource.Vanilla);skyBefore=fx.statistics().skyFrames();
                        require(!fx.tintsSky()&&!fx.hasSkybox(),"Vanilla mode disables sky, fog and cloud tint overrides");
                    }
                    if(ticks==226){require(fx.statistics().skyFrames()==skyBefore,"Vanilla mode preserves native sky rendering");shot("24-vanilla-sky");fx.skySource.set(ZenithEffects.SkySource.ResourcePack);}
                    if(ticks==232){require(fx.statistics().skyFrames()==skyBefore&&!fx.tintsSky(),"Resource Pack mode releases sky rendering and tint to external packs");fx.skySource.set(ZenithEffects.SkySource.CustomPanorama);fx.panoramaTexture.set("zenith-smoke:textures/sky/panorama.png");fx.replaceClouds.set(true);}
                    if(ticks==240){require(fx.statistics().skyFrames()>skyBefore,"custom panorama loaded through ResourceManager and submitted");shot("25-custom-panorama");fx.panoramaTexture.set("zenith-smoke:missing.png");skyBefore=fx.statistics().skyFrames();}
                    if(ticks==246){require(fx.statistics().skyFrames()==skyBefore,"missing custom panorama falls back without cancelling native sky");fx.skySource.set(ZenithEffects.SkySource.Zenith);fx.settings.get("sky-speed").parse("0");}
                    if(ticks==248){command("tp @s ~ ~ ~ 0 -35");}
                    if(ticks==254){shot("26-stable-sky-before");command("tp @s ~8 ~6 ~8 0 -35");}
                    if(ticks==268){shot("27-stable-sky-after");fx.settings.get("sky-speed").reset();next();}
                }
                case 8 -> {
                    if(ticks==1){mc.gui.setScreen(null);mc.player.setYRot(0);mc.player.setXRot(0);}
                    if(ticks==5){
                        require((Double)hud(ZenithHud.Kind.Keys).settings.get("size-multiplier").get()==2,"existing and new HUD widgets default to 2x size");
                        require(hud(ZenithHud.Kind.Keys).getWidth()>170,"enlarged keycaps render at readable physical size");
                    }
                    if(ticks==5)hudPressesBefore=hud(ZenithHud.Kind.Keys).feedback().acceptedPresses();
                    if(ticks==6) {
                        for(int key:new int[]{87,32}) {
                            MeteorClient.EVENT_BUS.post(meteordevelopment.meteorclient.events.meteor.KeyInputEvent.get(new KeyEvent(key,0,0),meteordevelopment.meteorclient.utils.misc.input.KeyAction.Press));
                            MeteorClient.EVENT_BUS.post(meteordevelopment.meteorclient.events.meteor.KeyInputEvent.get(new KeyEvent(key,0,0),meteordevelopment.meteorclient.utils.misc.input.KeyAction.Release));
                        }
                        for(int button:new int[]{0,1}) {
                            var click=new MouseButtonEvent(0,0,new MouseButtonInfo(button,0));
                            MeteorClient.EVENT_BUS.post(meteordevelopment.meteorclient.events.meteor.MouseClickEvent.get(click,meteordevelopment.meteorclient.utils.misc.input.KeyAction.Press));
                            MeteorClient.EVENT_BUS.post(meteordevelopment.meteorclient.events.meteor.MouseClickEvent.get(click,meteordevelopment.meteorclient.utils.misc.input.KeyAction.Release));
                        }
                    }
                    if(ticks==8) {
                        var feedback=hud(ZenithHud.Kind.Keys).feedback();
                        require(feedback.acceptedPresses()>=hudPressesBefore+4,"keyboard, jump and both mouse taps latch even when released between frames");
                        require(feedback.activeRipples()>=4,"independent circular key ripples are active after short taps");
                        require(feedback.keyAmounts().get(0)>.1&&feedback.keyAmounts().get(6)>.1,"W and jump short taps visibly illuminate their tiles");
                        shot("22-key-tap-rings");
                    }
                    if(ticks>=20&&ticks<=32){mc.player.setYRot(mc.player.getYRot()+8);mc.player.setXRot(mc.player.getXRot()+2);}
                    if(ticks>=21&&ticks<=35){var feedback=hud(ZenithHud.Kind.Radar).feedback();lookMaximum=Math.max(lookMaximum,Math.hypot(feedback.lookX(),feedback.lookY()));}
                    if(ticks==32)shot("23-look-direction");
                    if(ticks==36){require(lookMaximum>.05&&lookMaximum<=1.001,"direction dot deflects with camera motion and stays inside its circle");}
                    if(ticks==60) {
                        var look=hud(ZenithHud.Kind.Radar).feedback();var keys=hud(ZenithHud.Kind.Keys).feedback();
                        require(Math.hypot(look.lookX(),look.lookY())<.04,"direction dot recenters after the camera stops");
                        require(keys.activeRipples()==0&&keys.keyAmounts().stream().allMatch(v->v<.02),"released keys settle and transient ripples expire");
                        next();
                    }
                }
                case 9 -> {
                    if(ticks==1){mc.disconnectFromWorld(net.minecraft.network.chat.Component.literal("Zenith smoke completed"));}
                    if(ticks==40) {
                        require(mc.gui.screen() instanceof ZenithMenuScreen,"disconnect returns to Zenith main menu after all themes and addons");
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
    private ZenithHud hud(ZenithHud.Kind kind) {
        for(var element:Hud.get())if(element instanceof ZenithHud z&&z.feedback().kind()==kind)return z;
        throw new IllegalStateException("Missing HUD "+kind);
    }
    private meteordevelopment.meteorclient.gui.widgets.pressable.WButton findButton(meteordevelopment.meteorclient.gui.widgets.WWidget widget,String label) {
        if(widget instanceof meteordevelopment.meteorclient.gui.widgets.pressable.WButton b&&label.equals(b.getText()))return b;
        if(widget instanceof meteordevelopment.meteorclient.gui.widgets.containers.WContainer c)for(var cell:c.cells){var found=findButton(cell.widget(),label);if(found!=null)return found;}
        return null;
    }
    private void verifyCategoryButton() throws Exception {
        var field=meteordevelopment.meteorclient.gui.WindowScreen.class.getDeclaredField("window");field.setAccessible(true);
        var button=findCategoryButton((meteordevelopment.meteorclient.gui.widgets.WWidget)field.get(mc.gui.screen()));
        require(button!=null,"Nora custom category button is integrated into "+GuiThemes.get().name+" module settings");
        button.action.run();
        require(mc.gui.screen() instanceof me.noramibu.tweaks.gui.screens.AddToCategoryScreen,"Nora custom-category action opens the integrated editor");
    }
    private meteordevelopment.meteorclient.gui.widgets.pressable.WButton findCategoryButton(meteordevelopment.meteorclient.gui.widgets.WWidget widget) {
        if(widget instanceof meteordevelopment.meteorclient.gui.widgets.pressable.WButton b&&"Add to Custom Category".equals(b.getText()))return b;
        if(widget instanceof meteordevelopment.meteorclient.gui.widgets.containers.WContainer c)for(var cell:c.cells){var found=findCategoryButton(cell.widget());if(found!=null)return found;}
        return null;
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
