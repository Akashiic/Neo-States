package com.akashic.neostates.mixin.early.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.akashic.neostates.client.GrayWater;
import com.akashic.neostates.visual.BiomeTintScope;

/**
 * Water in the world is drawn with the grey sprites, so the biome colour is the whole colour as in 1.21.1. The block's
 * own icon array keeps the blue ones: {@code BlockLiquid.getLiquidIcon} (cauldrons, Forge's water fluid in tanks) is
 * not tinted by biome and must stay as it is.
 * <p>
 * The grey sprite is only right where the colour comes from the biome, that is inside the liquid renderer
 * ({@link BiomeTintScope}). Anything else that asks a water block for its icon — WAILA/WDMla's tooltip, NEI, an item
 * preview, a mod drawing it flat — gets the blue one, which needs no tint. Asking Forge for the world render pass was
 * the first attempt at that test and it does not survive the pack: FalseTweaks {@code @Overwrite}s
 * {@code ForgeHooksClient.getWorldRenderPass} and Beddium meshes off the main thread, so the value the GUI sees is not
 * "no pass running" (issue #2, the grey icon in WDMla's display).
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid_GrayIcon extends Block {

    protected MixinBlockLiquid_GrayIcon(Material material) {
        super(material);
    }

    @Inject(method = "getIcon(II)Lnet/minecraft/util/IIcon;", at = @At("HEAD"), cancellable = true)
    private void neostates$grayWater(int side, int meta, CallbackInfoReturnable<IIcon> cir) {
        if (this.blockMaterial == Material.water && BiomeTintScope.active()) {
            IIcon icon = GrayWater.icon(side);
            if (icon != null) {
                cir.setReturnValue(icon);
            }
        }
    }
}
