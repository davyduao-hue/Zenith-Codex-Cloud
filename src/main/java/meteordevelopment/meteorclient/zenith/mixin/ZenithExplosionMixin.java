package meteordevelopment.meteorclient.zenith.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.zenith.ZenithEffects;
import net.minecraft.client.multiplayer.ClientExplosionTracker;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.2 schedules secondary explosion clouds separately from the primary emitter. */
@Mixin(ClientExplosionTracker.class)
public abstract class ZenithExplosionMixin {
    @Inject(method="track",at=@At("HEAD"),cancellable=true)
    private void zenith$replaceClouds(Vec3 center,float radius,int blockCount,WeightedList<ExplosionParticleInfo> particles,CallbackInfo ci) {
        if(Modules.get()==null)return;
        ZenithEffects fx=Modules.get().get(ZenithEffects.class);
        if(fx!=null&&fx.replaceExplosionAt(center))ci.cancel();
    }
}
