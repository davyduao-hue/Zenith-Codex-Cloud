package meteordevelopment.meteorclient.zenith;

import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Items;

public final class SmallTotem extends Module {
    private final Setting<Double> scale = settings.getDefaultGroup().add(new DoubleSetting.Builder()
        .name("scale").description("First-person totem size; leaves other held items unchanged.")
        .defaultValue(.55).range(.1, 1).sliderRange(.1, 1).build());

    public SmallTotem() { super(Categories.Render, "small-totem", "Shrinks only the totem in either hand."); }

    @EventHandler private void onHand(HeldItemRendererEvent event) {
        if (mc.player == null || !mc.player.getItemInHand(event.hand).is(Items.TOTEM_OF_UNDYING)) return;
        float s = scale.get().floatValue();
        event.matrix.scale(s, s, s);
    }
}
