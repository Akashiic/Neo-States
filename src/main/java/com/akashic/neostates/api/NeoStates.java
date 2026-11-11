package com.akashic.neostates.api;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.akashic.neostates.fluid.FluidPipeline;
import com.akashic.neostates.fluid.FluidResolver;
import com.akashic.neostates.fluid.HeightKind;
import com.akashic.neostates.net.WaterNet;
import com.akashic.neostates.water.WaterLayer;

/**
 * What other mods ask Neo States. Small and stable, so it can be called by reflection without a compile dependency.
 * Raise {@link #API_VERSION} when adding to it.
 */
public final class NeoStates {

    /**
     * 1: fluid reads ({@link #fluidAt}, {@link #isWaterAt}, {@link #surfaceY}). 2: the waterlogging layer
     * ({@link #isWaterlogged}, {@link #setWaterlogged}).
     */
    public static final int API_VERSION = 2;

    private NeoStates() {}

    public static int apiVersion() {
        return API_VERSION;
    }

    /** The fluid of the cell as a packed {@link FluidView}; {@link FluidView#EMPTY} when there is none. */
    public static int fluidAt(IBlockAccess access, int x, int y, int z) {
        return FluidResolver.resolve(access, x, y, z);
    }

    public static boolean isWaterAt(IBlockAccess access, int x, int y, int z) {
        return FluidView.isWater(FluidResolver.resolve(access, x, y, z));
    }

    /** Whether the cell holds water next to its block (the layer's bit; false for a plain water block). */
    public static boolean isWaterlogged(World world, int x, int y, int z) {
        return WaterLayer.get(world, x, y, z);
    }

    /**
     * Sets or clears the water held in the cell and tells the players watching it. Server side. Only the layer is
     * written: the block, its metadata and its tile entity are left exactly as they are. It does not check that the
     * block is one that may hold water.
     *
     * @return whether anything changed
     */
    public static boolean setWaterlogged(World world, int x, int y, int z, boolean water) {
        if (world.isRemote || !world.blockExists(x, y, z)) {
            return false;
        }
        boolean changed = WaterLayer.set(world.getChunkFromBlockCoords(x, z), x, y, z, water);
        if (changed) {
            WaterNet.sendBlock(world, x, y, z, water);
        }
        return changed;
    }

    /**
     * The absolute height of the fluid surface in the cell, as entities and hooks see it, or {@code NaN} when the
     * cell has no fluid. A source is at {@code y + 8/9}; a cell under more of the same fluid at {@code y + 1}.
     */
    public static double surfaceY(IBlockAccess access, int x, int y, int z) {
        if (FluidView.isEmpty(FluidResolver.resolve(access, x, y, z))) {
            return Double.NaN;
        }
        return y + (double) FluidPipeline.height(access, x, y, z, HeightKind.INTERACTION);
    }
}
