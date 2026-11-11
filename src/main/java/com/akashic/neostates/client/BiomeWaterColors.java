package com.akashic.neostates.client;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenMutated;
import net.minecraftforge.common.BiomeDictionary;

import com.akashic.neostates.visual.WaterPalette;

/**
 * The 1.21.1 water and water-fog colour of each 1.7.10 biome. Built once on the main thread; reads are plain array
 * loads, because the water colour is asked from the chunk meshing threads.
 * <p>
 * A vanilla biome gets the colour of the 1.21.1 biome it became. A biome of a mod gets the swamp colours when the
 * dictionary calls it a swamp, else the default under its own legacy multiplier.
 */
public final class BiomeWaterColors {

    private static volatile int[] water = new int[0];
    private static volatile int[] fog = new int[0];
    private static volatile boolean[] closerFog = new boolean[0];

    private BiomeWaterColors() {}

    public static void rebuild() {
        BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();
        int[] w = new int[biomes.length];
        int[] f = new int[biomes.length];
        boolean[] c = new boolean[biomes.length];
        for (int id = 0; id < biomes.length; id++) {
            BiomeGenBase biome = biomes[id];
            if (biome == null) {
                w[id] = -1;
                continue;
            }
            BiomeGenBase base = baseOf(biome, biomes);
            boolean swamp = base == BiomeGenBase.swampland || isSwamp(biome);
            if (swamp) {
                w[id] = WaterPalette.WATER_SWAMP;
                f[id] = WaterPalette.FOG_SWAMP;
                c[id] = true;
            } else if (base == BiomeGenBase.frozenOcean || base == BiomeGenBase.frozenRiver) {
                w[id] = WaterPalette.WATER_FROZEN;
                f[id] = WaterPalette.FOG;
            } else if (base == BiomeGenBase.coldBeach || base == BiomeGenBase.coldTaiga
                || base == BiomeGenBase.coldTaigaHills) {
                    w[id] = WaterPalette.WATER_COLD;
                    f[id] = WaterPalette.FOG;
                } else {
                    w[id] = WaterPalette.fromLegacyMultiplier(biome.waterColorMultiplier);
                    f[id] = WaterPalette.FOG;
                }
        }
        water = w;
        fog = f;
        closerFog = c;
    }

    /** @param legacyMultiplier what the biome would multiply the blue texture by, for a biome not in the table */
    public static int water(BiomeGenBase biome, int legacyMultiplier) {
        int[] table = water;
        int id = biome.biomeID;
        if (id >= 0 && id < table.length && table[id] >= 0) {
            return table[id];
        }
        return WaterPalette.fromLegacyMultiplier(legacyMultiplier);
    }

    public static int fog(BiomeGenBase biome) {
        int[] table = fog;
        int id = biome.biomeID;
        return id >= 0 && id < table.length && water[id] >= 0 ? table[id] : WaterPalette.FOG;
    }

    public static boolean closerFog(BiomeGenBase biome) {
        boolean[] table = closerFog;
        int id = biome.biomeID;
        return id >= 0 && id < table.length && table[id];
    }

    /** A mutated biome ("M" variants, id + 128) counts as the biome it mutates. */
    private static BiomeGenBase baseOf(BiomeGenBase biome, BiomeGenBase[] biomes) {
        if (biome instanceof BiomeGenMutated && biome.biomeID >= 128 && biomes[biome.biomeID - 128] != null) {
            return biomes[biome.biomeID - 128];
        }
        return biome;
    }

    private static boolean isSwamp(BiomeGenBase biome) {
        try {
            return BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.SWAMP);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
