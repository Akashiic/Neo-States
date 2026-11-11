package com.akashic.neostates.light;

/**
 * Raises stored light after some cells became more transparent. It applies the rule of 1.7.10's
 * {@code World.computeLightValue}, {@code light = max(own, neighbour - max(1, opacity))}, as a flood fill that only
 * ever raises values: the stored light is a lower bound of the new answer, so starting from the changed cells and
 * following every raise reaches the same values a full recomputation would, without touching the rest.
 * <p>
 * No Minecraft types: the cells live behind {@link Grid}.
 */
public final class LightFill {

    /** Light stops at this opacity. */
    public static final int OPAQUE = 15;

    public interface Grid {

        /** Whether the cell exists for this fill; cells outside are neither read nor written. */
        boolean inside(int x, int y, int z);

        int light(int x, int y, int z);

        void light(int x, int y, int z, int value);

        /** 1..15: what the cell takes from light entering it; {@link #OPAQUE} lets none in. */
        int opacity(int x, int y, int z);
    }

    private static final int[] DX = { 0, 0, 0, 0, -1, 1 };
    private static final int[] DY = { -1, 1, 0, 0, 0, 0 };
    private static final int[] DZ = { 0, 0, -1, 1, 0, 0 };

    private int[] qx = new int[1024];
    private int[] qy = new int[1024];
    private int[] qz = new int[1024];
    private int head;
    private int tail;

    private void push(int x, int y, int z) {
        if (this.tail == this.qx.length) {
            if (this.head > 0) {
                int n = this.tail - this.head;
                System.arraycopy(this.qx, this.head, this.qx, 0, n);
                System.arraycopy(this.qy, this.head, this.qy, 0, n);
                System.arraycopy(this.qz, this.head, this.qz, 0, n);
                this.head = 0;
                this.tail = n;
            }
            if (this.tail == this.qx.length) {
                int size = this.qx.length * 2;
                this.qx = java.util.Arrays.copyOf(this.qx, size);
                this.qy = java.util.Arrays.copyOf(this.qy, size);
                this.qz = java.util.Arrays.copyOf(this.qz, size);
            }
        }
        this.qx[this.tail] = x;
        this.qy[this.tail] = y;
        this.qz[this.tail] = z;
        this.tail++;
    }

    /**
     * A cell whose opacity dropped: takes what its neighbours now give it.
     *
     * @return whether its light rose
     */
    public boolean seed(Grid grid, int x, int y, int z) {
        int opacity = grid.opacity(x, y, z);
        if (opacity >= OPAQUE) {
            return false;
        }
        int best = 0;
        for (int side = 0; side < 6; side++) {
            int nx = x + DX[side];
            int ny = y + DY[side];
            int nz = z + DZ[side];
            if (grid.inside(nx, ny, nz)) {
                int light = grid.light(nx, ny, nz);
                if (light > best) {
                    best = light;
                }
            }
        }
        int value = best - opacity;
        if (value <= grid.light(x, y, z)) {
            return false;
        }
        grid.light(x, y, z, value);
        push(x, y, z);
        return true;
    }

    /**
     * Follows every raise until nothing changes.
     *
     * @return how many cells were raised by spreading (seeds not counted)
     */
    public int drain(Grid grid) {
        int raised = 0;
        while (this.head < this.tail) {
            int x = this.qx[this.head];
            int y = this.qy[this.head];
            int z = this.qz[this.head];
            this.head++;
            int light = grid.light(x, y, z);
            if (light <= 1) {
                continue;
            }
            for (int side = 0; side < 6; side++) {
                int nx = x + DX[side];
                int ny = y + DY[side];
                int nz = z + DZ[side];
                if (!grid.inside(nx, ny, nz)) {
                    continue;
                }
                int opacity = grid.opacity(nx, ny, nz);
                if (opacity >= OPAQUE) {
                    continue;
                }
                int value = light - opacity;
                if (value > grid.light(nx, ny, nz)) {
                    grid.light(nx, ny, nz, value);
                    push(nx, ny, nz);
                    raised++;
                }
            }
        }
        this.head = 0;
        this.tail = 0;
        return raised;
    }
}
