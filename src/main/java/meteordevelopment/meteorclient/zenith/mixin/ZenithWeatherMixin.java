package meteordevelopment.meteorclient.zenith.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.zenith.ZenithEffects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace only rain columns; native snow and weather audio retain their normal paths. */
@Mixin(WeatherEffectRenderer.class)
public abstract class ZenithWeatherMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void zenith$silverRain(ClientLevel level, float partialTick, Vec3 camera,
                                   WeatherRenderState state, CallbackInfo ci) {
        if (Modules.get() == null) return;
        ZenithEffects effects = Modules.get().get(ZenithEffects.class);
        if (effects != null && effects.replacesNativeRain() && !state.rainColumns.isEmpty()) {
            effects.onNativeRainReplaced();
            state.rainColumns.clear();
        }
    }
}
