package com.akashic.neostates.mixin.early.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.physics.WaterBody;
import com.akashic.neostates.physics.WaterPhysics;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * Sinking and swimming up as in 1.21.1. Two points of {@code EntityLivingBase}, and neither is one of the three
 * {@code 0.8} water-drag multiplies Akashic Player Backport requires in {@code moveEntityWithHeading}.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase_ModernWater extends Entity {

    protected MixinEntityLivingBase_ModernWater(World world) {
        super(world);
    }

    /** The first {@code 0.02D} of the method is the water branch's {@code motionY -= 0.02D}; the second is lava's. */
    @ModifyConstant(method = "moveEntityWithHeading", constant = @Constant(doubleValue = 0.02D, ordinal = 0))
    private double neostates$waterGravity(double legacy) {
        return NeoConfig.modernWaterPhysics ? WaterPhysics.gravity(this.isSprinting()) : legacy;
    }

    /**
     * The jump key: vanilla paddles up whenever the entity is in water. 1.21.1 jumps off the ground instead when the
     * entity stands in water no deeper than the threshold.
     */
    @ModifyExpressionValue(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInWater()Z"))
    private boolean neostates$shallowWaterJumps(boolean inWater) {
        if (!inWater || !NeoConfig.modernWaterPhysics) {
            return inWater;
        }
        return WaterPhysics.swimsUp(
            this.onGround,
            ((WaterBody) this).neostates$waterHeight(),
            WaterPhysics.jumpThreshold(this.getEyeHeight()));
    }
}
