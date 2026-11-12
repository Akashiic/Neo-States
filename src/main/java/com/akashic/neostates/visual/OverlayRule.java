package com.akashic.neostates.visual;

/**
 * Where 1.21.1's {@code LiquidBlockRenderer} swaps the flowing texture of a side of water for {@code water_overlay}:
 * when the block on that side is a {@code HalfTransparentBlock} (glass, stained glass, ice) or leaves. No Minecraft
 * types here; the caller says what the neighbour is.
 */
public final class OverlayRule {

    private OverlayRule() {}

    /** The offset of the neighbour on a horizontal side (2 north, 3 south, 4 west, 5 east), as {x, z}. */
    public static int[] neighbour(int side) {
        switch (side) {
            case 2:
                return new int[] { 0, -1 };
            case 3:
                return new int[] { 0, 1 };
            case 4:
                return new int[] { -1, 0 };
            case 5:
                return new int[] { 1, 0 };
            default:
                return null;
        }
    }

    /**
     * @param water           the liquid being drawn is water (lava never takes the overlay)
     * @param halfTransparent the neighbour is glass, stained glass or ice; a portal, which 1.7.10 keeps in the same
     *                        class, is not
     * @param leaves          the neighbour is leaves
     */
    public static boolean applies(boolean water, int side, boolean halfTransparent, boolean leaves) {
        return water && side >= 2 && side <= 5 && (halfTransparent || leaves);
    }
}
