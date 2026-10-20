package meteordevelopment.meteorclient.zenith.smoke;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.zenith.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Screenshot;
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
    private final Path results=Path.of("../build/smoke");
    @Override public String getPackage(){return "meteordevelopment.meteorclient.zenith.smoke";}
    @Override public void onInitialize(){MeteorClient.EVENT_BUS.subscribe(this);}

    @EventHandler private void tick(TickEvent.Post event) {
        if(finished)return;
        try {
            if(!started) {
                if(!(mc.gui.screen() instanceof ZenithMenuScreen)||mc.gui.overlay()!=null)return;
                started=true;
                GuiThemes.get().settings.reset();
                Hud.get().resetToDefaultElements();
                Files.createDirectories(results);
                Files.deleteIfExists(results.resolve("result.json"));
                for(String name:List.of("crystal-aura","kill-aura","aimbot","aim-assist","esp","block-esp","stash-finder","auto-totem","small-totem","zenith-effects"))
                    require(Modules.get().get(name)!=null,"module registered: "+name);
                require(Hud.get().infos.keySet().stream().filter(n->n.startsWith("zenith-")).count()==8,"eight editable Zenith widgets registered");
            }
            ticks++;
            switch(stage) {
                case 0 -> { if(ticks==25) { shot("01-menu");next(); } }
                case 1 -> {
                    if(ticks==1)mc.gui.setScreen(GuiThemes.get().modulesScreen());
                    if(ticks==30){shot("02-modules");require(GuiThemes.get().name.equals("Zenith"),"Zenith theme selected");next();}
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
                        require(mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING),"Auto Totem equipped a supplied totem");
                        require(!mc.player.getActiveEffects().isEmpty(),"server potion state received");
                        require(Zenith.INSTANCE.frames.size()>0,"real world frames sampled");
                        for(Entity entity:mc.level.entitiesForRendering())if(entity instanceof Zombie&&mc.player.distanceTo(entity)<8){mc.gameMode.attack(mc.player,entity);break;}
                    }
                    if(ticks==90)shot("04-world-hud");
                    if(ticks==100){command("summon minecraft:ender_pearl ~ ~2 ~ {Motion:[0.05d,0.2d,0.15d]}");}
                    if(ticks==110)shot("05-pearl");
                    if(ticks==120)command("summon minecraft:tnt ~4 ~ ~3 {fuse:20}");
                    if(ticks==145)shot("06-explosion");
                    if(ticks==160)command("summon minecraft:zombie ~2 ~ ~3 {NoAI:1b,PersistenceRequired:1b}");
                    if(ticks==163)command("kill @e[type=minecraft:zombie,distance=..16]");
                    if(ticks==165)shot("07-death");
                    if(ticks==210){
                        require(Zenith.INSTANCE.memory.size()>0,"heap graph sampled");
                        ZenithEffects.Statistics stats=Modules.get().get(ZenithEffects.class).statistics();
                        require(stats.hits()>0,"damage-confirmed hit effect spawned");
                        require(stats.explosions()>0,"server explosion effect spawned");
                        require(stats.deaths()>0,"server death effect spawned");
                        require(stats.pearlSamples()>0,"pearl trail sampled");
                        require(stats.crystalFrames()>0,"crystal corona rendered");
                        require(stats.sparks()<=384,"Balanced particle budget respected");
                        next();
                    }
                }
                case 5 -> {
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
    private void command(String command){mc.player.connection.sendCommand(command);}
    private void require(boolean condition,String description){if(!condition)throw new IllegalStateException(description);checks.add(description);System.out.println("ZENITH_SMOKE OK: "+description);}
    private void shot(String name) {
        Screenshot.grab(results.toFile(),name+".png",mc.gameRenderer.mainRenderTarget(),1,_ -> {});
        System.out.println("ZENITH_SMOKE screenshot: "+name);
    }
}
