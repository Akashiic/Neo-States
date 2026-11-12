package com.akashic.neostates.mixin.late.angelica;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashic.neostates.water.WaterSection;

/**
 * Angelica copies each section for its meshing threads. The copy is a subclass of the section, so it has the layer's
 * field; this fills it, with its own array, when the copy is made (on the client thread).
 */
@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.compat.ExtendedBlockStorageExt", remap = false)
public abstract class MixinExtendedBlockStorageExt_Water {

    @Inject(
        method = "copyFrom(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;)V",
        at = @At("TAIL"))
    private void neostates$copyLayer(Chunk chunk, ExtendedBlockStorage storage, CallbackInfo ci) {
        long[] bits = ((WaterSection) storage).neostates$getWater();
        ((WaterSection) this).neostates$setWater(bits == null ? null : bits.clone());
    }
}
