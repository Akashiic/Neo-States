package com.akashic.neostates.mixin.late.angelica;

import net.minecraft.world.gen.structure.StructureBoundingBox;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import com.akashic.neostates.water.WaterBits;
import com.akashic.neostates.water.WaterSection;
import com.akashic.neostates.water.WaterView;
import com.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ClonedChunkSection;

/**
 * The slice of the world a meshing thread reads answers for the waterlogging layer from its section copies, the same
 * way it answers for blocks. No live world is touched off the client thread.
 */
@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.world.WorldSlice", remap = false)
public abstract class MixinWorldSlice_Water implements WaterView {

    @Shadow
    private ClonedChunkSection[] sections;

    @Shadow
    private int baseX;

    @Shadow
    private int baseY;

    @Shadow
    private int baseZ;

    @Shadow
    private StructureBoundingBox volume;

    @Shadow
    public static int getLocalSectionIndex(int x, int y, int z) {
        throw new AssertionError();
    }

    @Override
    public boolean neostates$water(int x, int y, int z) {
        StructureBoundingBox box = this.volume;
        ClonedChunkSection[] copies = this.sections;
        if (box == null || copies == null
            || x < box.minX
            || x > box.maxX
            || y < box.minY
            || y > box.maxY
            || z < box.minZ
            || z > box.maxZ) {
            return false;
        }
        int relX = x - this.baseX;
        int relY = y - this.baseY;
        int relZ = z - this.baseZ;
        ClonedChunkSection section = copies[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)];
        if (section == null) {
            return false;
        }
        Object data = section.getData();
        return data != null
            && WaterBits.get(((WaterSection) data).neostates$getWater(), WaterBits.index(relX, relY, relZ));
    }
}
