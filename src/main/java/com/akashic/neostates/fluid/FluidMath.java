package com.akashic.neostates.fluid;

import com.akashic.neostates.api.FluidView;

/**
 * The logical height table. It is the same in 1.7.10 ({@code 1 - (decay + 1) / 9}, the drawn surface) and in 1.21.1
 * ({@code amount / 9}); see docs/DELTAS.md, A-1 to A-4. Pure: no Minecraft classes.
 */
public final class FluidMath {

    /** {@code FlowingFluid#getOwnHeight} of a full cell: 8/9. */
    public static final float FULL_HEIGHT = 8.0F / 9.0F;

    private FluidMath() {}

    /** {@code FlowingFluid#getOwnHeight}: the surface of the cell alone, 0 when empty. */
    public static float ownHeight(int view) {
        return FluidView.amount(view) / 9.0F;
    }

    /** {@code FlowingFluid#getHeight}: 1 under more of the same fluid, else {@link #ownHeight(int)}. */
    public static float height(int view, int above) {
        if (FluidView.isEmpty(view)) {
            return 0.0F;
        }
        return FluidView.sameType(view, above) ? 1.0F : ownHeight(view);
    }

    /** 1.7.10's {@code BlockLiquid.getLiquidHeightPercent}: the fraction of the cell that is air. */
    public static float legacyAirFraction(int metadata) {
        int decay = metadata >= 8 ? 0 : metadata;
        return (decay + 1) / 9.0F;
    }
}
