package com.akashic.neostates.api;

import net.minecraft.world.IBlockAccess;

import com.akashic.neostates.fluid.FluidPipeline;
import com.akashic.neostates.fluid.FluidResolver;
import com.akashic.neostates.fluid.HeightKind;

/**
 * What other mods ask Neo States. Small and stable, so it can be called by reflection without a compile dependency.
 * Raise {@link #API_VERSION} when adding to it.
 */
public final class NeoStates {

    /** 1: fluid reads ({@link #fluidAt}, {@link #isWaterAt}, {@link #surfaceY}). */
    public static final int API_VERSION = 1;

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
