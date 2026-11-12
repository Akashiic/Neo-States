package com.akashic.neostates.water;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.akashic.neostates.NeoStatesMod;

/**
 * Finds the section of a Y in a chunk: the vanilla array for 0..255, BelowCore's deep sections below 0 (they are real
 * {@code ExtendedBlockStorage}, kept outside {@code Chunk.storageArrays}). BelowCore is optional and reached through
 * its public API by a method handle.
 */
public final class Sections {

    /** BelowCore's lowest section: 32 deep sections. */
    public static final int MIN_SECTION_Y = -32;

    private static final MethodHandle DEEP_SECTION = findDeepSection();

    private Sections() {}

    private static MethodHandle findDeepSection() {
        try {
            Class<?> api = Class.forName("com.akashiic.bellow.api.BellowAPI");
            return MethodHandles.publicLookup()
                .findStatic(
                    api,
                    "getDeepSection",
                    MethodType.methodType(ExtendedBlockStorage.class, Chunk.class, int.class));
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException | LinkageError e) {
            NeoStatesMod.LOG.error("BelowCore is present but its API did not link; no waterlogging below Y 0.", e);
            return null;
        }
    }

    public static boolean hasDeepSections() {
        return DEEP_SECTION != null;
    }

    /** @return the section, or null when it does not exist */
    public static ExtendedBlockStorage section(Chunk chunk, int sectionY) {
        if (sectionY >= 0) {
            ExtendedBlockStorage[] array = chunk.getBlockStorageArray();
            return sectionY < array.length ? array[sectionY] : null;
        }
        if (DEEP_SECTION == null || sectionY < MIN_SECTION_Y) {
            return null;
        }
        try {
            return (ExtendedBlockStorage) DEEP_SECTION.invokeExact(chunk, sectionY);
        } catch (Throwable t) {
            throw new IllegalStateException("BelowCore deep section lookup failed", t);
        }
    }

    /** The lowest section worth visiting in this environment. */
    public static int minSectionY() {
        return DEEP_SECTION == null ? 0 : MIN_SECTION_Y;
    }
}
