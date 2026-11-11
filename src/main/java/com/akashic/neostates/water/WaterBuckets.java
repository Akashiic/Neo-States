package com.akashic.neostates.water;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;

import com.akashic.neostates.api.NeoStates;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.21.1's bucket rules for a block that may hold water: a water bucket used on it fills it (instead of placing water
 * next to it), an empty bucket used on it takes the water back. Forge posts this event for the vanilla buckets before
 * they do anything; buckets of mods that do not post it keep their own behaviour.
 */
public final class WaterBuckets {

    @SubscribeEvent
    public void onBucket(FillBucketEvent event) {
        MovingObjectPosition target = event.target;
        if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
            || event.current == null) {
            return;
        }
        ItemStack result = use(event.world, target.blockX, target.blockY, target.blockZ, event.current);
        if (result != null) {
            event.result = result;
            event.setResult(Event.Result.ALLOW);
        }
    }

    /**
     * @return what the bucket becomes, or null when this is not a bucket on a block that holds (or may hold) water
     */
    public static ItemStack use(World world, int x, int y, int z, ItemStack bucket) {
        Block block = world.getBlock(x, y, z);
        if (!WaterHolders.canHold(block)) {
            return null;
        }
        boolean has = WaterLayer.get(world, x, y, z);
        if (bucket.getItem() == Items.water_bucket && !has) {
            if (world.provider.isHellWorld) {
                return null;
            }
            write(world, x, y, z, true);
            return new ItemStack(Items.bucket);
        }
        if (bucket.getItem() == Items.bucket && has) {
            write(world, x, y, z, false);
            return new ItemStack(Items.water_bucket);
        }
        return null;
    }

    private static void write(World world, int x, int y, int z, boolean water) {
        if (world.isRemote) {
            // the client's guess; the server's packet settles it
            WaterLayer.set(world.getChunkFromBlockCoords(x, z), x, y, z, water);
            world.markBlockForUpdate(x, y, z);
        } else {
            NeoStates.setWaterlogged(world, x, y, z, water);
        }
    }
}
