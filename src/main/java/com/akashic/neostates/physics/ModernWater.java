package com.akashic.neostates.physics;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;

import com.akashic.neostates.fluid.FluidPipeline;
import com.akashic.neostates.fluid.HeightKind;
import com.akashic.neostates.mixin.early.common.BlockLiquidInvoker;
import com.akashic.neostates.water.WaterSeams;

/**
 * 1.21.1's {@code Entity#updateFluidHeightAndDoFluidPushing} for water, on a 1.7.10 world: whether the entity is in
 * water, how deep, and the push of the current. One definition of the surface (the pipeline's interaction height)
 * serves the body, the eyes and the camera.
 */
public final class ModernWater {

    private ModernWater() {}

    /**
     * The surface of the water in a cell, 0..1 above the cell's floor, or a negative number when the cell has no
     * water. A Forge fluid block of water material answers with its own filled fraction.
     */
    public static float surface(IBlockAccess access, int x, int y, int z) {
        Block block = access.getBlock(x, y, z);
        if (block.getMaterial() == Material.water) {
            if (block instanceof IFluidBlock && access instanceof World) {
                float filled = ((IFluidBlock) block).getFilledPercentage((World) access, x, y, z);
                return filled < 0.0F ? 1.0F : Math.min(filled, 1.0F);
            }
            return FluidPipeline.height(access, x, y, z, HeightKind.INTERACTION);
        }
        if (WaterSeams.hosted(access, x, y, z, block)) {
            return FluidPipeline.height(access, x, y, z, HeightKind.INTERACTION);
        }
        return -1.0F;
    }

    /** Whether a point is under the water surface of its cell. */
    public static boolean submerged(IBlockAccess access, double px, double py, double pz) {
        int x = MathHelper.floor_double(px);
        int y = MathHelper.floor_double(py);
        int z = MathHelper.floor_double(pz);
        float surface = surface(access, x, y, z);
        return surface >= 0.0F && py < y + (double) surface;
    }

    /**
     * Replaces {@code World.handleMaterialAcceleration(box, water, entity)} for the entity's own update.
     *
     * @return whether the entity is in water
     */
    public static boolean update(Entity entity) {
        World world = entity.worldObj;
        AxisAlignedBB box = entity.boundingBox;
        double minX = box.minX + 0.001D;
        double minY = box.minY + 0.001D;
        double minZ = box.minZ + 0.001D;
        int x0 = MathHelper.floor_double(minX);
        int x1 = MathHelper.ceiling_double_int(box.maxX - 0.001D);
        int y0 = MathHelper.floor_double(minY);
        int y1 = MathHelper.ceiling_double_int(box.maxY - 0.001D);
        int z0 = MathHelper.floor_double(minZ);
        int z1 = MathHelper.ceiling_double_int(box.maxZ - 0.001D);
        WaterBody body = (WaterBody) entity;
        if (!world.checkChunksExist(x0, y0, z0, x1, y1, z1)) {
            body.neostates$waterHeight(0.0D);
            return false;
        }
        boolean pushed = entity.isPushedByWater();
        boolean inWater = false;
        double height = 0.0D;
        double[] flow = new double[3];
        Vec3 scratch = null;
        int cells = 0;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    float surface = surface(world, x, y, z);
                    if (surface < 0.0F) {
                        continue;
                    }
                    double top = y + (double) surface;
                    if (top < minY) {
                        continue;
                    }
                    inWater = true;
                    height = Math.max(top - minY, height);
                    if (!pushed) {
                        continue;
                    }
                    Block block = world.getBlock(x, y, z);
                    Vec3 cell;
                    if (block instanceof BlockLiquid) {
                        cell = ((BlockLiquidInvoker) block).neostates$getFlowVector(world, x, y, z);
                    } else if (block instanceof IFluidBlock) {
                        if (scratch == null) {
                            scratch = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
                        }
                        scratch.xCoord = scratch.yCoord = scratch.zCoord = 0.0D;
                        block.velocityToAddToEntity(world, x, y, z, entity, scratch);
                        cell = scratch;
                    } else {
                        // hosted water, or a water-material block that is not a liquid block (a bubble column)
                        cell = ((BlockLiquidInvoker) Blocks.flowing_water).neostates$getFlowVector(world, x, y, z);
                    }
                    double scale = height < WaterPhysics.SHALLOW ? height : 1.0D;
                    flow[0] += cell.xCoord * scale;
                    flow[1] += cell.yCoord * scale;
                    flow[2] += cell.zCoord * scale;
                    cells++;
                }
            }
        }
        if (cells > 0) {
            WaterPhysics.push(flow, cells, entity instanceof EntityPlayer, entity.motionX, entity.motionZ);
            entity.motionX += flow[0];
            entity.motionY += flow[1];
            entity.motionZ += flow[2];
        }
        body.neostates$waterHeight(inWater ? height : 0.0D);
        return inWater;
    }
}
