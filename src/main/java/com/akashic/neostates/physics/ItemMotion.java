package com.akashic.neostates.physics;

/** On an item entity: its motion as it was when the tick began, before any other mod's hook at the head of it. */
public interface ItemMotion {

    void neostates$saveMotion();

    void neostates$restoreMotion();
}
