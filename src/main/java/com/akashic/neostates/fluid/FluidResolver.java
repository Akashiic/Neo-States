package com.akashic.neostates.fluid;

import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

import com.akashic.neostates.api.FluidView;
import com.akashic.neostates.water.WaterSeams;

/**
 * What fluid a cell has: a block whose material is water or lava, with its metadata as the level (this covers Et
 * Futurum's bubble column, which is water material with metadata 0 or 8); else the waterlogging layer, for a world, a
 * chunk cache or Angelica's meshing slice.
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
        if (WaterSeams.hosted(access, x, y, z, access.getBlock(x, y, z))) {
            return FluidView.hostedWater();
        }
        return FluidView.EMPTY;
    }
}
