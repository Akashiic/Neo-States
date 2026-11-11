package com.akashic.neostates.mixin.early.common;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.akashic.neostates.water.WaterLayer;
import com.akashic.neostates.water.WaterView;

/** The block access of vanilla's chunk rendering and of pathfinding answers for the waterlogging layer. */
@Mixin(ChunkCache.class)
public abstract class MixinChunkCache_Water implements WaterView {

    @Shadow
    private int chunkX;

    @Shadow
    private int chunkZ;

    @Shadow
    private Chunk[][] chunkArray;

    @Override
    public boolean neostates$water(int x, int y, int z) {
        int cx = (x >> 4) - this.chunkX;
        int cz = (z >> 4) - this.chunkZ;
        if (cx < 0 || cx >= this.chunkArray.length || cz < 0 || cz >= this.chunkArray[cx].length) {
            return false;
        }
        Chunk chunk = this.chunkArray[cx][cz];
        return chunk != null && WaterLayer.get(chunk, x, y, z);
    }
}
