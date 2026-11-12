package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.BlockLiquid;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockLiquid.class)
public interface BlockLiquidInvoker {

    @Invoker("getFlowVector")
    Vec3 neostates$getFlowVector(IBlockAccess world, int x, int y, int z);
}
