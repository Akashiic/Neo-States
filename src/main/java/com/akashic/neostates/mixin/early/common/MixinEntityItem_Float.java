package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.physics.ItemMotion;
import com.akashic.neostates.physics.ModernWater;
import com.akashic.neostates.physics.WaterBody;
import com.akashic.neostates.physics.WaterPhysics;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * 1.21.1's {@code ItemEntity.tick} in water: with the surface more than 0.1 above its feet an item has no gravity,
 * gains 0.0005 a tick up to 0.06 and keeps 0.99 of its horizontal motion; the current still pushes it
 * ({@code Entity.handleWaterMovement}, already 1.21.1's). So it rises to the surface and drifts with the water.
 * <p>
 * It also remembers the motion the item has when its tick begins, for {@link MixinEntityItem_OneFloat}.
 */
@Mixin(EntityItem.class)
public abstract class MixinEntityItem_Float extends Entity implements ItemMotion {

    @Unique
    private double neostates$headX;
    @Unique
    private double neostates$headY;
    @Unique
    private double neostates$headZ;

    @Override
    public void neostates$saveMotion() {
        this.neostates$headX = this.motionX;
        this.neostates$headY = this.motionY;
        this.neostates$headZ = this.motionZ;
    }

    @Override
    public void neostates$restoreMotion() {
        this.motionX = this.neostates$headX;
        this.motionY = this.neostates$headY;
        this.motionZ = this.neostates$headZ;
    }

    protected MixinEntityItem_Float(World world) {
        super(world);
    }

    @Unique
    private boolean neostates$floats() {
        return NeoConfig.floatingItems
            && ((WaterBody) (Object) this).neostates$waterHeight() > WaterPhysics.ITEM_FLOAT_DEPTH;
    }

    /**
     * An item has its own {@code handleWaterMovement}: the whole box against the world, without the cut the other
     * entities get. It goes through the same reading of the surface, which also records how deep the item is and
     * pushes it as 1.21.1 does.
     */
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

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void neostates$rememberMotion(CallbackInfo ci) {
        this.neostates$saveMotion();
    }

    @Unique
    private double neostates$beforeGravityY;

    /**
     * Gravity is not touched where it is applied: Galacticraft already modifies that constant (gravity on other
     * planets), and a constant takes one modifier. The vertical motion is noted before it and put back, with the
     * float added, before the move.
     */
    @Inject(
        method = "onUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onUpdate()V", shift = At.Shift.AFTER))
    private void neostates$beforeGravity(CallbackInfo ci) {
        this.neostates$beforeGravityY = this.motionY;
    }

    @WrapOperation(
        method = "onUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityItem;moveEntity(DDD)V", ordinal = 0))
    private void neostates$floatInsteadOfFalling(EntityItem self, double x, double y, double z,
        Operation<Void> original) {
        // noClip: the item is inside a block and is being pushed out of it; that motion is left alone
        if (this.noClip || !this.neostates$floats()) {
            original.call(self, x, y, z);
            return;
        }
        this.motionX *= WaterPhysics.FLOAT_DRAG;
        this.motionZ *= WaterPhysics.FLOAT_DRAG;
        this.motionY = this.neostates$beforeGravityY - WaterPhysics.itemFloat(this.neostates$beforeGravityY);
        original.call(self, this.motionX, this.motionY, this.motionZ);
    }
}
