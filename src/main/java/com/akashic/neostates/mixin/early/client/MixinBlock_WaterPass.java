package com.akashic.neostates.mixin.early.client;

import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.akashic.neostates.water.WaterHolders;

/**
 * A block that may hold water is also offered to the translucent pass, where {@code RenderBlocks} draws the water it
 * holds (and nothing else). Both vanilla and Angelica ask {@code canRenderInPass} before drawing a block in a pass. A
 * mod block that overrides this method is not reached: its water is not drawn.
 */
@Mixin(Block.class)
public abstract class MixinBlock_WaterPass {

    @Inject(method = "canRenderInPass(I)Z", at = @At("RETURN"), cancellable = true, remap = false)
    private void neostates$offerToWaterPass(int pass, CallbackInfoReturnable<Boolean> cir) {
        if (pass == 1 && !cir.getReturnValueZ() && WaterHolders.canHold((Block) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
