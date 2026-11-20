package com.akashic.neostates.visual;

/**
 * The window in which the liquid renderer is drawing a water block and multiplying the sprite by the biome colour.
 * <p>
 * Only inside that window is the grey sprite the right one: the renderer puts the whole colour in the texture, as in
 * 1.21.1. Everywhere else the sprite is drawn as it is — a GUI, WAILA/WDMla, NEI, an item preview, a flat draw — and a
 * grey sprite with nothing to multiply it by comes out grey, so those callers have to get the blue one.
 * <p>
 * The window is per thread and nests: the faces of one block are drawn inside one call, and the chunk renderers
 * (Angelica, Beddium, the vanilla one) mesh on their own threads, each with its own window.
 */
public final class BiomeTintScope {

    private static final ThreadLocal<int[]> depth = new ThreadLocal<int[]>();

    private BiomeTintScope() {}

    public static void begin() {
        int[] d = depth.get();
        if (d == null) {
            d = new int[1];
            depth.set(d);
        }
        d[0]++;
    }

    public static void end() {
        int[] d = depth.get();
        if (d == null) {
            return;
        }
        if (--d[0] <= 0) {
            d[0] = 0;
            depth.remove();
        }
    }

    public static boolean active() {
        int[] d = depth.get();
        return d != null && d[0] > 0;
    }
}
