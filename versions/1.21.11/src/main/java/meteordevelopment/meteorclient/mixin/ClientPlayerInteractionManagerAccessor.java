/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("destroyProgress")
    float meteor$getBreakingProgress();

    @Accessor("destroyProgress")
    void meteor$setCurrentBreakingProgress(float progress);

    @Accessor("destroyBlockPos")
    BlockPos meteor$getCurrentBreakingBlockPos();
}
