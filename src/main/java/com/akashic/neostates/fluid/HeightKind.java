package com.akashic.neostates.fluid;

/** Which height a caller wants; see docs/adr/ADR-0003-pipeline-de-fluidos.md. */
public enum HeightKind {
    /** Simulation: whole ninths, never deformed. */
    LOGICAL,
    /** Entities, hooks, camera: must be the same on both sides. */
    INTERACTION,
    /** Mesh and particles, client only. */
    RENDER
}
