package com.akashic.neostates.water;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWall;
import net.minecraft.init.Blocks;

/**
 * Which blocks may hold water in their cell. The defaults are the 1.7.10 classes whose 1.21.1 descendants are
 * waterloggable and that vanilla water does not wash away; a full opaque cube (a double slab) never holds water.
 * Mod blocks that extend these classes (Et Futurum's slabs, stairs, walls, fences, trapdoors and signs) come along.
 * <p>
 * Rails, also waterloggable in 1.21.1, are left out for now: 1.7.10's flowing water breaks them, and that has to be
 * changed first.
 */
public final class WaterHolders {

    private static final Class<?>[] DEFAULTS = { BlockSlab.class, BlockStairs.class, BlockFence.class, BlockPane.class,
        BlockWall.class, BlockTrapDoor.class, BlockLadder.class, BlockSign.class, BlockChest.class,
        BlockEnderChest.class };

    private WaterHolders() {}

    public static boolean canHold(Block block) {
        if (block == Blocks.air) {
            return false;
        }
        HolderFlag flag = (HolderFlag) block;
        byte known = flag.neostates$holder();
        if (known == 0) {
            known = compute(block) ? (byte) 2 : (byte) 1;
            flag.neostates$holder(known);
        }
        return known == 2;
    }

    private static boolean compute(Block block) {
        if (block.getMaterial()
            .isLiquid() || block.isOpaqueCube()) {
            return false;
        }
        for (Class<?> type : DEFAULTS) {
            if (type.isInstance(block)) {
                return true;
            }
        }
        return false;
    }
}
