package com.akashic.neostates.fluid;

import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

import com.akashic.neostates.api.FluidView;

/**
 * What fluid a cell has. In this phase only the block answers: a block whose material is water or lava, with its
 * metadata as the level (this covers Et Futurum's bubble column, which is water material with metadata 0 or 8). The
 * waterlogging layer is consulted here once it exists.
 */
public final class FluidResolver {

    private FluidResolver() {}

    public static int resolve(IBlockAccess access, int x, int y, int z) {
        Material material = access.getBlock(x, y, z)
            .getMaterial();
        if (material == Material.water) {
            return FluidView.ofLegacy(FluidView.TYPE_WATER, access.getBlockMetadata(x, y, z));
        }
        if (material == Material.lava) {
            return FluidView.ofLegacy(FluidView.TYPE_LAVA, access.getBlockMetadata(x, y, z));
        }
        return FluidView.EMPTY;
    }
}
