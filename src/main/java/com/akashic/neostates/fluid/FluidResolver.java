package com.akashic.neostates.fluid;

import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.akashic.neostates.api.FluidView;
import com.akashic.neostates.water.WaterLayer;

/**
 * What fluid a cell has: a block whose material is water or lava, with its metadata as the level (this covers Et
 * Futurum's bubble column, which is water material with metadata 0 or 8); else the waterlogging layer, when the
 * access is a world. Chunk caches and the meshing slices do not see the layer yet (phase 5).
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
        if (access instanceof World && WaterLayer.get((World) access, x, y, z)) {
            return FluidView.hostedWater();
        }
        return FluidView.EMPTY;
    }
}
