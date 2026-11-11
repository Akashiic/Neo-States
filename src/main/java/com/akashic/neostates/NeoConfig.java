package com.akashic.neostates;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** {@code config/neostates.cfg}. Every layer can be turned off on its own. */
public final class NeoConfig {

    /** Grey water texture + 1.21.1 colours per biome. The two go together: one without the other looks wrong. */
    public static boolean modernWaterColor = true;
    /** 1.21.1 underwater fog: biome colour, linear distance, the view adapting over time. */
    public static boolean modernWaterFog = true;

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
        if (config.hasChanged()) {
            config.save();
        }
    }
}
