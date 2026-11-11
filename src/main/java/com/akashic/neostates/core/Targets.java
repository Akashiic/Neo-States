package com.akashic.neostates.core;

/** Injection targets shared by several mixins. Compile-time constants only. */
public final class Targets {

    public static final String WORLD_GET_BLOCK = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;";
    public static final String WORLD_GET_META = "Lnet/minecraft/world/World;getBlockMetadata(III)I";

    private Targets() {}
}
