package com.akashic.neostates.light;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkEvent;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.NeoStatesMod;
import com.akashic.neostates.net.MessageLight;
import com.akashic.neostates.net.WaterNet;
import com.akashic.neostates.water.Sections;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Light of the water in chunks that were lit before water became clearer. The light stored in a chunk is only
 * recomputed where a block changes, so a world generated with water taking 3 of light per block stays dark under
 * water for ever. Each such chunk is relit once: {@link LightFill} raises the light of its water cells and of
 * whatever they now light, the chunk is marked ({@code NeoStates.Light} in its NBT) and the new light is sent to the
 * players around.
 * <p>
 * It writes the light arrays of the sections directly and does not go through {@code World.updateLightByType}: that
 * one is asynchronous on Crucible and replaced by other mods on the client, and it would not say which chunks it
 * changed. A chunk is relit when the eight around it are loaded, because light raised in it can travel 14 blocks.
 * Sky and block light are done for Y 0..255; below 0 (BelowCore) only block light, which is all that layer has, and
 * no light crosses Y 0, as neither the game nor BelowCore lets it.
 * <p>
 * Values only rise. Turning the option off again does not darken chunks already relit.
 */
public final class WaterRelight {

    private static final Map<World, LinkedHashSet<Long>> PENDING = new WeakHashMap<>();
    private static final LightFill FILL = new LightFill();
    /** How many chunks that cannot be relit yet are looked at per tick. */
    private static final int SCAN_PER_TICK = 64;

    private static long chunksDone;
    private static long cellsRaised;
    private static long nanosSpent;
    private static boolean warnedDarker;

    // --- which chunks ---

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        World world = event.world;
        Chunk chunk = event.getChunk();
        if (world == null || world.isRemote || chunk instanceof EmptyChunk) {
            return;
        }
        LightMark mark = (LightMark) chunk;
        byte had = mark.neostates$lightMark();
        byte now = WaterLight.current();
        if (had == 0) {
            // no data was read for it: generated just now, with today's opacity
            mark.neostates$lightMark(now);
            return;
        }
        if (had == now) {
            return;
        }
        if (had == LightMark.LEGACY && now >= 3) {
            // lit by the game as it is: nothing to bring up to date
            mark.neostates$lightMark(now);
            return;
        }
        if (had != LightMark.LEGACY && had < now) {
            if (!warnedDarker) {
                warnedDarker = true;
                NeoStatesMod.LOG.warn(
                    "Chunks lit with clearer water (opacity {}) than now ({}): their light is not darkened.",
                    had,
                    now);
            }
            return;
        }
        if (NeoConfig.relightOldChunks) {
            queue(world).add(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition));
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.world == null || event.world.isRemote) {
            return;
        }
        LinkedHashSet<Long> queue = PENDING.get(event.world);
        if (queue != null) {
            Chunk chunk = event.getChunk();
            queue.remove(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition));
        }
    }

    private static LinkedHashSet<Long> queue(World world) {
        LinkedHashSet<Long> queue = PENDING.get(world);
        if (queue == null) {
            queue = new LinkedHashSet<>();
            PENDING.put(world, queue);
        }
        return queue;
    }

    // --- when ---

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        LinkedHashSet<Long> queue = PENDING.get(event.world);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        long start = System.nanoTime();
        long budget = NeoConfig.relightMillisPerTick * 1_000_000L;
        int scanned = 0;
        int size = queue.size();
        while (!queue.isEmpty() && scanned < SCAN_PER_TICK && scanned < size && System.nanoTime() - start < budget) {
            Iterator<Long> it = queue.iterator();
            long key = it.next();
            it.remove();
            scanned++;
            int cx = (int) (key & 0xFFFFFFFFL);
            int cz = (int) (key >>> 32);
            Chunk[] around = around(event.world, cx, cz);
            if (around == null) {
                if (loaded(event.world, cx, cz) != null) {
                    queue.add(key); // to the end: its neighbours are not all here yet
                }
                continue;
            }
            relight(event.world, around);
        }
        nanosSpent += System.nanoTime() - start;
    }

    private static Chunk loaded(World world, int cx, int cz) {
        if (!world.getChunkProvider()
            .chunkExists(cx, cz)) {
            return null;
        }
        Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
        return chunk instanceof EmptyChunk ? null : chunk;
    }

    /** @return the 3x3 around the chunk, index {@code x + 3 * z}, or null while one is missing */
    private static Chunk[] around(World world, int cx, int cz) {
        Chunk[] chunks = new Chunk[9];
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                Chunk chunk = loaded(world, cx + dx, cz + dz);
                if (chunk == null) {
                    return null;
                }
                chunks[(dx + 1) + 3 * (dz + 1)] = chunk;
            }
        }
        return chunks;
    }

    // --- the work ---

    /**
     * Relights the centre chunk now, whatever its mark, if the eight around it are loaded.
     *
     * @return cells raised, or -1 when a neighbour is missing
     */
    public static int relightNow(World world, int cx, int cz) {
        Chunk[] around = around(world, cx, cz);
        return around == null ? -1 : relight(world, around);
    }

    private static int relight(World world, Chunk[] chunks) {
        Chunk centre = chunks[4];
        int[] cells = waterCells(centre);
        int raised = 0;
        if (cells.length > 0) {
            if (!world.provider.hasNoSky) {
                raised += fill(world, chunks, cells, true, 0, 255);
            }
            raised += fill(world, chunks, cells, false, 0, 255);
            if (Sections.hasDeepSections()) {
                raised += fill(world, chunks, cells, false, Sections.MIN_SECTION_Y << 4, -1);
            }
        }
        ((LightMark) centre).neostates$lightMark(WaterLight.current());
        centre.setChunkModified();
        chunksDone++;
        cellsRaised += raised;
        return raised;
    }

    /** The water cells of a chunk, top first, packed as {@code x | z << 4 | (y + 512) << 8}. */
    private static int[] waterCells(Chunk chunk) {
        int[] cells = new int[256];
        int n = 0;
        for (int sectionY = 15; sectionY >= Sections.minSectionY(); sectionY--) {
            ExtendedBlockStorage section = Sections.section(chunk, sectionY);
            if (section == null || section.isEmpty()) {
                continue;
            }
            for (int y = 15; y >= 0; y--) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Block block = section.getBlockByExtId(x, y, z);
                        if (block == Blocks.water || block == Blocks.flowing_water) {
                            if (n == cells.length) {
                                cells = java.util.Arrays.copyOf(cells, n * 2);
                            }
                            cells[n++] = x | z << 4 | ((sectionY << 4) + y + 512) << 8;
                        }
                    }
                }
            }
        }
        return java.util.Arrays.copyOf(cells, n);
    }

    private static int fill(World world, Chunk[] chunks, int[] cells, boolean sky, int minY, int maxY) {
        WorldGrid grid = new WorldGrid(world, chunks, sky, minY, maxY);
        int raised = 0;
        for (int cell : cells) {
            int y = (cell >>> 8) - 512;
            if (y < minY || y > maxY) {
                continue;
            }
            if (FILL.seed(grid, 16 + (cell & 15), y, 16 + (cell >> 4 & 15))) {
                raised++;
            }
        }
        raised += FILL.drain(grid);
        if (raised > 0) {
            for (int i = 0; i < 9; i++) {
                if (grid.touched[i] != 0L) {
                    chunks[i].setChunkModified();
                    WaterNet.sendLight(world, MessageLight.of(chunks[i], sky, grid.touched[i]));
                }
            }
        }
        return raised;
    }

    /** One kind of light, in one range of Y, over a 3x3 of chunks. X and Z run 0..47. */
    private static final class WorldGrid implements LightFill.Grid {

        private final World world;
        private final Chunk[] chunks;
        private final boolean sky;
        private final int minY;
        private final int maxY;
        private final int originX;
        private final int originZ;
        /** Per chunk, a bit per section written (section Y + 32). */
        final long[] touched = new long[9];

        WorldGrid(World world, Chunk[] chunks, boolean sky, int minY, int maxY) {
            this.world = world;
            this.chunks = chunks;
            this.sky = sky;
            this.minY = minY;
            this.maxY = maxY;
            this.originX = chunks[0].xPosition << 4;
            this.originZ = chunks[0].zPosition << 4;
        }

        @Override
        public boolean inside(int x, int y, int z) {
            return x >= 0 && x < 48 && z >= 0 && z < 48 && y >= this.minY && y <= this.maxY;
        }

        private ExtendedBlockStorage section(int x, int y, int z) {
            return Sections.section(this.chunks[(x >> 4) + 3 * (z >> 4)], y >> 4);
        }

        @Override
        public int light(int x, int y, int z) {
            ExtendedBlockStorage section = section(x, y, z);
            if (section == null) {
                // a section of nothing but air: full sky above the ground, dark below, as the world answers
                return this.sky && y >= this.chunks[(x >> 4) + 3 * (z >> 4)].getHeightValue(x & 15, z & 15) ? 15 : 0;
            }
            if (this.sky) {
                return section.getSkylightArray() == null ? 0 : section.getExtSkylightValue(x & 15, y & 15, z & 15);
            }
            return section.getExtBlocklightValue(x & 15, y & 15, z & 15);
        }

        @Override
        public void light(int x, int y, int z, int value) {
            ExtendedBlockStorage section = section(x, y, z);
            if (section == null) {
                return; // nothing is stored for it, and nothing is drawn or spawned in it by its own light
            }
            if (this.sky) {
                NibbleArray array = section.getSkylightArray();
                if (array == null) {
                    return;
                }
                section.setExtSkylightValue(x & 15, y & 15, z & 15, value);
            } else {
                section.setExtBlocklightValue(x & 15, y & 15, z & 15, value);
            }
            this.touched[(x >> 4) + 3 * (z >> 4)] |= 1L << ((y >> 4) + 32);
        }

        @Override
        public int opacity(int x, int y, int z) {
            ExtendedBlockStorage section = section(x, y, z);
            if (section == null) {
                return 1;
            }
            Block block = section.getBlockByExtId(x & 15, y & 15, z & 15);
            if (block == Blocks.air) {
                return 1;
            }
            if (block == Blocks.water || block == Blocks.flowing_water || block == Blocks.stone) {
                return Math.max(1, block.getLightOpacity());
            }
            int wx = this.originX + x;
            int wz = this.originZ + z;
            int opacity = block.getLightOpacity(this.world, wx, y, wz);
            if (opacity >= LightFill.OPAQUE && block.getLightValue(this.world, wx, y, wz) > 0) {
                return 1; // computeLightValue: an opaque block that glows lets light through
            }
            return Math.max(1, opacity);
        }
    }

    // --- for the command ---

    public static String status(World world) {
        LinkedHashSet<Long> queue = PENDING.get(world);
        return "luz da água: opacidade " + WaterLight.current()
            + ", "
            + (queue == null ? 0 : queue.size())
            + " chunks na fila neste mundo, "
            + chunksDone
            + " refeitos, "
            + cellsRaised
            + " células clareadas, "
            + nanosSpent / 1_000_000L
            + " ms gastos";
    }
}
