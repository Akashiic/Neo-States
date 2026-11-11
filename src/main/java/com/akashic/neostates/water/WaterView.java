package com.akashic.neostates.water;

/**
 * Implemented by mixin on the block accesses the renderers read through ({@code ChunkCache}, Angelica's
 * {@code WorldSlice}): whether a cell holds water, from that access's own copy of the world.
 */
public interface WaterView {

    boolean neostates$water(int x, int y, int z);
}
