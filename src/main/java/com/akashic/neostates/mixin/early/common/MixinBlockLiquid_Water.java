package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.akashic.neostates.water.WaterSeams;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

/**
 * Water next to a hosted source sees a source: the level it takes from its neighbours, the count for the two-source
 * rule and the flow vector all read a neighbour through these two methods.
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid_Water extends Block {

    protected MixinBlockLiquid_Water(Material material) {
        super(material);
    }

    @WrapOperation(
        method = "func_149804_e",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"))
    private Block neostates$levelBlock(World world, int x, int y, int z, Operation<Block> original,
        @Share("hosted") LocalBooleanRef hosted) {
        Block block = original.call(world, x, y, z);
        boolean water = this.blockMaterial == Material.water && block.getMaterial() != Material.water
            && WaterSeams.hosted(world, x, y, z, block);
        hosted.set(water);
        return water ? (Block) (Object) this : block;
    }

    @WrapOperation(
        method = "func_149804_e",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"))
    private int neostates$levelMeta(World world, int x, int y, int z, Operation<Integer> original,
        @Share("hosted") LocalBooleanRef hosted) {
        return hosted.get() ? 0 : original.call(world, x, y, z);
    }

    /**
     * The flow vector asks whether its own cell is a falling column by its metadata. In a hosted cell that metadata
     * is the host's (a top slab is 8 and above), and the water there is a source.
     */
    @WrapOperation(
        method = "getFlowVector",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"))
    private int neostates$ownCellIsASource(IBlockAccess access, int x, int y, int z, Operation<Integer> original) {
        Block block = access.getBlock(x, y, z);
        if (block.getMaterial() != Material.water && WaterSeams.hosted(access, x, y, z, block)) {
            return 0;
        }
        return original.call(access, x, y, z);
    }

    @WrapOperation(
        method = "getEffectiveFlowDecay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/IBlockAccess;getBlock(III)Lnet/minecraft/block/Block;"))
    private Block neostates$decayBlock(IBlockAccess access, int x, int y, int z, Operation<Block> original,
        @Share("hosted") LocalBooleanRef hosted) {
        Block block = original.call(access, x, y, z);
        boolean water = this.blockMaterial == Material.water && block.getMaterial() != Material.water
            && WaterSeams.hosted(access, x, y, z, block);
        hosted.set(water);
        return water ? (Block) (Object) this : block;
    }

    @WrapOperation(
        method = "getEffectiveFlowDecay",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"))
    private int neostates$decayMeta(IBlockAccess access, int x, int y, int z, Operation<Integer> original,
        @Share("hosted") LocalBooleanRef hosted) {
        return hosted.get() ? 0 : original.call(access, x, y, z);
    }
}
