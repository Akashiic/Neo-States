package com.akashic.neostates.visual;

/**
 * The water colours of 1.21.1 ({@code data/minecraft/worldgen/biome/*.json}) and the arithmetic on them. Pure: no
 * Minecraft classes.
 */
public final class WaterPalette {

    /** 50 of the 64 biomes of 1.21.1. */
    public static final int WATER = 0x3F76E4;
    public static final int FOG = 0x050533;

    /** cold_ocean, deep_cold_ocean, snowy_beach, snowy_taiga. */
    public static final int WATER_COLD = 0x3D57D6;
    /** frozen_ocean, deep_frozen_ocean, frozen_river. */
    public static final int WATER_FROZEN = 0x3938C9;
    public static final int WATER_SWAMP = 0x617B64;
    public static final int FOG_SWAMP = 0x232317;

    /** What 1.7.10 biomes multiply the (blue) texture by when they change nothing. */
    public static final int LEGACY_NEUTRAL = 0xFFFFFF;

    private WaterPalette() {}

    /** Channel-wise {@code a * b / 255}: a legacy multiplier applied to a modern colour. */
    public static int multiply(int a, int b) {
        int r = (a >> 16 & 255) * (b >> 16 & 255) / 255;
        int g = (a >> 8 & 255) * (b >> 8 & 255) / 255;
        int bl = (a & 255) * (b & 255) / 255;
        return r << 16 | g << 8 | bl;
    }

    /** The modern colour of a biome 1.21.1 has no entry for: the default, under the biome's own legacy multiplier. */
    public static int fromLegacyMultiplier(int legacyMultiplier) {
        int m = legacyMultiplier & 0xFFFFFF;
        return m == LEGACY_NEUTRAL ? WATER : multiply(WATER, m);
    }
}
