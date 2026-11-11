package com.akashic.neostates.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkWatchEvent;

import com.akashic.neostates.NeoStatesMod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * Our own packets for the layer (decision of phase 1: no ChunkAPI). The chunk's layer follows the chunk when a player
 * starts watching it; a changed cell goes to whoever watches its chunk.
 */
public final class WaterNet {

    private static SimpleNetworkWrapper channel;

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(NeoStatesMod.MODID);
        channel.registerMessage(MessageChunkWater.Handler.class, MessageChunkWater.class, 0, Side.CLIENT);
        channel.registerMessage(MessageBlockWater.Handler.class, MessageBlockWater.class, 1, Side.CLIENT);
    }

    @SubscribeEvent
    public void onWatch(ChunkWatchEvent.Watch event) {
        EntityPlayerMP player = event.player;
        World world = player.worldObj;
        int cx = event.chunk.chunkXPos;
        int cz = event.chunk.chunkZPos;
        if (!world.getChunkProvider()
            .chunkExists(cx, cz)) {
            return;
        }
        Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
        MessageChunkWater message = MessageChunkWater.of(chunk);
        if (message != null) {
            channel.sendTo(message, player);
        }
    }

    /** How far a single-cell change is sent, in blocks: past any view distance worth drawing a block at. */
    private static final double BLOCK_RANGE = 192.0D;

    /**
     * Sent by distance, not by "is watching the chunk": a client that does not have the chunk yet gets the whole
     * layer with it, and one that has it applies the change. It does not depend on the server's chunk watcher lists,
     * which Crucible and Hodgepodge both rework.
     */
    public static void sendBlock(World world, int x, int y, int z, boolean water) {
        if (world.isRemote) {
            return;
        }
        channel.sendToAllAround(
            new MessageBlockWater(x, y, z, water),
            new NetworkRegistry.TargetPoint(world.provider.dimensionId, x + 0.5D, y + 0.5D, z + 0.5D, BLOCK_RANGE));
    }
}
