package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.akashic.neostates.light.LightMark;
import com.akashic.neostates.water.WaterSeams;

/**
 * Every block change goes through here, whoever makes it: the layer follows the block. The chunk also carries the
 * water opacity its light was computed with.
 */
@Mixin(Chunk.class)
public abstract class MixinChunk_Water implements LightMark {

    @Unique
    private byte neostates$lightMark;

    @Override
    public byte neostates$lightMark() {
        return this.neostates$lightMark;
    }

    @Override
    public void neostates$lightMark(byte mark) {
        this.neostates$lightMark = mark;
    }

    @Inject(method = "func_150807_a(IIILnet/minecraft/block/Block;I)Z", at = @At("HEAD"))
    private void neostates$layerFollowsBlock(int x, int y, int z, Block block, int meta,
        CallbackInfoReturnable<Boolean> cir) {
        WaterSeams.beforeBlockChange((Chunk) (Object) this, x, y, z, block);
    }
}
