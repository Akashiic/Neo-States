package com.akashic.neostates.light;

import net.minecraft.init.Blocks;

/** Water's light opacity: 1 per block in 1.21.1, 3 in 1.7.10. Both sides compute light, so both need the same. */
public final class WaterLight {

    public static final int MODERN = 1;

    private static int originalStill = -1;
    private static int originalFlowing;

    private WaterLight() {}

    /** Remembers what the game (or another mod) had set, then applies the option. */
    public static void init(boolean modern) {
        originalStill = Blocks.water.getLightOpacity();
        originalFlowing = Blocks.flowing_water.getLightOpacity();
        apply(modern);
    }

    public static void apply(boolean modern) {
        if (originalStill < 0) {
            return;
        }
        Blocks.water.setLightOpacity(modern ? Math.min(MODERN, originalStill) : originalStill);
        Blocks.flowing_water.setLightOpacity(modern ? Math.min(MODERN, originalFlowing) : originalFlowing);
    }

    /** The opacity light is being computed with right now, as a chunk's mark. */
    public static byte current() {
        return (byte) Math.max(1, Math.min(15, Blocks.water.getLightOpacity()));
    }
}
