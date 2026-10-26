/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;
import net.minecraft.client.resources.SkinManager;

@Mixin(SkinManager.TextureCache.class)
public interface FileCacheAccessor {
    @Accessor("root")
    Path meteor$getDirectory();
}
