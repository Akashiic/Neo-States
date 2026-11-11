package com.akashic.neostates.mixin.early.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.akashic.neostates.water.WaterSeams;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

/** The camera inside hosted water gets the underwater fog and field of view. */
@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo_Water {

    @WrapOperation(
        method = "getBlockAtEntityViewpoint",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"),
        require = 2)
    private static Block neostates$cameraBlock(World world, int x, int y, int z, Operation<Block> original,
        @Share("hosted") LocalBooleanRef hosted) {
        Block block = original.call(world, x, y, z);
        boolean water = !block.getMaterial()
            .isLiquid() && WaterSeams.hosted(world, x, y, z, block);
        hosted.set(water);
        return water ? net.minecraft.init.Blocks.water : block;
    }

    @WrapOperation(
        method = "getBlockAtEntityViewpoint",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"))
    private static int neostates$cameraLevel(World world, int x, int y, int z, Operation<Integer> original,
        @Share("hosted") LocalBooleanRef hosted) {
        return hosted.get() ? 0 : original.call(world, x, y, z);
    }
}
