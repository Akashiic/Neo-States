package com.akashic.neostates.water;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WaterBitsTest {

    @Test
    void indexIsTheSectionOrder() {
        assertEquals(0, WaterBits.index(0, 0, 0));
        assertEquals(15, WaterBits.index(15, 0, 0));
        assertEquals(16, WaterBits.index(0, 0, 1));
        assertEquals(256, WaterBits.index(0, 1, 0));
        assertEquals(4095, WaterBits.index(15, 15, 15));
        // world coordinates, negative ones included (deep sections, negative X and Z)
        assertEquals(WaterBits.index(15, 15, 15), WaterBits.index(-1, -1, -1));
        assertEquals(WaterBits.index(3, 0, 9), WaterBits.index(-13, -16, 25));
    }

    @Test
    void everyCellIsItsOwnBit() {
        long[] bits = new long[WaterBits.LONGS];
        for (int i = 0; i < 4096; i++) {
            assertFalse(WaterBits.get(bits, i));
            assertTrue(WaterBits.set(bits, i, true));
            assertFalse(WaterBits.set(bits, i, true));
            assertEquals(i + 1, WaterBits.count(bits));
        }
        assertTrue(WaterBits.set(bits, 63, false));
        assertFalse(WaterBits.get(bits, 63));
        assertTrue(WaterBits.get(bits, 62));
        assertTrue(WaterBits.get(bits, 64));
    }

    @Test
    void aMissingLayerIsEmpty() {
        assertFalse(WaterBits.get(null, 100));
        assertTrue(WaterBits.isEmpty(null));
        assertTrue(WaterBits.isEmpty(new long[WaterBits.LONGS]));
        assertEquals(0, WaterBits.count(null));
    }

    @Test
    void bytesRoundTrip() {
        long[] bits = new long[WaterBits.LONGS];
        WaterBits.set(bits, 0, true);
        WaterBits.set(bits, 63, true);
        WaterBits.set(bits, 2049, true);
        WaterBits.set(bits, 4095, true);
        byte[] bytes = WaterBits.toBytes(bits);
        assertEquals(512, bytes.length);
        assertEquals(1, bytes[0]);
        assertEquals((byte) 0x80, bytes[7]);
        assertArrayEquals(bits, WaterBits.fromBytes(bytes));
    }

    @Test
    void badOrEmptyBytesGiveNoLayer() {
        assertNull(WaterBits.fromBytes(null));
        assertNull(WaterBits.fromBytes(new byte[511]));
        assertNull(WaterBits.fromBytes(new byte[512]));
    }
}
