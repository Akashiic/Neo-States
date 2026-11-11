package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.physics.ModernWater;
import com.akashic.neostates.physics.WaterBody;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * An entity is in water, and is pushed by it, as in 1.21.1. Only the one call that asks the world is replaced; the
 * splash, the fall distance and the fire of the vanilla method stay. The world's own
 * {@code handleMaterialAcceleration} is not changed (other callers and Et Futurum's hooks in it are as they were; for
 * the entity's own water update they are simply not run).
 */
@Mixin(Entity.class)
public abstract class MixinEntity_ModernWater implements WaterBody {

    @Shadow
    public World worldObj;

    @Shadow
    public double posX;

    @Shadow
    public double posY;

    @Shadow
    public double posZ;

    @Shadow
    public abstract float getEyeHeight();

    @Unique
    private double neostates$waterHeight;

    @Override
    public double neostates$waterHeight() {
        return this.neostates$waterHeight;
    }

    @Override
    public void neostates$waterHeight(double height) {
        this.neostates$waterHeight = height;
    }

    @WrapOperation(
        method = "handleWaterMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;handleMaterialAcceleration(Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/block/material/Material;Lnet/minecraft/entity/Entity;)Z"))
    private boolean neostates$modernWater(World world, AxisAlignedBB box, Material material, Entity entity,
        Operation<Boolean> original) {
        if (!NeoConfig.modernWaterPhysics) {
            return original.call(world, box, material, entity);
        }
        return ModernWater.update(entity);
    }

    /** The eyes are in water when they are under the surface of their cell, not anywhere in a water cell. */
    @Inject(method = "isInsideOfMaterial", at = @At("HEAD"), cancellable = true)
    private void neostates$eyesUnderTheSurface(Material material, CallbackInfoReturnable<Boolean> cir) {
        if (material == Material.water && NeoConfig.modernWaterPhysics) {
            cir.setReturnValue(
                ModernWater.submerged(this.worldObj, this.posX, this.posY + (double) this.getEyeHeight(), this.posZ));
        }
    }
}
