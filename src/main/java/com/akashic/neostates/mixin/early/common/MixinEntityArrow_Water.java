package com.akashic.neostates.mixin.early.common;

import net.minecraft.entity.projectile.EntityArrow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.physics.WaterPhysics;

/** 1.21.1's {@code AbstractArrow.getWaterInertia}: an arrow keeps 0.6 of its motion per tick in water, not 0.8. */
@Mixin(EntityArrow.class)
public abstract class MixinEntityArrow_Water {

    @ModifyConstant(method = "onUpdate", constant = @Constant(floatValue = 0.8F, ordinal = 0))
    private float neostates$waterInertia(float legacy) {
        return NeoConfig.modernWaterPhysics ? WaterPhysics.ARROW_WATER_INERTIA : legacy;
    }
}
