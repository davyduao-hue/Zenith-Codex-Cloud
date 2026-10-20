package meteordevelopment.meteorclient.zenith.mixin;

import meteordevelopment.meteorclient.zenith.ZenithEffects;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class ZenithSkyMixin {
    @Unique private float zenith$partialTick;
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void zenith$sky(ClientLevel level,float partialTick,Camera camera,SkyRenderState state,CallbackInfo ci) {
        zenith$partialTick=partialTick;
        if(Modules.get()==null)return;
        ZenithEffects fx=Modules.get().get(ZenithEffects.class);
        if(fx!=null&&fx.isActive()&&fx.purpleSky.get())state.skyColor=fx.skyColor();
    }
    @Inject(method="renderSunMoonAndStars",at=@At("HEAD"),cancellable=true)
    private void zenith$renderSkybox(com.mojang.blaze3d.vertex.PoseStack matrices,float sunAngle,float moonAngle,float starAngle,net.minecraft.world.level.MoonPhase moonPhase,float rainBrightness,float starBrightness,CallbackInfo ci) {
        if(Modules.get()==null)return;
        ZenithEffects fx=Modules.get().get(ZenithEffects.class);
        if(fx!=null&&fx.hasSkybox()){fx.renderSky(matrices,zenith$partialTick);ci.cancel();}
    }
}
