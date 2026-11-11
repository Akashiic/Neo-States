package com.akashic.neostates.water;

/** Implemented on {@code ExtendedBlockStorage} by mixin: the section carries its own waterlogging layer. */
public interface WaterSection {

    /** null while the section has no waterlogged cell. */
    long[] neostates$getWater();

    void neostates$setWater(long[] bits);
}
