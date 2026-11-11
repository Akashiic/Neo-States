package com.akashic.neostates.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.akashic.neostates.NeoStatesMod;

/**
 * Around the drawing of hosted water: tells Angelica's shader pipeline that the vertices are water, not the slab or
 * stairs being meshed, so a shaderpack treats them as it treats water. Without Angelica it does nothing.
 */
public final class HostedWaterRender {

    private static final MethodHandle SET = find(
        "setShaderMaterialOverride",
        MethodType.methodType(void.class, Block.class, int.class));
    private static final MethodHandle RESET = find("resetShaderMaterialOverride", MethodType.methodType(void.class));

    private HostedWaterRender() {}

    private static MethodHandle find(String name, MethodType type) {
        try {
            return MethodHandles.publicLookup()
                .findStatic(Class.forName("net.coderbot.iris.Iris"), name, type);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException | LinkageError e) {
            NeoStatesMod.LOG
                .warn("Angelica's shader material override was not found; hosted water keeps its host's shader id.", e);
            return null;
        }
    }

    public static void begin() {
        if (SET != null && RESET != null) {
            try {
                SET.invokeExact(Blocks.water, 0);
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }
    }

    public static void end() {
        if (SET != null && RESET != null) {
            try {
                RESET.invokeExact();
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }
    }
}
