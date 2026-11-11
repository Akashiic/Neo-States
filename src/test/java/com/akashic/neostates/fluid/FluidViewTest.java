package com.akashic.neostates.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.akashic.neostates.api.FluidView;

class FluidViewTest {

    @Test
    void emptyIsZero() {
        assertTrue(FluidView.isEmpty(FluidView.EMPTY));
        assertEquals(0, FluidView.amount(FluidView.EMPTY));
        assertEquals(-1, FluidView.legacyLevel(FluidView.EMPTY));
        assertEquals(FluidView.EMPTY, FluidView.of(FluidView.TYPE_EMPTY, 5, false, false, false));
    }

    @Test
    void legacyMetadataRoundTrips() {
        for (int type : new int[] { FluidView.TYPE_WATER, FluidView.TYPE_LAVA }) {
            for (int meta = 0; meta < 16; meta++) {
                int view = FluidView.ofLegacy(type, meta);
                assertEquals(type, FluidView.type(view));
                // every falling metadata (8..15) is the same fluid, as all 1.7.10 code treats them
                assertEquals(meta >= 8 ? 8 : meta, FluidView.legacyLevel(view), "metadata " + meta);
                assertEquals(meta == 0, FluidView.isSource(view));
                assertEquals(meta >= 8, FluidView.isFalling(view));
                assertFalse(FluidView.isHosted(view));
            }
        }
    }

    @Test
    void amountIsEightMinusDecay() {
        assertEquals(8, FluidView.amount(FluidView.ofLegacy(FluidView.TYPE_WATER, 0)));
        assertEquals(7, FluidView.amount(FluidView.ofLegacy(FluidView.TYPE_WATER, 1)));
        assertEquals(1, FluidView.amount(FluidView.ofLegacy(FluidView.TYPE_WATER, 7)));
        assertEquals(8, FluidView.amount(FluidView.ofLegacy(FluidView.TYPE_WATER, 8)));
    }

    @Test
    void hostedWaterIsASource() {
        int view = FluidView.hostedWater();
        assertTrue(FluidView.isWater(view));
        assertTrue(FluidView.isSource(view));
        assertTrue(FluidView.isHosted(view));
        assertEquals(0, FluidView.legacyLevel(view));
        assertTrue(FluidView.sameType(view, FluidView.ofLegacy(FluidView.TYPE_WATER, 3)));
        assertFalse(FluidView.sameType(view, FluidView.ofLegacy(FluidView.TYPE_LAVA, 0)));
    }

    @Test
    void rejectsBadValues() {
        assertThrows(IllegalArgumentException.class, () -> FluidView.of(3, 8, true, false, false));
        assertThrows(IllegalArgumentException.class, () -> FluidView.of(FluidView.TYPE_WATER, 0, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> FluidView.of(FluidView.TYPE_WATER, 9, false, false, false));
    }
}
