package com.akashic.neostates.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WaterPaletteTest {

    @Test
    void neutralMultiplierIsTheDefaultColour() {
        assertEquals(WaterPalette.WATER, WaterPalette.fromLegacyMultiplier(0xFFFFFF));
        assertEquals(WaterPalette.WATER, WaterPalette.fromLegacyMultiplier(0xFFFFFFFF));
    }

    @Test
    void aLegacyMultiplierTintsTheDefault() {
        // 1.7.10's swamp multiplier, as a mod biome would carry it
        assertEquals(0x37769B, WaterPalette.fromLegacyMultiplier(0xE0FFAE));
        assertEquals(0, WaterPalette.multiply(WaterPalette.WATER, 0));
    }
}
