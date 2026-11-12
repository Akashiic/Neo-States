package com.akashic.neostates.visual;

/**
 * 1.21.1's underwater view: {@code LocalPlayer#getWaterVision} and the water branches of {@code FogRenderer}. Pure: no
 * Minecraft classes.
 */
public final class UnderwaterMath {

    /** Ticks with the eyes in water until the view is fully adapted. */
    public static final int VISION_TICKS = 600;
    /** {@code FogRenderer.setupFog}: the fog starts behind the camera. */
    public static final float FOG_START = -8.0F;
    public static final float FOG_END = 96.0F;
    /** Biomes tagged {@code has_closer_water_fog} (swamps). */
    public static final float CLOSER_FOG = 0.85F;
    /** {@code FogRenderer.setupColor}: the fog colour follows a biome change over this long. */
    public static final long BIOME_FADE_MILLIS = 5000L;

    private UnderwaterMath() {}

    /** One tick of {@code LocalPlayer.aiStep}: up 1 with the eyes in water, down 10 out of it. */
    public static int tickVision(int ticks, boolean eyesInWater) {
        int next = eyesInWater ? ticks + 1 : ticks - 10;
        return next < 0 ? 0 : Math.min(next, VISION_TICKS);
    }

    /** {@code LocalPlayer#getWaterVision}, 0..1. */
    public static float vision(int ticks, boolean eyesInWater) {
        if (!eyesInWater) {
            return 0.0F;
        }
        if (ticks >= VISION_TICKS) {
            return 1.0F;
        }
        float fast = clamp01(ticks / 100.0F);
        float slow = ticks < 100 ? 0.0F : clamp01((ticks - 100.0F) / 500.0F);
        return fast * 0.6F + slow * 0.39999998F;
    }

    /** The far end of the linear fog, before the cap of the render distance. */
    public static float fogEnd(float vision, boolean closerFog) {
        float end = FOG_END * Math.max(0.25F, vision);
        return closerFog ? end * CLOSER_FOG : end;
    }

    /** One channel of the fade between the previous and the current biome's fog colour. */
    public static float fade(float previous, float target, long elapsedMillis) {
        float t = clamp01((float) elapsedMillis / (float) BIOME_FADE_MILLIS);
        return previous + t * (target - previous);
    }

    /**
     * {@code FogRenderer.setupColor}: the colour is pushed towards its brightest form (one channel at 1) by the
     * adapted view. Writes into {@code rgb}.
     */
    public static void brighten(float[] rgb, float vision) {
        if (rgb[0] == 0.0F || rgb[1] == 0.0F || rgb[2] == 0.0F) {
            return;
        }
        float scale = Math.min(1.0F / rgb[0], Math.min(1.0F / rgb[1], 1.0F / rgb[2]));
        for (int i = 0; i < 3; i++) {
            rgb[i] = rgb[i] * (1.0F - vision) + rgb[i] * scale * vision;
        }
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
    }
}
