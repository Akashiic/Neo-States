package com.akashic.neostates.fluid;

import net.minecraft.world.IBlockAccess;

/**
 * One step of the height pipeline. The base stage reproduces vanilla; later stages (waves) change what came before.
 */
public interface FluidStage {

    /**
     * @param view     the fluid of the cell, never empty
     * @param previous the height the stages before this one gave, 0..1
     * @return the height of the surface in the cell, 0..1
     */
    float height(IBlockAccess access, int x, int y, int z, int view, HeightKind kind, float previous);

    /** Whether the result is the same on client and server for the same world and tick. */
    boolean deterministic();
}
