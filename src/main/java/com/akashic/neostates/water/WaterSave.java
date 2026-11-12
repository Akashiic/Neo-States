package com.akashic.neostates.water;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkDataEvent;

import com.akashic.neostates.light.LightMark;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Persistence of the layer through Forge's chunk data events, with no other mod involved. The chunk's NBT gets a
 * {@code NeoStates} compound: {@code V} (format version) and {@code Water}, a list of {@code {Y: section, Bits: 512
 * bytes}} for the sections that hold any bit, and {@code Light}, the water opacity the chunk's light was computed
 * with (see {@link com.akashic.neostates.light.WaterRelight}). Without the mod the compound is ignored and the hosted
 * water is gone.
 */
public final class WaterSave {

    public static final String ROOT = "NeoStates";
    private static final byte VERSION = 1;

    @SubscribeEvent
    public void onSave(ChunkDataEvent.Save event) {
        NBTTagList list = write(event.getChunk());
        byte light = ((LightMark) event.getChunk()).neostates$lightMark();
        if (list.tagCount() == 0 && light <= 0) {
            event.getData()
                .removeTag(ROOT);
            return;
        }
        NBTTagCompound root = new NBTTagCompound();
        root.setByte("V", VERSION);
        if (list.tagCount() > 0) {
            root.setTag("Water", list);
        }
        if (light > 0) {
            root.setByte("Light", light);
        }
        event.getData()
            .setTag(ROOT, root);
    }

    @SubscribeEvent
    public void onLoad(ChunkDataEvent.Load event) {
        NBTTagCompound data = event.getData();
        LightMark mark = (LightMark) event.getChunk();
        // read from disk: until a mark says otherwise, its light is from before the migration
        mark.neostates$lightMark(LightMark.LEGACY);
        if (!data.hasKey(ROOT, 10)) {
            return;
        }
        NBTTagCompound root = data.getCompoundTag(ROOT);
        if (root.getByte("V") != VERSION) {
            return;
        }
        byte light = root.getByte("Light");
        if (light > 0) {
            mark.neostates$lightMark(light);
        }
        read(event.getChunk(), root.getTagList("Water", 10));
    }

    public static NBTTagList write(Chunk chunk) {
        NBTTagList list = new NBTTagList();
        for (int sectionY = Sections.minSectionY(); sectionY < 16; sectionY++) {
            ExtendedBlockStorage section = Sections.section(chunk, sectionY);
            if (section == null) {
                continue;
            }
            long[] bits = ((WaterSection) section).neostates$getWater();
            if (WaterBits.isEmpty(bits)) {
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("Y", (byte) sectionY);
            tag.setByteArray("Bits", WaterBits.toBytes(bits));
            list.appendTag(tag);
        }
        return list;
    }

    /** @return how many sections took their layer */
    public static int read(Chunk chunk, NBTTagList list) {
        int applied = 0;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            ExtendedBlockStorage section = Sections.section(chunk, tag.getByte("Y"));
            if (section == null) {
                continue;
            }
            ((WaterSection) section).neostates$setWater(WaterBits.fromBytes(tag.getByteArray("Bits")));
            applied++;
        }
        return applied;
    }
}
