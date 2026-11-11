package com.akashic.neostates.client;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.NeoStatesMod;
import com.akashic.neostates.net.MessageBlockWater;
import com.akashic.neostates.net.MessageChunkWater;
import com.akashic.neostates.net.MessageConfig;
import com.akashic.neostates.net.MessageLight;
import com.akashic.neostates.water.Sections;
import com.akashic.neostates.water.WaterBits;
import com.akashic.neostates.water.WaterLayer;
import com.akashic.neostates.water.WaterSection;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

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
    private static int lightSections;

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
            if (incoming instanceof MessageConfig) {
                MessageConfig config = (MessageConfig) incoming;
                NeoConfig.followServer(config.physics, config.light, config.waterlogging);
                NeoStatesMod.LOG.info(
                    "Following the server: water physics {}, water light {}, waterlogging {}.",
                    config.physics,
                    config.light,
                    config.waterlogging);
                continue;
            }
            this.waiting.add(new Pending(incoming));
        }
        if (world == null) {
            this.waiting.clear();
            return;
        }
        for (Iterator<Pending> it = this.waiting.iterator(); it.hasNext();) {
            Pending pending = it.next();
            boolean done;
            if (pending.message instanceof MessageChunkWater) {
                done = apply(world, (MessageChunkWater) pending.message);
            } else if (pending.message instanceof MessageLight) {
                // no waiting: a chunk that is not here yet brings the new light when it comes
                apply(world, (MessageLight) pending.message);
                done = true;
            } else {
                done = apply(world, (MessageBlockWater) pending.message);
            }
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

    private static void apply(World world, MessageLight message) {
        Chunk chunk = loaded(world, message.chunkX, message.chunkZ);
        if (chunk == null) {
            return;
        }
        int x = message.chunkX << 4;
        int z = message.chunkZ << 4;
        for (int i = 0; i < message.sectionYs.length; i++) {
            int sectionY = message.sectionYs[i];
            ExtendedBlockStorage section = Sections.section(chunk, sectionY);
            if (section == null) {
                continue;
            }
            NibbleArray array = message.sky ? section.getSkylightArray() : section.getBlocklightArray();
            if (array == null || array.data.length != MessageLight.NIBBLES) {
                continue;
            }
            System.arraycopy(message.light[i], 0, array.data, 0, MessageLight.NIBBLES);
            if (lightSections++ % 256 == 0) {
                NeoStatesMod.LOG.info(
                    "Relit water light from the server: {} sections so far (chunk {},{}).",
                    lightSections,
                    message.chunkX,
                    message.chunkZ);
            }
            int y = sectionY << 4;
            // one more block around: the faces of the neighbours take their brightness from these cells
            world.markBlockRangeForRenderUpdate(x - 1, y - 1, z - 1, x + 16, y + 16, z + 16);
        }
    }

    /** Connecting and leaving, on the network thread: only flags are touched. */
    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (!event.isLocal) {
            NeoConfig.awaitServer();
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        NeoConfig.restoreFile();
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
