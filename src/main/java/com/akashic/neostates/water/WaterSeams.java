package com.akashic.neostates.water;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.akashic.neostates.net.WaterNet;

/**
 * What the mixins call. Reads answer "a water source" for a cell whose block holds water; writes keep the layer in
 * step with the block of the cell. Nothing here writes a block into the host cell.
 */
public final class WaterSeams {

    private WaterSeams() {}

    // --- reads ---

    /** Whether the cell's block (already read by the caller) shares the cell with water. */
    public static boolean hosted(IBlockAccess access, int x, int y, int z, Block block) {
        return access instanceof World && WaterHolders.canHold(block) && WaterLayer.get((World) access, x, y, z);
    }

    /** The block a fluid query should see: water when the cell holds it, else the block that is there. */
    public static Block blockOrWater(IBlockAccess access, int x, int y, int z, Block block) {
        return hosted(access, x, y, z, block) ? Blocks.water : block;
    }

    // --- writes ---

    /**
     * Called before a chunk replaces the block of a cell. Keeps the water when the new block can hold it, lets it go
     * when it cannot, and fills a holder that is placed into a water source (1.21.1's placement rule).
     *
     * @param x chunk-local, {@code y} world, {@code z} chunk-local
     */
    public static void beforeBlockChange(Chunk chunk, int x, int y, int z, Block newBlock) {
        boolean had = WaterLayer.get(chunk, x, y, z);
        boolean want;
        if (!WaterHolders.canHold(newBlock)) {
            want = false;
        } else if (had) {
            want = true;
        } else {
            Block old = chunk.getBlock(x, y, z);
            want = old.getMaterial() == Material.water && (old == Blocks.water || old == Blocks.flowing_water)
                && chunk.getBlockMetadata(x, y, z) == 0;
        }
        if (want == had) {
            return;
        }
        if (WaterLayer.set(chunk, x, y, z, want)) {
            World world = chunk.worldObj;
            if (world != null && !world.isRemote) {
                int wx = (chunk.xPosition << 4) + x;
                int wz = (chunk.zPosition << 4) + z;
                WaterNet.sendBlock(world, wx, y, wz, want);
                if (want) {
                    WaterTicks.schedule(world, wx, y, wz);
                }
            }
        }
    }
}
