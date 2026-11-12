package com.akashic.neostates.water;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HolderShapesTest {

    private static int closedFaces(int kind, int meta) {
        int mask = 0;
        for (int side = 0; side < 6; side++) {
            if (HolderShapes.closed(kind, meta, side)) {
                mask |= 1 << side;
            }
        }
        return mask;
    }

    @Test
    void slabsCloseTheFaceTheyLieOn() {
        assertEquals(1 << 0, closedFaces(HolderShapes.SLAB, 0));
        assertEquals(1 << 0, closedFaces(HolderShapes.SLAB, 5));
        assertEquals(1 << 1, closedFaces(HolderShapes.SLAB, 8));
        assertEquals(1 << 1, closedFaces(HolderShapes.SLAB, 13));
    }

    @Test
    void stairsCloseTheirBaseAndTheirBack() {
        assertEquals(1 << 0 | 1 << 5, closedFaces(HolderShapes.STAIRS, 0));
        assertEquals(1 << 0 | 1 << 4, closedFaces(HolderShapes.STAIRS, 1));
        assertEquals(1 << 0 | 1 << 3, closedFaces(HolderShapes.STAIRS, 2));
        assertEquals(1 << 0 | 1 << 2, closedFaces(HolderShapes.STAIRS, 3));
        assertEquals(1 << 1 | 1 << 5, closedFaces(HolderShapes.STAIRS, 4));
        assertEquals(1 << 1 | 1 << 2, closedFaces(HolderShapes.STAIRS, 7));
    }

    @Test
    void trapdoorsAreASlabShutAndAWallOpen() {
        assertEquals(1 << 0, closedFaces(HolderShapes.TRAPDOOR, 0));
        assertEquals(1 << 1, closedFaces(HolderShapes.TRAPDOOR, 8));
        assertEquals(1 << 3, closedFaces(HolderShapes.TRAPDOOR, 4));
        assertEquals(1 << 2, closedFaces(HolderShapes.TRAPDOOR, 5));
        assertEquals(1 << 5, closedFaces(HolderShapes.TRAPDOOR, 6));
        assertEquals(1 << 4, closedFaces(HolderShapes.TRAPDOOR, 7));
        assertEquals(1 << 4, closedFaces(HolderShapes.TRAPDOOR, 15));
    }

    @Test
    void laddersCloseTheWallTheyHangOn() {
        assertEquals(1 << 3, closedFaces(HolderShapes.LADDER, 2));
        assertEquals(1 << 2, closedFaces(HolderShapes.LADDER, 3));
        assertEquals(1 << 5, closedFaces(HolderShapes.LADDER, 4));
        assertEquals(1 << 4, closedFaces(HolderShapes.LADDER, 5));
        assertEquals(0, closedFaces(HolderShapes.LADDER, 0));
    }

    @Test
    void theRestIsOpenAllRound() {
        for (int meta = 0; meta < 16; meta++) {
            assertEquals(0, closedFaces(HolderShapes.NONE, meta));
        }
        assertTrue(HolderShapes.closed(HolderShapes.SLAB, 0, 0));
        assertFalse(HolderShapes.closed(HolderShapes.SLAB, 0, 1));
    }
}
