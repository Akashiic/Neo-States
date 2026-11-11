package com.akashic.neostates.physics;

/**
 * The numbers of an entity in water in 1.21.1 ({@code Entity#updateFluidHeightAndDoFluidPushing},
 * {@code LivingEntity#travel}, {@code LivingEntity#aiStep}, {@code LocalPlayer#aiStep}). Pure: no Minecraft classes.
 */
public final class WaterPhysics {

    /** The push of water on an entity, per tick. The same in 1.7.10. */
    public static final double PUSH = 0.014D;
    /** Below this depth the flow of a cell is scaled by the depth. */
    public static final double SHALLOW = 0.4D;
    /** An entity that is almost still is pushed at least this much. */
    public static final double MIN_PUSH = 0.0045000000000000005D;
    public static final double STILL = 0.003D;

    /** 1.7.10's sinking in water, per tick. */
    public static final double LEGACY_GRAVITY = 0.02D;
    /** 1.21.1's: gravity (0.08) / 16. */
    public static final double GRAVITY = 0.005D;
    /** Swimming up, per tick. The same in 1.7.10. */
    public static final double JUMP = 0.03999999910593033D;
    /** {@code LocalPlayer}: sneaking in water. */
    public static final double SNEAK = 0.03999999910593033D;
    /** Motion kept per tick in water. */
    public static final double DRAG = 0.800000011920929D;

    private WaterPhysics() {}

    /** {@code LivingEntity#getFluidFallingAdjustedMovement}: a sprinting entity does not sink. */
    public static double gravity(boolean sprinting) {
        return sprinting ? 0.0D : GRAVITY;
    }

    /** {@code ItemEntity#tick}: an item floats when the water reaches more than this above its feet. */
    public static final double ITEM_FLOAT_DEPTH = 0.1F;
    /** {@code setUnderwaterMovement}: what a floating item or orb gains per tick, and where it stops gaining. */
    public static final double FLOAT_LIFT = 5.0E-4F;
    public static final double FLOAT_MAX = 0.06F;
    /** {@code setUnderwaterMovement}: horizontal drag of a floating item or orb. */
    public static final double FLOAT_DRAG = 0.99F;
    /** {@code ExperienceOrb}: its eyes, above its feet ({@code 0.85} of a height of 0.5). */
    public static final double ORB_EYES = 0.425D;
    /** {@code LivingEntity#increaseAirSupply}. */
    public static final int AIR_REGAIN = 4;

    /**
     * {@code ItemEntity#setUnderwaterMovement} in place of gravity: what to subtract from the vertical motion (the
     * game's line is {@code motionY -= gravity}).
     */
    public static double itemFloat(double motionY) {
        return motionY < FLOAT_MAX ? -FLOAT_LIFT : 0.0D;
    }

    /** {@code ExperienceOrb#setUnderwaterMovement}, the same way: {@code min(motionY + lift, max)}. */
    public static double orbFloat(double motionY) {
        return motionY - Math.min(motionY + FLOAT_LIFT, FLOAT_MAX);
    }

    /** {@code LivingEntity#baseTick}: out of the water, air comes back a little per tick. */
    public static int regainAir(int air, int max) {
        return air >= max ? max : Math.min(air + AIR_REGAIN, max);
    }

    /** {@code Entity#getFluidJumpThreshold}. */
    public static double jumpThreshold(float eyeHeight) {
        return eyeHeight < 0.4F ? 0.0D : 0.4D;
    }

    /**
     * {@code LivingEntity#aiStep}: whether the jump key paddles up (true) or is a jump off the ground (false), for an
     * entity in water.
     */
    public static boolean swimsUp(boolean onGround, double waterHeight, double threshold) {
        return waterHeight > 0.0D && (!onGround || waterHeight > threshold);
    }

    /**
     * The last step of the push: the summed flow of the cells becomes the motion to add. Writes into {@code flow}.
     *
     * @param cells   how many cells contributed
     * @param player  a player's flow is not normalised
     * @param motionX the entity's motion, to tell an entity that is almost still
     */
    public static void push(double[] flow, int cells, boolean player, double motionX, double motionZ) {
        double length = length(flow);
        if (length <= 0.0D) {
            flow[0] = flow[1] = flow[2] = 0.0D;
            return;
        }
        if (cells > 0) {
            scale(flow, 1.0D / cells);
        }
        if (!player) {
            normalize(flow);
        }
        scale(flow, PUSH);
        if (Math.abs(motionX) < STILL && Math.abs(motionZ) < STILL && length(flow) < MIN_PUSH) {
            normalize(flow);
            scale(flow, MIN_PUSH);
        }
    }

    private static double length(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static void scale(double[] v, double by) {
        v[0] *= by;
        v[1] *= by;
        v[2] *= by;
    }

    /** As 1.21.1's {@code Vec3#normalize}: a vector shorter than 1e-4 becomes zero. */
    private static void normalize(double[] v) {
        double length = length(v);
        if (length < 1.0E-4D) {
            v[0] = v[1] = v[2] = 0.0D;
        } else {
            scale(v, 1.0D / length);
        }
    }
}
