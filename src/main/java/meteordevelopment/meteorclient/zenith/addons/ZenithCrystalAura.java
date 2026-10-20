package meteordevelopment.meteorclient.zenith.addons;

import meteordevelopment.meteorclient.mixin.MinecraftAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;

/** Zenith profiles and optional workload guards, retaining Meteor's damage/placement engine. */
public final class ZenithCrystalAura extends CrystalAura {
    public enum Profile { Balanced, Fast, Manual }
    private final SettingGroup zenith=settings.createGroup("Zenith Addons");
    private final Setting<Profile> profile=zenith.add(new EnumSetting.Builder<Profile>().name("zenith-profile")
        .description("Balanced uses a one-tick placement gap; Fast removes the gap; Manual preserves engine settings.").defaultValue(Profile.Balanced)
        .onChanged(value->{if(isActive())applyProfile(value);}).build());
    private final Setting<Boolean> pauseGui=zenith.add(new BoolSetting.Builder().name("pause-in-menus")
        .description("Pause aura work while a menu or inventory is open.").defaultValue(true).build());
    private final Setting<Integer> minimumFps=zenith.add(new IntSetting.Builder().name("minimum-fps")
        .description("Optional workload guard. Zero disables it; otherwise pause while measured FPS is lower.")
        .defaultValue(0).range(0,120).sliderRange(0,120).build());
    public ZenithCrystalAura(){super(ZenithAddons.CATEGORY,"zenith-crystal-aura","Zenith crystal profiles, menu/load guards and Meteor's full crystal damage engine.");}
    @Override public void onActivate() {
        Modules.get().get(CrystalAura.class).disable();
        applyProfile(profile.get());
        super.onActivate();
    }
    private void applyProfile(Profile value) {
        if(value!=Profile.Manual) {
            settings.get("place-delay",Integer.class).set(value==Profile.Fast?0:1);
            settings.get("break-delay",Integer.class).set(0);
        }
    }
    @Override protected boolean shouldRunAuraTick() {
        return (!pauseGui.get()||mc.gui.screen()==null)&&(minimumFps.get()==0||MinecraftAccessor.meteor$getFps()>=minimumFps.get());
    }
}
