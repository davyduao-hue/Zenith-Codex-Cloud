package meteordevelopment.meteorclient.zenith.mixin;

import meteordevelopment.meteorclient.zenith.ZenithEffects;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class ZenithSkyMixin {
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void zenith$sky(ClientLevel level,float partialTick,Camera camera,SkyRenderState state,CallbackInfo ci) {
        if(Modules.get()==null)return;
        ZenithEffects fx=Modules.get().get(ZenithEffects.class);
        if(fx!=null&&fx.isActive()&&fx.purpleSky.get())state.skyColor=fx.skyColor();
    }
}
