package com.akashic.neostates.visual;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnderwaterMathTest {

    @Test
    void visionCurveOfLocalPlayer() {
        assertEquals(0.0F, UnderwaterMath.vision(300, false), 0.0F);
        assertEquals(0.0F, UnderwaterMath.vision(0, true), 0.0F);
        assertEquals(0.3F, UnderwaterMath.vision(50, true), 1.0E-6F);
        assertEquals(0.6F, UnderwaterMath.vision(100, true), 1.0E-6F);
        assertEquals(0.6F + 0.5F * 0.39999998F, UnderwaterMath.vision(350, true), 1.0E-6F);
        assertEquals(1.0F, UnderwaterMath.vision(600, true), 0.0F);
    }

    @Test
    void visionRisesByOneAndFallsByTen() {
        assertEquals(1, UnderwaterMath.tickVision(0, true));
        assertEquals(600, UnderwaterMath.tickVision(600, true));
        assertEquals(590, UnderwaterMath.tickVision(600, false));
        assertEquals(0, UnderwaterMath.tickVision(4, false));
    }

    @Test
    void fogReachesNinetySixBlocksWhenAdapted() {
        assertEquals(24.0F, UnderwaterMath.fogEnd(0.0F, false), 1.0E-5F);
        assertEquals(96.0F, UnderwaterMath.fogEnd(1.0F, false), 1.0E-5F);
        assertEquals(96.0F * 0.85F, UnderwaterMath.fogEnd(1.0F, true), 1.0E-4F);
    }

    @Test
    void biomeFadeTakesFiveSeconds() {
        assertEquals(10.0F, UnderwaterMath.fade(10.0F, 50.0F, 0L), 0.0F);
        assertEquals(30.0F, UnderwaterMath.fade(10.0F, 50.0F, 2500L), 1.0E-4F);
        assertEquals(50.0F, UnderwaterMath.fade(10.0F, 50.0F, 9000L), 0.0F);
    }

    @Test
    void brightenSaturatesTheStrongestChannel() {
        float[] rgb = { 0x05 / 255.0F, 0x05 / 255.0F, 0x33 / 255.0F };
        float[] dark = rgb.clone();
        UnderwaterMath.brighten(dark, 0.0F);
        assertArrayEquals(rgb, dark, 0.0F);
        UnderwaterMath.brighten(rgb, 1.0F);
        assertEquals(1.0F, rgb[2], 1.0E-6F);
        assertEquals(5.0F / 51.0F, rgb[0], 1.0E-6F);
    }

    @Test
    void brightenLeavesAColourWithAZeroChannel() {
        float[] rgb = { 0.0F, 0.2F, 0.4F };
        UnderwaterMath.brighten(rgb, 1.0F);
        assertArrayEquals(new float[] { 0.0F, 0.2F, 0.4F }, rgb, 0.0F);
    }
}
