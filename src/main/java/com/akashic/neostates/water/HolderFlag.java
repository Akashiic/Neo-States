package com.akashic.neostates.water;

/** Implemented on {@code Block} by mixin: the cached answer of {@link WaterHolders}, a field read on the hot paths. */
public interface HolderFlag {

    /** 0 not asked yet, 1 may not hold water, 2 may. */
    byte neostates$holder();

    void neostates$holder(byte value);
}
