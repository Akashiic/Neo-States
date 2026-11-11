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
 * The queue is not saved: after a restart a hosted source waits for the next neighbour change to flow again.
 * Crucible's {@code BlockFromToEvent} is not fired yet for this flow (docs/COMPATIBILIDADE.md).
 */
public final class WaterTicks {

    /** Water's tick rate. */
    public static final int DELAY = 5;
    private static final int MAX_PER_TICK = 1000;

    private static final Map<World, LinkedHashMap<ChunkCoordinates, Long>> queues = new WeakHashMap<>();

    public static void schedule(World world, int x, int y, int z) {
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
            queue.put(pos, world.getTotalWorldTime() + DELAY);
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

    /** For the self-test: the tick of a hosted source, without waiting for the queue. */
    public static void spreadNow(World world, int x, int y, int z) {
        spread(world, x, y, z);
    }

    /** One tick of a hosted source: down if it can, else to the sides vanilla would pick. */
    static void spread(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z) || !WaterLayer.get(world, x, y, z)) {
            return;
        }
        Block host = world.getBlock(x, y, z);
        if (!WaterHolders.canHold(host)) {
            return;
        }
        BlockDynamicLiquid water = (BlockDynamicLiquid) Blocks.flowing_water;
        BlockDynamicLiquidInvoker flow = (BlockDynamicLiquidInvoker) water;
        if (flow.neostates$canFlowInto(world, x, y - 1, z)) {
            // a falling column, as a source over a hole makes
            flow.neostates$flowInto(world, x, y - 1, z, 8);
            return;
        }
        boolean[] sides = flow.neostates$optimalSides(world, x, y, z);
        boolean west = sides[0];
        boolean east = sides[1];
        boolean north = sides[2];
        boolean south = sides[3];
        if (west) flow.neostates$flowInto(world, x - 1, y, z, 1);
        if (east) flow.neostates$flowInto(world, x + 1, y, z, 1);
        if (north) flow.neostates$flowInto(world, x, y, z - 1, 1);
        if (south) flow.neostates$flowInto(world, x, y, z + 1, 1);
    }
}
