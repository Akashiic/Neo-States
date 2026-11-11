package com.akashic.neostates.api;

/**
 * The fluid of one cell, packed in an int so reads allocate nothing. It answers "what fluid is here" the same way for
 * a liquid block and (from the waterlogging layer on) for water held inside another block.
 * <p>
 * Levels follow 1.21.1's {@code FluidState#getAmount}: 8 for a source or a falling column, 1..7 for a current. The
 * 1.7.10 metadata is the inverse ({@code amount = 8 - decay}); {@link #legacyLevel(int)} converts back.
 * <p>
 * Pure: no Minecraft classes.
 */
public final class FluidView {

    /** No fluid. */
    public static final int EMPTY = 0;

    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_WATER = 1;
    public static final int TYPE_LAVA = 2;

    /** {@code FluidState.AMOUNT_FULL}: a source, or a falling column. */
    public static final int AMOUNT_FULL = 8;

    private static final int TYPE_MASK = 0x3;
    private static final int AMOUNT_SHIFT = 2;
    private static final int AMOUNT_MASK = 0xF;
    private static final int FALLING = 1 << 6;
    private static final int SOURCE = 1 << 7;
    private static final int HOSTED = 1 << 8;

    private FluidView() {}

    /**
     * @param type    {@link #TYPE_WATER} or {@link #TYPE_LAVA}
     * @param amount  1..8
     * @param source  a source block (metadata 0)
     * @param falling a falling column (metadata 8 and above)
     * @param hosted  the fluid shares the cell with another block (waterlogging)
     */
    public static int of(int type, int amount, boolean source, boolean falling, boolean hosted) {
        if (type == TYPE_EMPTY) {
            return EMPTY;
        }
        if (type != TYPE_WATER && type != TYPE_LAVA) {
            throw new IllegalArgumentException("fluid type " + type);
        }
        if (amount < 1 || amount > AMOUNT_FULL) {
            throw new IllegalArgumentException("fluid amount " + amount);
        }
        return type | amount << AMOUNT_SHIFT | (falling ? FALLING : 0) | (source ? SOURCE : 0) | (hosted ? HOSTED : 0);
    }

    /** The fluid a 1.7.10 liquid block with this metadata is. */
    public static int ofLegacy(int type, int metadata) {
        int meta = metadata & 15;
        if (meta == 0) {
            return of(type, AMOUNT_FULL, true, false, false);
        }
        if (meta >= 8) {
            return of(type, AMOUNT_FULL, false, true, false);
        }
        return of(type, AMOUNT_FULL - meta, false, false, false);
    }

    /** Water held inside another block: always a source, as in 1.21.1. */
    public static int hostedWater() {
        return of(TYPE_WATER, AMOUNT_FULL, true, false, true);
    }

    public static int type(int view) {
        return view & TYPE_MASK;
    }

    public static boolean isEmpty(int view) {
        return (view & TYPE_MASK) == TYPE_EMPTY;
    }

    public static boolean isWater(int view) {
        return (view & TYPE_MASK) == TYPE_WATER;
    }

    public static boolean isLava(int view) {
        return (view & TYPE_MASK) == TYPE_LAVA;
    }

    /** 0 when empty, else 1..8. */
    public static int amount(int view) {
        return view >> AMOUNT_SHIFT & AMOUNT_MASK;
    }

    public static boolean isSource(int view) {
        return (view & SOURCE) != 0;
    }

    public static boolean isFalling(int view) {
        return (view & FALLING) != 0;
    }

    public static boolean isHosted(int view) {
        return (view & HOSTED) != 0;
    }

    public static boolean sameType(int a, int b) {
        return (a & TYPE_MASK) == (b & TYPE_MASK);
    }

    /**
     * The 1.7.10 metadata of this fluid, as {@code FlowingFluid.getLegacyLevel}: 0 for a source, 8 for a falling
     * column, else {@code 8 - amount}. -1 when empty.
     */
    public static int legacyLevel(int view) {
        if (isEmpty(view)) {
            return -1;
        }
        if (isSource(view)) {
            return 0;
        }
        return AMOUNT_FULL - Math.min(amount(view), AMOUNT_FULL) + (isFalling(view) ? 8 : 0);
    }
}
