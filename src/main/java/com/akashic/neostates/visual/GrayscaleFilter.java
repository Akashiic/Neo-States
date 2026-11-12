package com.akashic.neostates.visual;

/**
 * Turns the water texture of the active resource pack into the grey one 1.21.1 ships, without shipping Mojang's file:
 * each pixel becomes its luminance, scaled so the image's mean matches the modern texture. Alpha is kept (1.7.10's
 * mean alpha is 179, the modern texture is a flat 180). Pure: no Minecraft classes.
 */
public final class GrayscaleFilter {

    /** Mean grey of 1.21.1's {@code water_still.png}. */
    public static final int STILL_MEAN = 177;
    /** Mean grey of 1.21.1's {@code water_flow.png}. */
    public static final int FLOW_MEAN = 169;

    private GrayscaleFilter() {}

    /** @return false when the image was already grey (a pack with modern-style water) and was left alone */
    public static boolean apply(int[] argb, int targetMean) {
        long sum = 0;
        int counted = 0;
        boolean grey = true;
        for (int pixel : argb) {
            if (pixel >>> 24 == 0) {
                continue;
            }
            int r = pixel >> 16 & 255;
            int g = pixel >> 8 & 255;
            int b = pixel & 255;
            if (r != g || g != b) {
                grey = false;
            }
            sum += luminance(r, g, b);
            counted++;
        }
        if (grey || counted == 0 || sum == 0) {
            return false;
        }
        double scale = targetMean / ((double) sum / counted);
        for (int i = 0; i < argb.length; i++) {
            int pixel = argb[i];
            int value = (int) Math.round(luminance(pixel >> 16 & 255, pixel >> 8 & 255, pixel & 255) * scale);
            value = value < 0 ? 0 : Math.min(value, 255);
            argb[i] = pixel & 0xFF000000 | value << 16 | value << 8 | value;
        }
        return true;
    }

    static int luminance(int r, int g, int b) {
        return (r * 299 + g * 587 + b * 114) / 1000;
    }
}
