package com.akashic.neostates.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkWatchEvent;

import com.akashic.neostates.NeoStatesMod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
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
        channel.registerMessage(MessageLight.Handler.class, MessageLight.class, 2, Side.CLIENT);
        channel.registerMessage(MessageConfig.Handler.class, MessageConfig.class, 3, Side.CLIENT);
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

    /** The server's options go to each player as they come in: the client follows them while connected. */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            channel.sendTo(MessageConfig.ofServer(), (EntityPlayerMP) event.player);
        }
    }

    /**
     * Relit sections, to the players close enough to have the chunk. One that does not have it yet ignores this and
     * gets the new light with the chunk.
     */
    public static void sendLight(World world, MessageLight message) {
        if (world.isRemote || message == null || world.playerEntities.isEmpty()) {
            return;
        }
        int reach = MinecraftServer.getServer()
            .getConfigurationManager()
            .getViewDistance() + 2;
        for (Object each : world.playerEntities) {
            if (!(each instanceof EntityPlayerMP)) {
                continue;
            }
            EntityPlayerMP player = (EntityPlayerMP) each;
            int dx = (MathHelper.floor_double(player.posX) >> 4) - message.chunkX;
            int dz = (MathHelper.floor_double(player.posZ) >> 4) - message.chunkZ;
            if (Math.abs(dx) <= reach && Math.abs(dz) <= reach) {
                channel.sendTo(message, player);
            }
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
