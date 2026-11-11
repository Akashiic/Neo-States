package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's own decisions about where water may go, for the hosted source's spreading. */
@Mixin(BlockDynamicLiquid.class)
public interface BlockDynamicLiquidInvoker {

    /** "May this fluid take the cell?" */
    @Invoker("func_149809_q")
    boolean neostates$canFlowInto(World world, int x, int y, int z);

    /** Writes the fluid into a cell that may take it, dropping what was there. */
    @Invoker("func_149813_h")
    void neostates$flowInto(World world, int x, int y, int z, int metadata);

    /** The sides (west, east, north, south) with the shortest way down. */
    @Invoker("func_149808_o")
    boolean[] neostates$optimalSides(World world, int x, int y, int z);
}
