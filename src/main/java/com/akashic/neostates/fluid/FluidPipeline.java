package com.akashic.neostates.fluid;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.IBlockAccess;

import com.akashic.neostates.api.FluidView;

/**
 * Every height read goes through here, so a later stage can deform the surface without touching the callers.
 * Stages are registered during mod loading and frozen after it.
 */
public final class FluidPipeline {

    private static final List<FluidStage> pending = new ArrayList<>();
    private static volatile FluidStage[] stages = new FluidStage[0];
    private static volatile boolean frozen;

    private FluidPipeline() {}

    /**
     * Adds a stage after the ones already registered. A stage that is not deterministic only sees
     * {@link HeightKind#RENDER}.
     */
    public static synchronized void register(FluidStage stage) {
        if (frozen) {
            throw new IllegalStateException("fluid stages are registered before postInit ends");
        }
        if (stage == null) {
            throw new NullPointerException("stage");
        }
        pending.add(stage);
        stages = pending.toArray(new FluidStage[0]);
    }

    public static synchronized void freeze() {
        frozen = true;
    }

    /** The height of the surface in the cell, 0..1; 0 when the cell has no fluid. */
    public static float height(IBlockAccess access, int x, int y, int z, HeightKind kind) {
        int view = FluidResolver.resolve(access, x, y, z);
        if (FluidView.isEmpty(view)) {
            return 0.0F;
        }
        float height = FluidMath.height(view, FluidResolver.resolve(access, x, y + 1, z));
        if (kind == HeightKind.LOGICAL) {
            return height;
        }
        FluidStage[] all = stages;
        for (FluidStage stage : all) {
            if (kind == HeightKind.RENDER || stage.deterministic()) {
                height = stage.height(access, x, y, z, view, kind, height);
            }
        }
        return height;
    }
}
