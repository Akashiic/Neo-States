package com.akashic.neostates.water;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkEvent;

import com.akashic.neostates.api.NeoStates;
import com.akashic.neostates.mixin.early.common.BlockDynamicLiquidInvoker;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The spreading of hosted water. A scheduled block tick for water is dropped by the world when the cell's block is
 * not water, so hosted water has its own queue: a cell is queued when it gains water or a neighbour changes, and
 * {@link #DELAY} ticks later it behaves as the source it is.
 * <p>
 * It never writes into its own cell. Whether and where the water may go is asked of vanilla's own flowing water
 * ({@code func_149809_q}, {@code func_149808_o}), and the neighbour is written by vanilla's {@code func_149813_h}.
 * <p>
 * A holder with no water is queued the same way when a neighbour changes, and its tick applies 1.21.1's rule for
 * new sources: between two sources, over a solid block or a source, it fills up.
 * <p>
 * The queue is not saved. Instead every hosted source of a chunk is queued when the chunk loads, which gives the same
 * result (a source that has nowhere to go does nothing) without a second thing to keep in step with the layer. On a
 * Bukkit server every step asks {@link BukkitFlow} first, as vanilla's flowing water does.
 */
public final class WaterTicks {

    /** Water's tick rate. */
    public static final int DELAY = 5;
    private static final int MAX_PER_TICK = 1000;
    /** A cell whose surroundings are not loaded waits this long before it is looked at again. */
    private static final int RETRY = 40;

    private static final Map<World, LinkedHashMap<ChunkCoordinates, Long>> queues = new WeakHashMap<>();

    public static void schedule(World world, int x, int y, int z) {
        schedule(world, x, y, z, DELAY);
    }

    private static void schedule(World world, int x, int y, int z, int delay) {
        if (world.isRemote) {
            return;
        }
        LinkedHashMap<ChunkCoordinates, Long> queue = queues.get(world);
        if (queue == null) {
            queue = new LinkedHashMap<>();
            queues.put(world, queue);
        }
        ChunkCoordinates pos = new ChunkCoordinates(x, y, z);
        if (!queue.containsKey(pos)) {
            queue.put(pos, world.getTotalWorldTime() + delay);
        }
    }

    /** A chunk came in: its hosted sources flow again if something changed around them while it was away. */
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        World world = event.world;
        Chunk chunk = event.getChunk();
        if (world == null || world.isRemote || chunk instanceof EmptyChunk) {
            return;
        }
        for (int sectionY = Sections.minSectionY(); sectionY < 16; sectionY++) {
            ExtendedBlockStorage section = Sections.section(chunk, sectionY);
            if (section == null) {
                continue;
            }
            long[] bits = ((WaterSection) section).neostates$getWater();
            if (bits == null) {
                continue;
            }
            for (int index = 0; index < WaterBits.CELLS; index++) {
                if (WaterBits.get(bits, index)) {
                    schedule(
                        world,
                        (chunk.xPosition << 4) + WaterBits.x(index),
                        (sectionY << 4) + WaterBits.y(index),
                        (chunk.zPosition << 4) + WaterBits.z(index),
                        RETRY);
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        LinkedHashMap<ChunkCoordinates, Long> queue = queues.get(event.world);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        long now = event.world.getTotalWorldTime();
        List<ChunkCoordinates> due = new ArrayList<>();
        for (Iterator<Map.Entry<ChunkCoordinates, Long>> it = queue.entrySet()
            .iterator(); it.hasNext() && due.size() < MAX_PER_TICK;) {
            Map.Entry<ChunkCoordinates, Long> entry = it.next();
            if (entry.getValue() <= now) {
                due.add(entry.getKey());
                it.remove();
            }
        }
        for (ChunkCoordinates pos : due) {
            spread(event.world, pos.posX, pos.posY, pos.posZ);
        }
    }

    private static final int[] SIDE_X = { -1, 1, 0, 0 };
    private static final int[] SIDE_Z = { 0, 0, -1, 1 };
    /** The side of the source at that offset that faces the cell: east, west, south, north. */
    private static final int[] SIDE_FACE = { 5, 4, 3, 2 };
    /** The side of the cell that faces the source at that offset: west, east, north, south. */
    private static final int[] SIDE_BACK = { 4, 5, 2, 3 };

    /** Whether water can go from the source at that offset into the cell: no whole face in between. */
    private static boolean opens(World world, int x, int y, int z, Block host, int meta, int side) {
        if (HolderShapes.closed(host, meta, SIDE_BACK[side])) {
            return false;
        }
        int sx = x + SIDE_X[side];
        int sz = z + SIDE_Z[side];
        Block source = world.getBlock(sx, y, sz);
        return !HolderShapes.closed(source, world.getBlockMetadata(sx, y, sz), SIDE_FACE[side]);
    }

    /** Whether the cell is a water source: a still or flowing water block at level 0, or a block that holds water. */
    public static boolean isSource(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) {
            return false;
        }
        Block block = world.getBlock(x, y, z);
        if (block == Blocks.water || block == Blocks.flowing_water) {
            return world.getBlockMetadata(x, y, z) == 0;
        }
        return WaterHolders.canHold(block) && WaterLayer.get(world, x, y, z);
    }

    /**
     * 1.21.1's {@code FlowingFluid.getNewLiquid} for a block that can hold water: with two sources beside it, and a
     * solid block or a source under it, the fluid that would form there is a source, and a source is the one thing
     * such a block accepts.
     */
    private static void fill(World world, int x, int y, int z) {
        int sources = 0;
        int from = -1;
        Block host = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        for (int side = 0; side < 4; side++) {
            if (isSource(world, x + SIDE_X[side], y, z + SIDE_Z[side]) && opens(world, x, y, z, host, meta, side)) {
                sources++;
                if (from < 0) {
                    from = side;
                }
            }
        }
        if (sources < 2) {
            return;
        }
        if (!world.getBlock(x, y - 1, z)
            .getMaterial()
            .isSolid() && !isSource(world, x, y - 1, z)) {
            return;
        }
        if (BukkitFlow.allows(world, x + SIDE_X[from], y, z + SIDE_Z[from], SIDE_FACE[from])) {
            NeoStates.setWaterlogged(world, x, y, z, true);
        }
    }

    /** For the self-test: the tick of a hosted source, without waiting for the queue. */
    public static void spreadNow(World world, int x, int y, int z) {
        spread(world, x, y, z);
    }

    /** One tick of a hosted source: down if it can, else to the sides vanilla would pick. */
    static void spread(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) {
            return;
        }
        Block host = world.getBlock(x, y, z);
        if (!WaterHolders.canHold(host)) {
            return;
        }
        if (!world.checkChunksExist(x - 1, y, z - 1, x + 1, y, z + 1)) {
            // as the world does with a block tick at the edge of what is loaded: later, never by loading a chunk
            schedule(world, x, y, z, RETRY);
            return;
        }
        if (!WaterLayer.get(world, x, y, z)) {
            fill(world, x, y, z);
            return;
        }
        for (int side = 0; side < 4; side++) {
            int nx = x + SIDE_X[side];
            int nz = z + SIDE_Z[side];
            if (!WaterLayer.get(world, nx, y, nz) && WaterHolders.canHold(world.getBlock(nx, y, nz))) {
                schedule(world, nx, y, nz); // a dry holder beside this source: it may be between two now
            }
        }
        BlockDynamicLiquid water = (BlockDynamicLiquid) Blocks.flowing_water;
        BlockDynamicLiquidInvoker flow = (BlockDynamicLiquidInvoker) water;
        int meta = world.getBlockMetadata(x, y, z);
        int shape = HolderShapes.kind(host);
        if (!HolderShapes.closed(shape, meta, 0) && flow.neostates$canFlowInto(world, x, y - 1, z)) {
            // a falling column, as a source over a hole makes
            if (BukkitFlow.allows(world, x, y, z, 0)) {
                flow.neostates$flowInto(world, x, y - 1, z, 8);
            }
            return;
        }
        boolean[] sides = flow.neostates$optimalSides(world, x, y, z);
        // the water leaves through the faces the block leaves open (1.21.1's canPassThroughWall)
        boolean west = sides[0] && !HolderShapes.closed(shape, meta, 4);
        boolean east = sides[1] && !HolderShapes.closed(shape, meta, 5);
        boolean north = sides[2] && !HolderShapes.closed(shape, meta, 2);
        boolean south = sides[3] && !HolderShapes.closed(shape, meta, 3);
        if (west && BukkitFlow.allows(world, x, y, z, 4)) flow.neostates$flowInto(world, x - 1, y, z, 1);
        if (east && BukkitFlow.allows(world, x, y, z, 5)) flow.neostates$flowInto(world, x + 1, y, z, 1);
        if (north && BukkitFlow.allows(world, x, y, z, 2)) flow.neostates$flowInto(world, x, y, z - 1, 1);
        if (south && BukkitFlow.allows(world, x, y, z, 3)) flow.neostates$flowInto(world, x, y, z + 1, 1);
    }
}
