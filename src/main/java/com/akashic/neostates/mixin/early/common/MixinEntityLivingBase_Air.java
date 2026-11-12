package com.akashic.neostates.mixin.early.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.physics.WaterPhysics;

/**
 * 1.21.1's {@code LivingEntity.baseTick}: out of the water the air comes back 4 a tick. 1.7.10 fills it at once
 * ({@code setAir(300)}; Crucible passes its {@code maxAirTicks} to the same call).
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase_Air extends Entity {

    protected MixinEntityLivingBase_Air(World world) {
        super(world);
    }

    /**
     * The third {@code setAir} of the method: the first two are the loss of air and the reset after drowning damage.
     */
    @ModifyArg(
        method = "onEntityUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;setAir(I)V", ordinal = 2))
    private int neostates$airComesBackSlowly(int full) {
        return NeoConfig.modernAirRecovery ? WaterPhysics.regainAir(this.getAir(), full) : full;
    }
}
