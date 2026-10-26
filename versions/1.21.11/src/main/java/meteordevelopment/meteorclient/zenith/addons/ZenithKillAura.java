package meteordevelopment.meteorclient.zenith.addons;

import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;

public final class ZenithKillAura extends KillAura {
    private final SettingGroup zenith=settings.createGroup("Zenith Addons");
    private final Setting<Boolean> pauseGui=zenith.add(new BoolSetting.Builder().name("pause-in-menus")
        .description("Pause targeting and attacks while a menu or inventory is open.").defaultValue(true).build());
    private final Setting<Boolean> requireAttack=zenith.add(new BoolSetting.Builder().name("hold-to-attack")
        .description("Only run the aura while the attack key is held.").defaultValue(false).build());
    private final Setting<Integer> minimumFps=zenith.add(new IntSetting.Builder().name("minimum-fps")
        .description("Optional measured-FPS workload guard; zero disables it.").defaultValue(0).range(0,120).sliderRange(0,120).build());
    public ZenithKillAura(){super(ZenithAddons.CATEGORY,"zenith-kill-aura","Zenith combat controls and workload guards on Meteor's cooldown-aware attack engine.");}
    @Override public void onActivate(){Modules.get().get(KillAura.class).disable();super.onActivate();}
    @Override protected boolean shouldRunAuraTick(){return (!pauseGui.get()||mc.screen==null)
        &&(!requireAttack.get()||mc.options.keyAttack.isDown())&&(minimumFps.get()==0||MinecraftClientAccessor.meteor$getFps()>=minimumFps.get());}
    @Override protected CrystalAura crystalAuraForPause() {
        ZenithCrystalAura custom=Modules.get().get(ZenithCrystalAura.class);
        return custom!=null&&custom.isActive()?custom:super.crystalAuraForPause();
    }
}
