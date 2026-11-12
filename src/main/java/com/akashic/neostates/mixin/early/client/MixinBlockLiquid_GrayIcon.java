package com.akashic.neostates.mixin.early.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.ForgeHooksClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.akashic.neostates.client.GrayWater;

/**
 * Water in the world is drawn with the grey sprites, so the biome colour is the whole colour as in 1.21.1. The block's
 * own icon array keeps the blue ones: {@code BlockLiquid.getLiquidIcon} (cauldrons, Forge's water fluid in tanks) is
 * not tinted by biome and must stay as it is.
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid_GrayIcon extends Block {

    protected MixinBlockLiquid_GrayIcon(Material material) {
        super(material);
    }

    @Inject(method = "getIcon(II)Lnet/minecraft/util/IIcon;", at = @At("HEAD"), cancellable = true)
    private void neostates$grayWater(int side, int meta, CallbackInfoReturnable<IIcon> cir) {
        // Only while a chunk is being meshed, where the biome colour is applied. Anything else that asks a water
        // block for its icon (WAILA, NEI, a mod drawing it flat) gets the blue one, which needs no tint.
        if (this.blockMaterial == Material.water && ForgeHooksClient.getWorldRenderPass() >= 0) {
            IIcon icon = GrayWater.icon(side);
            if (icon != null) {
                cir.setReturnValue(icon);
            }
        }
    }
}
