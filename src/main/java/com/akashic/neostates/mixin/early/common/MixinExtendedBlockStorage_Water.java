package com.akashic.neostates.mixin.early.common;

import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.akashic.neostates.water.WaterSection;

/**
 * Adds the waterlogging layer to the section. Only a field: the block, metadata and light arrays (which EndlessIDs
 * and others rewrite) are not touched. Anchoring on the section is what makes BelowCore's deep sections work the same
 * way as the vanilla ones; see docs/adr/ADR-0001-armazenamento-waterlogging.md.
 */
@Mixin(ExtendedBlockStorage.class)
public abstract class MixinExtendedBlockStorage_Water implements WaterSection {

    @Unique
    private long[] neostates$water;

    @Override
    public long[] neostates$getWater() {
        return this.neostates$water;
    }

    @Override
    public void neostates$setWater(long[] bits) {
        this.neostates$water = bits;
    }
}
