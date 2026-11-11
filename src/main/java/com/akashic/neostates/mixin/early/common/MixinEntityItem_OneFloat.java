package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.physics.ItemMotion;

/**
 * Et Futurum makes items float too, roughly (a fixed 0.05 upwards, horizontal motion cut to 0.4, any liquid), from a
 * hook at the head of {@code EntityItem.onUpdate} in a mixin of priority 1002. {@link MixinEntityItem_Float}
 * remembers the motion in its own head hook; of two head hooks the one from the mixin of higher priority runs
 * later, so that one runs before Et Futurum's and this one after it. While our floating is on and the item is in
 * water (by their own test), this puts the motion back, so the two do not add up; in lava theirs is left alone.
 * <p>
 * The self-test measures an item's rising speed, which is wrong if either order is not what is said here.
 */
@Mixin(value = EntityItem.class, priority = 1100)
public abstract class MixinEntityItem_OneFloat extends Entity {

    protected MixinEntityItem_OneFloat(World world) {
        super(world);
    }

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void neostates$onlyOneFloat(CallbackInfo ci) {
        if (!NeoConfig.floatingItems) {
            return;
        }
        // their own test: the block the item's position is in. Water only; in lava theirs stays
        if (this.worldObj
            .getBlock(
                MathHelper.floor_double(this.posX),
                MathHelper.floor_double(this.posY),
                MathHelper.floor_double(this.posZ))
            .getMaterial() == Material.water) {
            ((ItemMotion) (Object) this).neostates$restoreMotion();
        }
    }
}
