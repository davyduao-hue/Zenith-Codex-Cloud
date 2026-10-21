package meteordevelopment.meteorclient.zenith;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** View-only aim control: target selection is O(players), once per client tick. */
public abstract class ZenithAim extends Module {
    private final boolean instant;
    private final SettingGroup group = settings.getDefaultGroup();
    private final Setting<Double> range = group.add(new DoubleSetting.Builder().name("range")
        .description("Maximum target distance.").defaultValue(4).range(1, 8).sliderRange(1, 8).build());
    private final Setting<Double> fov = group.add(new DoubleSetting.Builder().name("field-of-view")
        .description("Horizontal targeting cone in degrees.").defaultValue(70).range(1, 180).sliderRange(1, 180).build());
    private final Setting<Double> speed = group.add(new DoubleSetting.Builder().name("turn-speed")
        .description("Maximum degrees of camera movement per tick for aim assist.").defaultValue(5).range(.1, 45).sliderRange(.1, 20).build());
    private final Setting<Boolean> attackOnly = group.add(new BoolSetting.Builder().name("while-attacking")
        .description("Only adjust aim while holding attack.").defaultValue(true).build());
    private final Setting<Boolean> visibleOnly = group.add(new BoolSetting.Builder().name("visible-only")
        .description("Require an unobstructed line of sight.").defaultValue(true).build());

    public static final class Aimbot extends ZenithAim { public Aimbot() { super(true); } }
    public static final class AimAssist extends ZenithAim { public AimAssist() { super(false); } }

    private ZenithAim(boolean instant) {
        super(Categories.Combat, instant ? "aimbot" : "zenith-aim-assist", instant
            ? "Faces a visible player inside the targeting cone. Does not attack."
            : "Smoothly guides the camera towards a visible player while attacking.");
        this.instant = instant;
    }

    @EventHandler private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;
        if (attackOnly.get() && !mc.options.keyAttack.isDown()) return;
        // Only one aim controller may drive the view at a time.
        if (!instant && meteordevelopment.meteorclient.systems.modules.Modules.get().get("aimbot").isActive()) return;
        Player best = null;
        double bestAngle = fov.get() / 2;
        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive() || player.isSpectator()
                || Friends.get().isFriend(player) || mc.player.distanceToSqr(player) > range.get() * range.get()
                || (visibleOnly.get() && !mc.player.hasLineOfSight(player))) continue;
            double yaw = Math.toDegrees(Math.atan2(player.getZ() - mc.player.getZ(), player.getX() - mc.player.getX())) - 90;
            double angle = Math.abs(Mth.wrapDegrees(yaw - mc.player.getYRot()));
            if (angle < bestAngle) { best = player; bestAngle = angle; }
        }
        if (best == null) return;
        double dx = best.getX() - mc.player.getX(), dz = best.getZ() - mc.player.getZ();
        double dy = best.getEyeY() - .18 - mc.player.getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        float step = instant ? 180 : speed.get().floatValue();
        mc.player.setYRot(mc.player.getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - mc.player.getYRot()), -step, step));
        mc.player.setXRot(Mth.clamp(mc.player.getXRot() + Mth.clamp(pitch - mc.player.getXRot(), -step, step), -90, 90));
    }
}
