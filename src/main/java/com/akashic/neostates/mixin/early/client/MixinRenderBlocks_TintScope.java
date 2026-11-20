package com.akashic.neostates.mixin.early.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;

import org.spongepowered.asm.mixin.Mixin;

import com.akashic.neostates.visual.BiomeTintScope;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/**
 * Opens the window in which the grey water sprite is the right one: {@code renderBlockLiquid} is the one place of the
 * client that asks water for its icon <em>and</em> multiplies it by {@code BlockLiquid.colorMultiplier}. Every chunk
 * renderer in the pack ends up here — the vanilla mesher, the Angelica one and Beddium through
 * {@code renderBlockByRenderType} — and so does the hosted water drawn by {@code MixinRenderBlocks_Water}.
 * <p>
 * The window is closed in a {@code finally}: a throw inside the renderer would otherwise leave the thread grey forever.
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks_TintScope {

    @WrapMethod(method = "renderBlockLiquid")
    private boolean neostates$biomeTintScope(Block block, int x, int y, int z, Operation<Boolean> original) {
        BiomeTintScope.begin();
        try {
            return original.call(block, x, y, z);
        } finally {
            BiomeTintScope.end();
        }
    }
}
