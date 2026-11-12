package com.akashic.neostates.mixin.early.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBreakable;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import com.akashic.neostates.client.GrayWater;
import com.akashic.neostates.visual.OverlayRule;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * 1.21.1's {@code water_overlay}: the side of water that touches glass, ice or leaves is drawn flat, not with the
 * flowing texture. In the liquid renderer the icon of each side is asked once, in the loop of the four sides, after
 * the bottom face; that one call is wrapped (the two before the bottom face are the top's).
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks_Overlay {

    @Shadow
    public IBlockAccess blockAccess;

    @WrapOperation(
        method = "renderBlockLiquid",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/RenderBlocks;renderFaceYNeg(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V")),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderBlocks;getBlockIconFromSideAndMetadata(Lnet/minecraft/block/Block;II)Lnet/minecraft/util/IIcon;"))
    private IIcon neostates$overlayAgainstGlass(RenderBlocks self, Block block, int side, int meta,
        Operation<IIcon> original, Block liquid, int x, int y, int z) {
        IIcon icon = original.call(self, block, side, meta);
        IIcon overlay = GrayWater.overlay();
        int[] offset = OverlayRule.neighbour(side);
        if (overlay == null || offset == null || liquid.getMaterial() != Material.water) {
            return icon;
        }
        Block beside = this.blockAccess.getBlock(x + offset[0], y, z + offset[1]);
        boolean halfTransparent = beside instanceof BlockBreakable && !(beside instanceof BlockPortal);
        return OverlayRule.applies(true, side, halfTransparent, beside instanceof BlockLeavesBase) ? overlay : icon;
    }
}
