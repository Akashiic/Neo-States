package com.akashic.neostates.physics;

/** Implemented on {@code Entity} by mixin: how deep in water the entity was at its last update. */
public interface WaterBody {

    /** Height of the water surface above the entity's feet, 0 out of water (1.21.1's {@code getFluidHeight}). */
    double neostates$waterHeight();

    void neostates$waterHeight(double height);
}
