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
    /** Water takes 1 of light per block as in 1.21.1, instead of 3. */
    public static boolean modernWaterLight = true;
    /** Chunks lit before the water became clearer are relit once, as they load. */
    public static boolean relightOldChunks = true;
    /** Server time the relighting may take per world tick. */
    public static int relightMillisPerTick = 2;
    /** Blocks may hold water. Off: no block is a holder, and water already held stays stored but has no effect. */
    public static boolean waterlogging = true;

    // What config/neostates.cfg says. The three fields above that both sides must agree on (physics, light,
    // waterlogging) follow the server while a client is connected to one.
    private static boolean filePhysics = true;
    private static boolean fileLight = true;
    private static boolean fileWaterlogging = true;

    private NeoConfig() {}

    /** Client: the server said how it runs. */
    public static void followServer(boolean physics, boolean light, boolean holds) {
        modernWaterPhysics = physics;
        modernWaterLight = light;
        waterlogging = holds;
        com.akashic.neostates.light.WaterLight.apply(light);
    }

    /** Client: connecting to a remote server. Until it says otherwise, it is taken not to have the mod. */
    public static void awaitServer() {
        followServer(false, false, false);
    }

    /** Client: back to what the file says. */
    public static void restoreFile() {
        followServer(filePhysics, fileLight, fileWaterlogging);
    }

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
                + " pulo no raso, descer agachado. Num servidor, vale o valor do servidor.");
        modernWaterLight = config.getBoolean(
            "modernWaterLight",
            "physics",
            true,
            "A agua tira 1 de luz por bloco, como no 1.21.1 (no 1.7.10 tira 3). Num servidor, vale o valor do"
                + " servidor.");
        relightOldChunks = config.getBoolean(
            "relightOldChunks",
            "physics",
            true,
            "Recalcula uma unica vez, ao carregar, a luz da agua dos chunks gerados antes de a agua ficar mais clara."
                + " So clareia: desligar modernWaterLight depois nao escurece o que ja foi refeito.");
        relightMillisPerTick = config.getInt(
            "relightMillisPerTick",
            "physics",
            2,
            1,
            40,
            "Tempo maximo, em milissegundos por tick de cada mundo, gasto recalculando a luz de chunks antigos.");
        waterlogging = config.getBoolean(
            "waterlogging",
            "state",
            true,
            "Lajes, escadas, cercas, muros, paineis, alcapoes, escadas de mao, placas e baus podem conter agua. Num"
                + " servidor, vale o valor do servidor.");
        filePhysics = modernWaterPhysics;
        fileLight = modernWaterLight;
        fileWaterlogging = waterlogging;
        if (config.hasChanged()) {
            config.save();
        }
    }
}
