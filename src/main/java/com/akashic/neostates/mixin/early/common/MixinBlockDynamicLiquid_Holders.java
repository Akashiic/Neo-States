package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.akashic.neostates.water.WaterHolders;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/**
 * Flowing water does not take the cell of a block that can hold water: in 1.21.1 such a block accepts a source and
 * nothing else. Most of them stop water already; rails do not, and 1.7.10's water washes them away.
 */
@Mixin(BlockDynamicLiquid.class)
public abstract class MixinBlockDynamicLiquid_Holders {

    @ModifyReturnValue(method = "func_149807_p", at = @At("RETURN"))
    private boolean neostates$holdersStopTheFlow(boolean blocked, World world, int x, int y, int z) {
        return blocked || WaterHolders.canHold(world.getBlock(x, y, z));
    }
}
