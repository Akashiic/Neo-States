package com.akashic.neostates.light;

/** On a chunk: the water opacity its stored light was computed with. */
public interface LightMark {

    /** A chunk read from disk that carries no mark: lit by a version without the migration. */
    byte LEGACY = -1;

    /** @return 0 when not known yet, {@link #LEGACY}, or the opacity (1..15) */
    byte neostates$lightMark();

    void neostates$lightMark(byte mark);
}
