package com.akashic.neostates.water;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * Reads and writes of the waterlogging layer. Only the bit is ever written: the block, its metadata and its tile entity
 * are never touched from here (the invariant of layer C).
 */
public final class WaterLayer {

    private WaterLayer() {}

    public static boolean get(Chunk chunk, int x, int y, int z) {
        ExtendedBlockStorage section = Sections.section(chunk, y >> 4);
        if (section == null) {
            return false;
        }
        return WaterBits.get(((WaterSection) section).neostates$getWater(), WaterBits.index(x, y, z));
    }

    public static boolean get(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) {
            return false;
        }
        return get(world.getChunkFromBlockCoords(x, z), x, y, z);
    }

    /**
     * Sets the bit in the chunk, nothing else.
     *
     * @return whether it changed; false too when the section does not exist (there is no block to hold water)
     */
    public static boolean set(Chunk chunk, int x, int y, int z, boolean water) {
        if (chunk instanceof EmptyChunk) {
            return false;
        }
        ExtendedBlockStorage section = Sections.section(chunk, y >> 4);
        if (section == null) {
            return false;
        }
        WaterSection layer = (WaterSection) section;
        long[] bits = layer.neostates$getWater();
        if (bits == null) {
            if (!water) {
                return false;
            }
            bits = new long[WaterBits.LONGS];
            layer.neostates$setWater(bits);
        }
        boolean changed = WaterBits.set(bits, WaterBits.index(x, y, z), water);
        if (changed) {
            if (!water && WaterBits.isEmpty(bits)) {
                layer.neostates$setWater(null);
            }
            chunk.setChunkModified();
        }
        return changed;
    }
}
