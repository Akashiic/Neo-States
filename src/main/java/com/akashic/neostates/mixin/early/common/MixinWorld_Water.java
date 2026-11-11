package com.akashic.neostates.mixin.early.common;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.akashic.neostates.core.Targets;
import com.akashic.neostates.water.WaterHolders;
import com.akashic.neostates.water.WaterLayer;
import com.akashic.neostates.water.WaterSeams;
import com.akashic.neostates.water.WaterTicks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

/**
 * The world's fluid queries see hosted water. Each wraps the block read of the query and leaves the rest of the
 * method alone: the {@code minY < 0.0} tests BelowCore rewrites, and the {@code 0.014D} and {@code normalize()} Et
 * Futurum hooks, are not touched (docs/COMPATIBILIDADE.md, rules 1 and 5).
 */
@Mixin(World.class)
public abstract class MixinWorld_Water {

    @Shadow
    public boolean isRemote;

    @Shadow
    public abstract boolean setBlock(int x, int y, int z, Block block, int meta, int flags);

    // --- breaking a block that holds water leaves the water (1.21.1's removeBlock) ---

    @Inject(method = "setBlockToAir(III)Z", at = @At("HEAD"), cancellable = true)
    private void neostates$leaveTheWater(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        World self = (World) (Object) this;
        if (WaterLayer.get(self, x, y, z)) {
            cir.setReturnValue(this.setBlock(x, y, z, Blocks.water, 0, 3));
        }
    }

    // --- a neighbour changed: the hosted source may have somewhere to go ---

    @Inject(method = "notifyBlockOfNeighborChange(IIILnet/minecraft/block/Block;)V", at = @At("HEAD"))
    private void neostates$wakeHostedWater(int x, int y, int z, Block neighbor, CallbackInfo ci) {
        World self = (World) (Object) this;
        if (this.isRemote) {
            return;
        }
        // a hosted source may flow; a dry holder beside new water may fill (the two-source rule)
        if (WaterLayer.get(self, x, y, z)
            || neighbor.getMaterial() == Material.water && WaterHolders.canHold(self.getBlock(x, y, z))) {
            WaterTicks.schedule(self, x, y, z);
        }
    }

    // --- entities: in water, and pushed by it ---

    @WrapOperation(method = "handleMaterialAcceleration", at = @At(value = "INVOKE", target = Targets.WORLD_GET_BLOCK))
    private Block neostates$accelerationBlock(World world, int x, int y, int z, Operation<Block> original,
        @Local(argsOnly = true) Material material, @Share("hosted") LocalBooleanRef hosted) {
        Block block = original.call(world, x, y, z);
        boolean water = material == Material.water && block.getMaterial() != Material.water
            && WaterSeams.hosted(world, x, y, z, block);
        hosted.set(water);
        return water ? Blocks.water : block;
    }

    @WrapOperation(method = "handleMaterialAcceleration", at = @At(value = "INVOKE", target = Targets.WORLD_GET_META))
    private int neostates$accelerationLevel(World world, int x, int y, int z, Operation<Integer> original,
        @Share("hosted") LocalBooleanRef hosted) {
        return hosted.get() ? 0 : original.call(world, x, y, z);
    }

    /**
     * The push of a hosted cell is added here and not by the block's method: Et Futurum's liquid physics reads the
     * cell's metadata there as a fluid level, and in a hosted cell that metadata belongs to the slab or the stairs.
     */
    @WrapOperation(
        method = "handleMaterialAcceleration",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;velocityToAddToEntity(Lnet/minecraft/world/World;IIILnet/minecraft/entity/Entity;Lnet/minecraft/util/Vec3;)V"))
    private void neostates$accelerationPush(Block block, World world, int x, int y, int z, Entity entity, Vec3 sum,
        Operation<Void> original, @Share("hosted") LocalBooleanRef hosted) {
        if (!hosted.get()) {
            original.call(block, world, x, y, z, entity, sum);
            return;
        }
        Vec3 flow = ((BlockLiquidInvoker) Blocks.flowing_water).neostates$getFlowVector(world, x, y, z);
        sum.xCoord += flow.xCoord;
        sum.yCoord += flow.yCoord;
        sum.zCoord += flow.zCoord;
    }

    // --- box queries ---

    @WrapOperation(method = "isAnyLiquid", at = @At(value = "INVOKE", target = Targets.WORLD_GET_BLOCK))
    private Block neostates$anyLiquidBlock(World world, int x, int y, int z, Operation<Block> original) {
        Block block = original.call(world, x, y, z);
        return block.getMaterial()
            .isLiquid() ? block : WaterSeams.blockOrWater(world, x, y, z, block);
    }

    @WrapOperation(method = "isMaterialInBB", at = @At(value = "INVOKE", target = Targets.WORLD_GET_BLOCK))
    private Block neostates$materialInBoxBlock(World world, int x, int y, int z, Operation<Block> original,
        @Local(argsOnly = true) Material material) {
        Block block = original.call(world, x, y, z);
        return material == Material.water && block.getMaterial() != Material.water
            ? WaterSeams.blockOrWater(world, x, y, z, block)
            : block;
    }

    @WrapOperation(method = "isAABBInMaterial", at = @At(value = "INVOKE", target = Targets.WORLD_GET_BLOCK))
    private Block neostates$boxInMaterialBlock(World world, int x, int y, int z, Operation<Block> original,
        @Local(argsOnly = true) Material material, @Share("hosted") LocalBooleanRef hosted) {
        Block block = original.call(world, x, y, z);
        boolean water = material == Material.water && block.getMaterial() != Material.water
            && WaterSeams.hosted(world, x, y, z, block);
        hosted.set(water);
        return water ? Blocks.water : block;
    }

    @WrapOperation(method = "isAABBInMaterial", at = @At(value = "INVOKE", target = Targets.WORLD_GET_META))
    private int neostates$boxInMaterialLevel(World world, int x, int y, int z, Operation<Integer> original,
        @Share("hosted") LocalBooleanRef hosted) {
        return hosted.get() ? 0 : original.call(world, x, y, z);
    }
}
