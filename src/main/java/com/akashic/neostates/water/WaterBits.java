package com.akashic.neostates.water;

/**
 * The waterlogging layer of one section: 4096 bits, one per cell, in the section's own order
 * ({@code y << 8 | z << 4 | x}). A set bit is a water source sharing the cell with the block there, as 1.21.1's
 * {@code WATERLOGGED}. Pure: no Minecraft classes.
 */
public final class WaterBits {

    public static final int LONGS = 64;
    public static final int BYTES = LONGS * 8;

    private WaterBits() {}

    public static int index(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | x & 15;
    }

    public static boolean get(long[] bits, int index) {
        return bits != null && (bits[index >> 6] & 1L << index) != 0L;
    }

    /** @return whether the bit changed */
    public static boolean set(long[] bits, int index, boolean value) {
        long mask = 1L << index;
        long old = bits[index >> 6];
        long now = value ? old | mask : old & ~mask;
        bits[index >> 6] = now;
        return now != old;
    }

    public static boolean isEmpty(long[] bits) {
        if (bits == null) {
            return true;
        }
        for (long word : bits) {
            if (word != 0L) {
                return false;
            }
        }
        return true;
    }

    public static int count(long[] bits) {
        int n = 0;
        if (bits != null) {
            for (long word : bits) {
                n += Long.bitCount(word);
            }
        }
        return n;
    }

    /** 512 bytes, each word little-endian. */
    public static byte[] toBytes(long[] bits) {
        byte[] out = new byte[BYTES];
        for (int i = 0; i < LONGS; i++) {
            long word = bits[i];
            for (int b = 0; b < 8; b++) {
                out[i * 8 + b] = (byte) (word >>> 8 * b);
            }
        }
        return out;
    }

    /** @return null when the bytes are not a layer (wrong length) or hold no bit */
    public static long[] fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != BYTES) {
            return null;
        }
        long[] bits = new long[LONGS];
        boolean any = false;
        for (int i = 0; i < LONGS; i++) {
            long word = 0L;
            for (int b = 0; b < 8; b++) {
                word |= (bytes[i * 8 + b] & 0xFFL) << 8 * b;
            }
            bits[i] = word;
            any |= word != 0L;
        }
        return any ? bits : null;
    }
}
