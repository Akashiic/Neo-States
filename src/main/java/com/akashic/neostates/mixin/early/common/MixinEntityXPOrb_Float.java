package com.akashic.neostates.mixin.early.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.physics.ModernWater;
import com.akashic.neostates.physics.WaterPhysics;

/**
 * 1.21.1's {@code ExperienceOrb.tick} in water: with its eyes under the surface an orb has no gravity, rises by
 * 0.0005 a tick up to 0.06 and keeps 0.99 of its horizontal motion.
 */
@Mixin(EntityXPOrb.class)
public abstract class MixinEntityXPOrb_Float extends Entity {

    protected MixinEntityXPOrb_Float(World world) {
        super(world);
    }

    @ModifyConstant(method = "onUpdate", constant = @Constant(doubleValue = 0.029999999329447746D, ordinal = 0))
    private double neostates$floatInsteadOfFalling(double gravity) {
        if (!NeoConfig.floatingItems || !ModernWater
            .submerged(this.worldObj, this.posX, this.boundingBox.minY + WaterPhysics.ORB_EYES, this.posZ)) {
            return gravity;
        }
        this.motionX *= WaterPhysics.FLOAT_DRAG;
        this.motionZ *= WaterPhysics.FLOAT_DRAG;
        return WaterPhysics.orbFloat(this.motionY);
    }
}
