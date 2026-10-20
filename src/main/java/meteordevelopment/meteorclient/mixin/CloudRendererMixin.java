/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method="render",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private int zenith$cloudMood(int color) {
        if(Modules.get()==null)return color;
        var fx=Modules.get().get(meteordevelopment.meteorclient.zenith.ZenithEffects.class);
        return fx!=null&&fx.isActive()&&fx.purpleSky.get()?fx.cloudTint(color):color;
    }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderClouds(int color, CloudStatus cloudStatus, float bottomY, int range, Vec3 cameraPosition, long gameTime, float partialTicks, CallbackInfo ci) {
        var fx=Modules.get().get(meteordevelopment.meteorclient.zenith.ZenithEffects.class);
        if(fx!=null&&fx.hasSkybox()&&fx.replaceClouds.get()){ci.cancel();return;}
        Ambience ambience = Modules.get().get(Ambience.class);
        if (ambience.isActive() && ambience.endSky.get()) ci.cancel();
    }
}
