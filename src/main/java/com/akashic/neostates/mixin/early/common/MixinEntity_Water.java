package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.akashic.neostates.water.WaterSeams;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

/** Eyes inside hosted water count as eyes in water: breathing, the fog, the shaders' isEyeInWater. */
@Mixin(Entity.class)
public abstract class MixinEntity_Water {

    @WrapOperation(
        method = "isInsideOfMaterial",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"))
    private Block neostates$eyesInHostedWater(World world, int x, int y, int z, Operation<Block> original,
        @Local(argsOnly = true) Material material) {
        Block block = original.call(world, x, y, z);
        return material == Material.water && block.getMaterial() != Material.water
            ? WaterSeams.blockOrWater(world, x, y, z, block)
            : block;
    }
}
