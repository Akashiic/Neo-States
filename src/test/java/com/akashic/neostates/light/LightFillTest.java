package com.akashic.neostates.light;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

class LightFillTest {

    /** A box of cells; the row above the top is open sky (15), outside the box. */
    private static final class Box implements LightFill.Grid {

        final int sx;
        final int sy;
        final int sz;
        final int[] light;
        final int[] opacity;
        /** Emission for the reference computation. */
        final int[] source;

        Box(int sx, int sy, int sz) {
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.light = new int[sx * sy * sz];
            this.opacity = new int[sx * sy * sz];
            this.source = new int[sx * sy * sz];
            java.util.Arrays.fill(this.opacity, 1);
        }

        int at(int x, int y, int z) {
            return (y * this.sz + z) * this.sx + x;
        }

        @Override
        public boolean inside(int x, int y, int z) {
            return x >= 0 && x < this.sx && y >= 0 && y < this.sy && z >= 0 && z < this.sz;
        }

        @Override
        public int light(int x, int y, int z) {
            return this.light[at(x, y, z)];
        }

        @Override
        public void light(int x, int y, int z, int value) {
            this.light[at(x, y, z)] = value;
        }

        @Override
        public int opacity(int x, int y, int z) {
            return this.opacity[at(x, y, z)];
        }

        /** The light from scratch: computeLightValue's rule iterated until nothing changes. */
        int[] reference() {
            int[] out = this.source.clone();
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int y = 0; y < this.sy; y++) {
                    for (int z = 0; z < this.sz; z++) {
                        for (int x = 0; x < this.sx; x++) {
                            int i = at(x, y, z);
                            if (this.opacity[i] >= LightFill.OPAQUE) {
                                continue;
                            }
                            int best = out[i];
                            int[][] sides = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
                                { 0, 0, -1 } };
                            for (int[] d : sides) {
                                if (inside(x + d[0], y + d[1], z + d[2])) {
                                    best = Math.max(best, out[at(x + d[0], y + d[1], z + d[2])] - this.opacity[i]);
                                }
                            }
                            if (best != out[i]) {
                                out[i] = best;
                                changed = true;
                            }
                        }
                    }
                }
            }
            return out;
        }
    }

    @Test
    void aColumnOfWaterLosesOnePerBlock() {
        Box box = new Box(1, 12, 1);
        box.source[box.at(0, 11, 0)] = 15; // the air above the surface
        java.util.Arrays.fill(box.opacity, 3);
        box.opacity[box.at(0, 11, 0)] = 1;
        System.arraycopy(box.reference(), 0, box.light, 0, box.light.length);
        assertEquals(12, box.light(0, 10, 0));
        assertEquals(0, box.light(0, 5, 0));

        java.util.Arrays.fill(box.opacity, 1);
        LightFill fill = new LightFill();
        for (int y = 10; y >= 0; y--) {
            fill.seed(box, 0, y, 0);
        }
        fill.drain(box);
        for (int y = 10; y >= 0; y--) {
            assertEquals(15 - (11 - y), box.light(0, y, 0), "y " + y);
        }
    }

    /** Random caves, water and lamps: raising from the old light gives exactly the light computed from scratch. */
    @Test
    void raisingFromOldLightEqualsRecomputing() {
        Random random = new Random(1234L);
        LightFill fill = new LightFill();
        for (int round = 0; round < 60; round++) {
            Box box = new Box(14, 12, 14);
            boolean[] water = new boolean[box.light.length];
            for (int i = 0; i < box.light.length; i++) {
                int roll = random.nextInt(100);
                if (roll < 25) {
                    box.opacity[i] = LightFill.OPAQUE;
                } else if (roll < 65) {
                    water[i] = true;
                    box.opacity[i] = 3;
                } else if (roll < 67) {
                    box.source[i] = 6 + random.nextInt(10);
                }
            }
            for (int z = 0; z < box.sz; z++) {
                for (int x = 0; x < box.sx; x++) {
                    int top = box.at(x, box.sy - 1, z);
                    box.opacity[top] = 1;
                    water[top] = false;
                    box.source[top] = 15;
                }
            }
            System.arraycopy(box.reference(), 0, box.light, 0, box.light.length);
            for (int i = 0; i < water.length; i++) {
                if (water[i]) {
                    box.opacity[i] = 1;
                }
            }
            int[] expected = box.reference();
            for (int y = box.sy - 1; y >= 0; y--) {
                for (int z = 0; z < box.sz; z++) {
                    for (int x = 0; x < box.sx; x++) {
                        if (water[box.at(x, y, z)]) {
                            fill.seed(box, x, y, z);
                        }
                    }
                }
            }
            fill.drain(box);
            assertArrayEquals(expected, box.light, "round " + round);
        }
    }

    @Test
    void nothingChangesWhenTheLightIsAlreadyRight() {
        Box box = new Box(6, 6, 6);
        box.source[box.at(3, 5, 3)] = 15;
        System.arraycopy(box.reference(), 0, box.light, 0, box.light.length);
        int[] before = box.light.clone();
        LightFill fill = new LightFill();
        boolean any = false;
        for (int y = 0; y < 6; y++) {
            any |= fill.seed(box, 2, y, 2);
        }
        assertEquals(0, fill.drain(box));
        assertEquals(false, any);
        assertArrayEquals(before, box.light);
    }
}
