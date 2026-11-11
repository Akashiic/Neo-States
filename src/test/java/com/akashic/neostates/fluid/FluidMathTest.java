package com.akashic.neostates.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.akashic.neostates.api.FluidView;

class FluidMathTest {

    /** docs/DELTAS.md A-1 to A-4: the 1.21.1 height is the surface 1.7.10 draws, for every metadata. */
    @Test
    void modernHeightIsTheLegacyDrawnSurface() {
        for (int meta = 0; meta < 16; meta++) {
            int view = FluidView.ofLegacy(FluidView.TYPE_WATER, meta);
            float legacy = 1.0F - FluidMath.legacyAirFraction(meta);
            assertEquals(legacy, FluidMath.ownHeight(view), 1.0E-6F, "metadata " + meta);
        }
    }

    @Test
    void sourceIsEightNinths() {
        int source = FluidView.ofLegacy(FluidView.TYPE_WATER, 0);
        assertEquals(8.0F / 9.0F, FluidMath.ownHeight(source), 0.0F);
        assertEquals(FluidMath.FULL_HEIGHT, FluidMath.height(source, FluidView.EMPTY), 0.0F);
    }

    @Test
    void fullUnderTheSameFluidOnly() {
        int water = FluidView.ofLegacy(FluidView.TYPE_WATER, 3);
        assertEquals(1.0F, FluidMath.height(water, FluidView.ofLegacy(FluidView.TYPE_WATER, 7)), 0.0F);
        assertEquals(1.0F, FluidMath.height(water, FluidView.hostedWater()), 0.0F);
        assertEquals(5.0F / 9.0F, FluidMath.height(water, FluidView.ofLegacy(FluidView.TYPE_LAVA, 0)), 1.0E-6F);
        assertEquals(0.0F, FluidMath.height(FluidView.EMPTY, water), 0.0F);
    }
}
