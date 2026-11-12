package com.akashic.neostates.mixin.early.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.akashic.neostates.water.WaterSeams;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/**
 * Water does not draw a face against a neighbour that holds water, as it does not against water. The result is
 * adjusted instead of the block read inside the method: in the TEOF pack that read is not the vanilla call any more
 * (a wrap of it found nothing to wrap), and the result is the same whatever the body looks like.
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid_WaterFaces extends Block {

    protected MixinBlockLiquid_WaterFaces(Material material) {
        super(material);
    }

    /** {@code x, y, z} are the neighbour's, as vanilla passes them. */
    @ModifyReturnValue(method = "shouldSideBeRendered", at = @At("RETURN"))
    private boolean neostates$noFaceAgainstHostedWater(boolean original, IBlockAccess access, int x, int y, int z,
        int side) {
        if (!original || this.blockMaterial != Material.water) {
            return original;
        }
        Block neighbour = access.getBlock(x, y, z);
        return !WaterSeams.hosted(access, x, y, z, neighbour);
    }
}
