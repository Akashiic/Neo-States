package com.akashic.neostates;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** {@code config/neostates.cfg}. Every layer can be turned off on its own. */
public final class NeoConfig {

    /** Grey water texture + 1.21.1 colours per biome. The two go together: one without the other looks wrong. */
    public static boolean modernWaterColor = true;
    /** 1.21.1 underwater fog: biome colour, linear distance, the view adapting over time. */
    public static boolean modernWaterFog = true;

    /** 1.21.1 water physics: "in water" by the real surface, the current's push, sinking, the jump rule, sneaking. */
    public static boolean modernWaterPhysics = true;
    /** Water takes 1 of light per block as in 1.21.1, instead of 3. Old chunks keep their light until relit. */
    public static boolean modernWaterLight = true;
    /** Blocks may hold water. Off: no block is a holder, and water already held stays stored but has no effect. */
    public static boolean waterlogging = true;

    private NeoConfig() {}

    public static void load(File file) {
        Configuration config = new Configuration(file);
        modernWaterColor = config.getBoolean(
            "modernWaterColor",
            "visual",
            true,
            "Agua com a textura em cinza e a cor de cada bioma, como no 1.21.1.");
        modernWaterFog = config.getBoolean(
            "modernWaterFog",
            "visual",
            true,
            "Nevoa submersa do 1.21.1: cor do bioma, distancia linear e visao que se adapta com o tempo.");
        modernWaterPhysics = config.getBoolean(
            "modernWaterPhysics",
            "physics",
            true,
            "Fisica da agua do 1.21.1: estar na agua pela superficie real, empurrao da correnteza, afundar mais devagar,"
                + " pulo no raso, descer agachado. Use o mesmo valor no servidor e nos clientes.");
        modernWaterLight = config.getBoolean(
            "modernWaterLight",
            "physics",
            true,
            "A agua tira 1 de luz por bloco, como no 1.21.1 (no 1.7.10 tira 3). Use o mesmo valor no servidor e nos"
                + " clientes. Chunks antigos so mudam quando a luz for recalculada.");
        waterlogging = config.getBoolean(
            "waterlogging",
            "state",
            true,
            "Lajes, escadas, cercas, muros, paineis, alcapoes, escadas de mao, placas e baus podem conter agua.");
        if (config.hasChanged()) {
            config.save();
        }
    }
}
