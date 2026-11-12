package com.akashic.neostates.water;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;

/**
 * Which faces of a block that holds water are closed. In 1.21.1 water leaves a cell through a face unless the
 * shapes on the two sides of it close the whole square ({@code FlowingFluid.canPassThroughWall}): a bottom slab
 * holds its water over a hole, a top slab lets it fall, stairs keep it behind their back. 1.7.10 has no shapes, so
 * the closed faces are read from the metadata of the vanilla classes; mod blocks that extend them keep the layout.
 * <p>
 * Only whole faces count. Two partial faces that together close a square, and the corner shapes of stairs, are not
 * looked at: stairs are taken as straight.
 * <p>
 * Sides are 1.7.10's: 0 down, 1 up, 2 north (z-), 3 south (z+), 4 west (x-), 5 east (x+).
 */
public final class HolderShapes {

    public static final int NONE = 0;
    public static final int SLAB = 1;
    public static final int STAIRS = 2;
    public static final int TRAPDOOR = 3;
    public static final int LADDER = 4;

    private HolderShapes() {}

    public static int kind(Block block) {
        if (block instanceof BlockSlab) {
            return SLAB;
        }
        if (block instanceof BlockStairs) {
            return STAIRS;
        }
        if (block instanceof BlockTrapDoor) {
            return TRAPDOOR;
        }
        if (block instanceof BlockLadder) {
            return LADDER;
        }
        return NONE;
    }

    public static boolean closed(Block block, int meta, int side) {
        return closed(kind(block), meta, side);
    }

    /** Whether that face of the block is a whole square. */
    public static boolean closed(int kind, int meta, int side) {
        switch (kind) {
            case SLAB:
                // bit 8: the upper half
                return side == ((meta & 8) != 0 ? 1 : 0);
            case STAIRS:
                if (side < 2) {
                    // bit 4: upside down
                    return side == ((meta & 4) != 0 ? 1 : 0);
                }
                // 0 rises to the east, 1 to the west, 2 to the south, 3 to the north: the tall side is whole
                return side == STAIRS_BACK[meta & 3];
            case TRAPDOOR:
                if ((meta & 4) == 0) {
                    // shut: a thin slab, bit 8 says in which half
                    return side == ((meta & 8) != 0 ? 1 : 0);
                }
                return side == TRAPDOOR_OPEN[meta & 3];
            case LADDER:
                return meta >= 2 && meta <= 5 && side == LADDER_BACK[meta - 2];
            default:
                return false;
        }
    }

    private static final int[] STAIRS_BACK = { 5, 4, 3, 2 };
    /** BlockTrapDoor.func_150117_b: open, it stands against south, north, east, west. */
    private static final int[] TRAPDOOR_OPEN = { 3, 2, 5, 4 };
    /** BlockLadder.func_149797_b: metadata 2..5 stand against south, north, east, west. */
    private static final int[] LADDER_BACK = { 3, 2, 5, 4 };
}
