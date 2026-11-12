package com.akashic.neostates.mixin.early.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.akashic.neostates.client.HostedWaterRender;
import com.akashic.neostates.water.WaterHolders;
import com.akashic.neostates.water.WaterSeams;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

/**
 * Draws the water a block holds, in the translucent pass, with vanilla's own liquid renderer (which Angelica also
 * runs). The liquid renderer's reads of the cell and its neighbours see hosted water as a source.
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks_Water {

    @Shadow
    public IBlockAccess blockAccess;

    @Shadow
    public abstract boolean renderBlockLiquid(Block block, int x, int y, int z);

    @Shadow
    public abstract void setRenderBoundsFromBlock(Block block);

    @Inject(method = "renderBlockByRenderType", at = @At("HEAD"), cancellable = true)
    private void neostates$hostedWater(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (ForgeHooksClient.getWorldRenderPass() != 1 || !WaterHolders.canHold(block)) {
            return;
        }
        boolean drawn = false;
        if (WaterSeams.hosted(this.blockAccess, x, y, z, block)) {
            HostedWaterRender.begin();
            try {
                // what vanilla does for the block it is about to draw; the liquid's bottom face uses these bounds
                this.setRenderBoundsFromBlock(Blocks.water);
                drawn = this.renderBlockLiquid(Blocks.water, x, y, z);
            } finally {
                HostedWaterRender.end();
            }
        }
        if (block.getRenderBlockPass() != 1) {
            // the block itself belongs to the first pass: only its water is drawn here
            cir.setReturnValue(drawn);
        }
    }

    @WrapOperation(
        method = "getLiquidHeight",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/IBlockAccess;getBlock(III)Lnet/minecraft/block/Block;"),
        require = 2)
    private Block neostates$cornerBlock(IBlockAccess access, int x, int y, int z, Operation<Block> original,
        @Share("hosted") LocalBooleanRef hosted) {
        Block block = original.call(access, x, y, z);
        boolean water = !block.getMaterial()
            .isLiquid() && WaterSeams.hosted(access, x, y, z, block);
        hosted.set(water);
        return water ? Blocks.water : block;
    }

    @WrapOperation(
        method = "getLiquidHeight",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"))
    private int neostates$cornerLevel(IBlockAccess access, int x, int y, int z, Operation<Integer> original,
        @Share("hosted") LocalBooleanRef hosted) {
        return hosted.get() ? 0 : original.call(access, x, y, z);
    }
}
