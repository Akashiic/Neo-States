package com.akashic.neostates.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrayscaleFilterTest {

    @Test
    void blueWaterBecomesGreyAtTheModernMean() {
        // two pixels around 1.7.10's mean water colour, different alpha
        int[] pixels = { 0xB32F43F4, 0xA8203AE0 };
        assertTrue(GrayscaleFilter.apply(pixels, GrayscaleFilter.STILL_MEAN));
        long sum = 0;
        for (int p : pixels) {
            int r = p >> 16 & 255;
            assertEquals(r, p >> 8 & 255);
            assertEquals(r, p & 255);
            sum += r;
        }
        assertEquals(GrayscaleFilter.STILL_MEAN, sum / 2.0, 1.0);
        assertEquals(0xB3, pixels[0] >>> 24);
        assertEquals(0xA8, pixels[1] >>> 24);
    }

    @Test
    void aGreyTextureIsLeftAlone() {
        int[] pixels = { 0xB4B1B1B1, 0xB4A0A0A0 };
        assertFalse(GrayscaleFilter.apply(pixels, GrayscaleFilter.STILL_MEAN));
        assertEquals(0xB4B1B1B1, pixels[0]);
    }

    @Test
    void fullyTransparentPixelsDoNotCount() {
        int[] pixels = { 0x00FFFFFF, 0xFF0000FF };
        assertTrue(GrayscaleFilter.apply(pixels, 100));
        assertEquals(100, pixels[1] & 255);
    }
}
