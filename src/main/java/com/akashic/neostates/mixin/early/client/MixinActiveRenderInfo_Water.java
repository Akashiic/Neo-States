package com.akashic.neostates.mixin.early.client;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.akashic.neostates.physics.ModernWater;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/**
 * "Which block is the camera in" decides the underwater fog and field of view. The answer is corrected, not the
 * method's reads: the camera is in water when it is under the surface of its cell (hosted water included), with the
 * surface every other reader uses.
 */
@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo_Water {

    @ModifyReturnValue(method = "getBlockAtEntityViewpoint", at = @At("RETURN"))
    private static Block neostates$cameraInWater(Block original, World world, EntityLivingBase entity,
        float partialTicks) {
        if (original.getMaterial() == Material.lava) {
            return original;
        }
        Vec3 camera = ActiveRenderInfo.projectViewFromEntity(entity, (double) partialTicks);
        int x = MathHelper.floor_double(camera.xCoord);
        int y = MathHelper.floor_double(camera.yCoord);
        int z = MathHelper.floor_double(camera.zCoord);
        float surface = ModernWater.surface(world, x, y, z);
        if (surface < 0.0F) {
            return original;
        }
        if (camera.yCoord < y + (double) surface) {
            Block here = world.getBlock(x, y, z);
            return here.getMaterial() == Material.water ? here : Blocks.water;
        }
        // above the surface of a water cell: what is over it, as vanilla answers
        Block above = world.getBlock(x, y + 1, z);
        return above.getMaterial() == Material.water ? Blocks.air : above;
    }
}
