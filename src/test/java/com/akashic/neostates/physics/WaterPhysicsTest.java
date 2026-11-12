package com.akashic.neostates.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WaterPhysicsTest {

    /** The speed an entity moves at each tick once it settles: accelerate, move, then drag and sink. */
    private static double terminal(double perTick, double gravity) {
        double motion = 0.0D;
        double moved = 0.0D;
        for (int tick = 0; tick < 200; tick++) {
            motion += perTick;
            moved = motion;
            motion = motion * WaterPhysics.DRAG - gravity;
        }
        return moved;
    }

    /** docs/DELTAS.md, D-4, D-6 and D-7. */
    @Test
    void terminalSpeeds() {
        assertEquals(-0.1D, terminal(0.0D, WaterPhysics.LEGACY_GRAVITY), 1.0E-6D);
        assertEquals(-0.025D, terminal(0.0D, WaterPhysics.GRAVITY), 1.0E-6D);
        assertEquals(0.1D, terminal(WaterPhysics.JUMP, WaterPhysics.LEGACY_GRAVITY), 1.0E-6D);
        assertEquals(0.175D, terminal(WaterPhysics.JUMP, WaterPhysics.GRAVITY), 1.0E-6D);
        assertEquals(-0.225D, terminal(-WaterPhysics.SNEAK, WaterPhysics.GRAVITY), 1.0E-6D);
    }

    @Test
    void sprintingDoesNotSink() {
        assertEquals(0.0D, WaterPhysics.gravity(true), 0.0D);
        assertEquals(0.005D, WaterPhysics.gravity(false), 0.0D);
    }

    @Test
    void jumpRule() {
        double threshold = WaterPhysics.jumpThreshold(1.62F);
        assertEquals(0.4D, threshold, 0.0D);
        assertEquals(0.0D, WaterPhysics.jumpThreshold(0.3F), 0.0D);
        assertTrue(WaterPhysics.swimsUp(false, 0.1D, threshold), "off the ground in any water");
        assertTrue(WaterPhysics.swimsUp(true, 0.9D, threshold), "standing in deep water");
        assertFalse(WaterPhysics.swimsUp(true, 0.3D, threshold), "standing in shallow water jumps off the ground");
        assertFalse(WaterPhysics.swimsUp(false, 0.0D, threshold), "no water");
    }

    @Test
    void pushOfAMobIsNormalised() {
        double[] flow = { 3.0D, 0.0D, 4.0D };
        WaterPhysics.push(flow, 2, false, 0.1D, 0.1D);
        assertEquals(0.6D * 0.014D, flow[0], 1.0E-12D);
        assertEquals(0.8D * 0.014D, flow[2], 1.0E-12D);
    }

    @Test
    void pushOfAPlayerIsTheAverage() {
        double[] flow = { 1.0D, 0.0D, 0.0D };
        WaterPhysics.push(flow, 4, true, 0.1D, 0.1D);
        assertEquals(0.25D * 0.014D, flow[0], 1.0E-12D);
    }

    @Test
    void aStillEntityGetsTheMinimumPush() {
        double[] flow = { 0.2D, 0.0D, 0.0D };
        WaterPhysics.push(flow, 1, true, 0.0D, 0.0D);
        assertEquals(WaterPhysics.MIN_PUSH, flow[0], 1.0E-12D);
        double[] moving = { 0.2D, 0.0D, 0.0D };
        WaterPhysics.push(moving, 1, true, 0.05D, 0.0D);
        assertEquals(0.2D * 0.014D, moving[0], 1.0E-12D);
    }

    @Test
    void noFlowNoPush() {
        double[] flow = { 0.0D, 0.0D, 0.0D };
        WaterPhysics.push(flow, 3, false, 0.0D, 0.0D);
        assertEquals(0.0D, flow[0] + flow[1] + flow[2], 0.0D);
    }

    @Test
    void aFloatingItemRisesToASteadySpeed() {
        // ItemEntity.tick in water: setUnderwaterMovement, then the 0.98 drag
        double v = -0.3D;
        for (int tick = 0; tick < 600; tick++) {
            v -= WaterPhysics.itemFloat(v);
            v *= 0.98D;
        }
        assertEquals(5.0E-4F * 0.98D / 0.02D, v, 1.0e-5D);
        assertEquals(0.0D, WaterPhysics.itemFloat(0.06F), 0.0D);
        assertEquals(0.0D, WaterPhysics.itemFloat(0.2D), 0.0D);
    }

    @Test
    void anOrbNeverRisesFasterThanTheCap() {
        assertEquals(0.06F, 0.3D - WaterPhysics.orbFloat(0.3D), 0.0D);
        assertEquals(0.01D + 5.0E-4F, 0.01D - WaterPhysics.orbFloat(0.01D), 1.0e-12D);
    }

    @Test
    void airComesBackFourATick() {
        assertEquals(104, WaterPhysics.regainAir(100, 300));
        assertEquals(300, WaterPhysics.regainAir(298, 300));
        assertEquals(300, WaterPhysics.regainAir(300, 300));
        assertEquals(4, WaterPhysics.regainAir(0, 300));
    }
}
