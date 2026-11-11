package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.akashic.neostates.water.HolderFlag;

@Mixin(Block.class)
public abstract class MixinBlock_Holder implements HolderFlag {

    @Unique
    private byte neostates$holder;

    @Override
    public byte neostates$holder() {
        return this.neostates$holder;
    }

    @Override
    public void neostates$holder(byte value) {
        this.neostates$holder = value;
    }
}
