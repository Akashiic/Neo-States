package com.akashic.neostates.visual;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OverlayRuleTest {

    @Test
    void onlyTheFourSidesHaveANeighbour() {
        assertNull(OverlayRule.neighbour(0));
        assertNull(OverlayRule.neighbour(1));
        assertArrayEquals(new int[] { 0, -1 }, OverlayRule.neighbour(2));
        assertArrayEquals(new int[] { 0, 1 }, OverlayRule.neighbour(3));
        assertArrayEquals(new int[] { -1, 0 }, OverlayRule.neighbour(4));
        assertArrayEquals(new int[] { 1, 0 }, OverlayRule.neighbour(5));
    }

    @Test
    void waterAgainstGlassOrLeavesTakesTheOverlay() {
        assertTrue(OverlayRule.applies(true, 2, true, false));
        assertTrue(OverlayRule.applies(true, 5, false, true));
        assertFalse(OverlayRule.applies(true, 3, false, false));
        assertFalse(OverlayRule.applies(false, 3, true, true));
        assertFalse(OverlayRule.applies(true, 1, true, false));
    }
}
