package com.akashic.neostates.client;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.akashic.neostates.NeoStatesMod;
import com.akashic.neostates.net.MessageBlockWater;
import com.akashic.neostates.net.MessageChunkWater;
import com.akashic.neostates.water.Sections;
import com.akashic.neostates.water.WaterBits;
import com.akashic.neostates.water.WaterLayer;
import com.akashic.neostates.water.WaterSection;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Applies the layer on the client. Forge hands these messages over on the network thread, before the chunk packet
 * they follow has been processed by the client thread, so they wait here: each is applied on a client tick once its
 * chunk (and, below Y 0, its deep section) is there, and dropped after a while if it never comes.
 */
public final class ClientWaterSync {

    /** How long a message waits for its chunk or section, in ticks. */
    private static final int PATIENCE = 200;

    private final ConcurrentLinkedQueue<Object> inbox = new ConcurrentLinkedQueue<>();
    private final ArrayDeque<Pending> waiting = new ArrayDeque<>();
    private World lastWorld;

    private static final class Pending {

        final Object message;
        int age;

        Pending(Object message) {
            this.message = message;
        }
    }

    public void receive(Object message) {
        this.inbox.add(message);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        World world = Minecraft.getMinecraft().theWorld;
        if (world != this.lastWorld) {
            this.waiting.clear();
            this.lastWorld = world;
        }
        Object incoming;
        while ((incoming = this.inbox.poll()) != null) {
            this.waiting.add(new Pending(incoming));
        }
        if (world == null) {
            this.waiting.clear();
            return;
        }
        for (Iterator<Pending> it = this.waiting.iterator(); it.hasNext();) {
            Pending pending = it.next();
            boolean done = pending.message instanceof MessageChunkWater
                ? apply(world, (MessageChunkWater) pending.message)
                : apply(world, (MessageBlockWater) pending.message);
            if (done) {
                it.remove();
            } else if (++pending.age > PATIENCE) {
                // a single cell out of the loaded area is normal: the chunk brings its whole layer when it comes
                if (pending.message instanceof MessageChunkWater) {
                    NeoStatesMod.LOG.warn("Dropped a chunk's waterlogging layer: the chunk never arrived.");
                }
                it.remove();
            }
        }
    }

    private static Chunk loaded(World world, int chunkX, int chunkZ) {
        if (!world.getChunkProvider()
            .chunkExists(chunkX, chunkZ)) {
            return null;
        }
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        return chunk instanceof EmptyChunk ? null : chunk;
    }

    private static boolean apply(World world, MessageChunkWater message) {
        Chunk chunk = loaded(world, message.chunkX, message.chunkZ);
        if (chunk == null) {
            return false;
        }
        for (byte sectionY : message.sectionYs) {
            if (Sections.section(chunk, sectionY) == null) {
                return false;
            }
        }
        for (int i = 0; i < message.sectionYs.length; i++) {
            int sectionY = message.sectionYs[i];
            ExtendedBlockStorage section = Sections.section(chunk, sectionY);
            ((WaterSection) section).neostates$setWater(WaterBits.fromBytes(message.bits[i]));
            int x = message.chunkX << 4;
            int y = sectionY << 4;
            int z = message.chunkZ << 4;
            world.markBlockRangeForRenderUpdate(x, y, z, x + 15, y + 15, z + 15);
        }
        NeoStatesMod.LOG.info(
            "Waterlogging layer of chunk {},{}: {} sections.",
            message.chunkX,
            message.chunkZ,
            message.sectionYs.length);
        return true;
    }

    private static boolean apply(World world, MessageBlockWater message) {
        Chunk chunk = loaded(world, message.x >> 4, message.z >> 4);
        if (chunk == null || Sections.section(chunk, message.y >> 4) == null) {
            return false;
        }
        WaterLayer.set(chunk, message.x, message.y, message.z, message.water);
        NeoStatesMod.LOG.info("Waterlogging at {} {} {}: {}.", message.x, message.y, message.z, message.water);
        world.markBlockForUpdate(message.x, message.y, message.z);
        return true;
    }
}
