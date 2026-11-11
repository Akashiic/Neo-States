package com.akashic.neostates.debug;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.api.FluidView;
import com.akashic.neostates.api.NeoStates;
import com.akashic.neostates.physics.ModernWater;
import com.akashic.neostates.water.BukkitFlow;
import com.akashic.neostates.water.WaterBuckets;
import com.akashic.neostates.water.WaterTicks;

/**
 * {@code /neostates autoteste <x> <y> <z>}: the rules of layer C, run in the real server on a 9x9 stone platform it
 * builds at the given corner (and clears afterwards). Each line is one rule; it is how the layer is checked on
 * Crucible, where the bytecode of the patched classes is not the development one.
 */
public final class WaterSelfTest {

    private final World world;
    private final int bx;
    private final int by;
    private final int bz;
    private final List<String> lines = new ArrayList<>();
    private int failures;

    private WaterSelfTest(World world, int bx, int by, int bz) {
        this.world = world;
        this.bx = bx;
        this.by = by;
        this.bz = bz;
    }

    public static List<String> run(World world, int bx, int by, int bz) {
        WaterSelfTest test = new WaterSelfTest(world, bx, by, bz);
        if (!world.blockExists(bx, by, bz) || !world.blockExists(bx + 8, by, bz + 8)) {
            test.lines.add("RESULTADO: NÃO RODOU (os chunks de " + bx + " " + by + " " + bz + " não estão carregados)");
            return test.lines;
        }
        test.clear(true);
        try {
            test.all();
        } catch (Throwable t) {
            test.check("sem exceção (" + t + ")", false);
        } finally {
            test.clear(false);
        }
        test.lines.add(test.failures == 0 ? "RESULTADO: PASSOU" : "RESULTADO: FALHOU (" + test.failures + ")");
        return test.lines;
    }

    private void check(String what, boolean ok) {
        if (!ok) {
            this.failures++;
        }
        this.lines.add((ok ? "ok    " : "FALHA ") + what);
    }

    private void clear(boolean platform) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                for (int y = 6; y >= 1; y--) {
                    this.world.setBlock(this.bx + x, this.by + y, this.bz + z, Blocks.air, 0, 2);
                }
                if (platform) {
                    this.world.setBlock(this.bx + x, this.by, this.bz + z, Blocks.stone, 0, 2);
                }
            }
        }
    }

    private boolean logged(int x, int y, int z) {
        return NeoStates.isWaterlogged(this.world, x, y, z);
    }

    private AxisAlignedBB cell(int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x + 0.2D, y + 0.2D, z + 0.2D, x + 0.8D, y + 0.8D, z + 0.8D);
    }

    private void all() {
        World w = this.world;
        int y = this.by + 1;

        // 1. the API on a slab, and on a block that may not hold water
        int x = this.bx + 1;
        int z = this.bz + 1;
        w.setBlock(x, y, z, Blocks.stone_slab, 0, 2);
        check("laje aceita água pela API", NeoStates.setWaterlogged(w, x, y, z, true) && logged(x, y, z));
        check(
            "fluidAt diz água fonte hospedada",
            FluidView.isHosted(NeoStates.fluidAt(w, x, y, z)) && FluidView.isSource(NeoStates.fluidAt(w, x, y, z)));
        check("pedra recusa água", !NeoStates.setWaterlogged(w, this.bx, this.by, this.bz, true));

        // 2. queries of the world and of an entity, with and without the bit
        AxisAlignedBB box = cell(x, y, z);
        check("isAnyLiquid vê a água hospedada", w.isAnyLiquid(box));
        check("isMaterialInBB(água) vê", w.isMaterialInBB(box, Material.water));
        check("isMaterialInBB(lava) não vê", !w.isMaterialInBB(box, Material.lava));
        check("isAABBInMaterial(água) vê", w.isAABBInMaterial(box, Material.water));
        EntityItem probe = new EntityItem(w, x + 0.5D, y + 0.6D, z + 0.5D, new ItemStack(Items.stick));
        check("handleMaterialAcceleration diz na água", w.handleMaterialAcceleration(box, Material.water, probe));
        check("isInsideOfMaterial(água) diz dentro", probe.isInsideOfMaterial(Material.water));
        NeoStates.setWaterlogged(w, x, y, z, false);
        check("sem o bit, isAnyLiquid não vê", !w.isAnyLiquid(box));
        check("sem o bit, não está na água", !w.handleMaterialAcceleration(box, Material.water, probe));
        check("sem o bit, olhos fora", !probe.isInsideOfMaterial(Material.water));

        // 3. a chest keeps its tile entity and contents
        x = this.bx + 3;
        w.setBlock(x, y, z, Blocks.chest, 0, 2);
        TileEntity chest = w.getTileEntity(x, y, z);
        if (chest instanceof TileEntityChest) {
            ((TileEntityChest) chest).setInventorySlotContents(5, new ItemStack(Items.diamond, 7));
        }
        NBTTagCompound before = new NBTTagCompound();
        if (chest != null) {
            chest.writeToNBT(before);
        }
        boolean on = NeoStates.setWaterlogged(w, x, y, z, true);
        NBTTagCompound during = new NBTTagCompound();
        TileEntity chestDuring = w.getTileEntity(x, y, z);
        if (chestDuring != null) {
            chestDuring.writeToNBT(during);
        }
        NeoStates.setWaterlogged(w, x, y, z, false);
        check("baú aceita água", on);
        check("baú: mesma TileEntity", chest != null && chestDuring == chest && w.getTileEntity(x, y, z) == chest);
        check("baú: NBT idêntico com água", chest != null && before.equals(during));
        check("baú: bloco e metadata intactos", w.getBlock(x, y, z) == Blocks.chest);

        // 4. placing a holder into a water source fills it; setBlock(air) takes the water away
        x = this.bx + 5;
        w.setBlock(x, y, z, Blocks.water, 0, 2);
        w.setBlock(x, y, z, Blocks.stone_slab, 0, 2);
        check("laje colocada em água fonte nasce com água", logged(x, y, z));
        w.setBlock(x, y, z, Blocks.stonebrick, 0, 2);
        check("trocar por bloco cheio tira a água", !logged(x, y, z));
        w.setBlock(x, y, z, Blocks.flowing_water, 3, 2);
        w.setBlock(x, y, z, Blocks.stone_slab, 0, 2);
        check("laje colocada em água corrente fica seca", !logged(x, y, z));
        NeoStates.setWaterlogged(w, x, y, z, true);
        w.setBlock(x, y, z, Blocks.air, 0, 2);
        check("setBlock(ar) direto remove bloco e água", w.isAirBlock(x, y, z) && !logged(x, y, z));

        // 5. breaking (setBlockToAir) leaves the water
        w.setBlock(x, y, z, Blocks.stone_slab, 0, 2);
        NeoStates.setWaterlogged(w, x, y, z, true);
        w.setBlockToAir(x, y, z);
        check(
            "quebrar deixa água fonte",
            w.getBlock(x, y, z)
                .getMaterial() == Material.water && w.getBlockMetadata(x, y, z) == 0
                && !logged(x, y, z));
        w.setBlock(x, y, z, Blocks.air, 0, 2);

        // 6. buckets
        x = this.bx + 7;
        w.setBlock(x, y, z, Blocks.oak_stairs, 0, 2);
        ItemStack empty = WaterBuckets.use(w, x, y, z, new ItemStack(Items.water_bucket));
        check("balde de água enche a escada", empty != null && empty.getItem() == Items.bucket && logged(x, y, z));
        check(
            "balde de água em escada cheia: regra vanilla",
            WaterBuckets.use(w, x, y, z, new ItemStack(Items.water_bucket)) == null);
        ItemStack full = WaterBuckets.use(w, x, y, z, new ItemStack(Items.bucket));
        check("balde vazio tira a água", full != null && full.getItem() == Items.water_bucket && !logged(x, y, z));
        check(
            "balde em pedra: regra vanilla",
            WaterBuckets.use(w, this.bx, this.by, this.bz, new ItemStack(Items.water_bucket)) == null);

        int firedBefore = BukkitFlow.fired();

        // 7. a hosted source over a hole falls; the host is not written
        x = this.bx + 1;
        z = this.bz + 5;
        int high = this.by + 4;
        w.setBlock(x, high, z, Blocks.stone_slab, 8, 2);
        NeoStates.setWaterlogged(w, x, high, z, true);
        WaterTicks.spreadNow(w, x, high, z);
        check(
            "fonte hospedada sobre o vazio cai",
            w.getBlock(x, high - 1, z) == Blocks.flowing_water && w.getBlockMetadata(x, high - 1, z) >= 8);
        check(
            "hospedeiro intacto depois de espalhar",
            w.getBlock(x, high, z) == Blocks.stone_slab && w.getBlockMetadata(x, high, z) == 8 && logged(x, high, z));
        w.setBlock(x, high, z, Blocks.air, 0, 2);
        w.setBlock(x, high - 1, z, Blocks.air, 0, 2);

        // 8. on a floor it spreads sideways, and the water next to it stays fed by it. The centre of the platform:
        // every edge is equally far, so vanilla's slope search picks all four sides.
        x = this.bx + 4;
        z = this.bz + 4;
        w.setBlock(x, y, z, Blocks.stone_slab, 0, 2);
        NeoStates.setWaterlogged(w, x, y, z, true);
        WaterTicks.spreadNow(w, x, y, z);
        check(
            "fonte hospedada no chão espalha para o lado",
            w.getBlock(x + 1, y, z) == Blocks.flowing_water && w.getBlockMetadata(x + 1, y, z) == 1);
        Blocks.flowing_water.updateTick(w, x + 1, y, z, w.rand);
        check(
            "água vizinha continua alimentada (nível 1)",
            w.getBlock(x + 1, y, z)
                .getMaterial() == Material.water && w.getBlockMetadata(x + 1, y, z) == 1);
        NeoStates.setWaterlogged(w, x, y, z, false);
        Blocks.flowing_water.updateTick(w, x + 1, y, z, w.rand);
        Blocks.flowing_water.updateTick(w, x + 1, y, z, w.rand);
        check(
            "sem a fonte, a água vizinha seca",
            w.getBlock(x + 1, y, z)
                .getMaterial() != Material.water || w.getBlockMetadata(x + 1, y, z) != 1);

        // 9. plugins are asked before hosted water flows (Bukkit servers)
        if (BukkitFlow.available()) {
            check("o fluxo da água hospedada pergunta ao Bukkit (BlockFromToEvent)", BukkitFlow.fired() > firedBefore);
        } else {
            this.lines.add("--    sem Bukkit: evento de fluxo não se aplica");
        }

        // 10. the surface the eyes and the body use is the drawn one, 8/9 of a source
        x = this.bx + 7;
        z = this.bz + 7;
        w.setBlock(x, y, z, Blocks.water, 0, 2);
        if (NeoConfig.modernWaterPhysics) {
            check("olho a 0.5 de uma fonte: dentro", ModernWater.submerged(w, x + 0.5D, y + 0.5D, z + 0.5D));
            check(
                "olho a 0.95 de uma fonte: fora (a superfície é 8/9)",
                !ModernWater.submerged(w, x + 0.5D, y + 0.95D, z + 0.5D));
        }
        w.setBlock(x, y, z, Blocks.air, 0, 2);

        // 11. sinking and swimming up at 1.21.1's speeds, in a closed shaft of still water
        if (NeoConfig.modernWaterPhysics) {
            x = this.bx + 6;
            z = this.bz + 2;
            for (int h = 1; h <= 6; h++) {
                w.setBlock(x - 1, this.by + h, z, Blocks.glass, 0, 2);
                w.setBlock(x + 1, this.by + h, z, Blocks.glass, 0, 2);
                w.setBlock(x, this.by + h, z - 1, Blocks.glass, 0, 2);
                w.setBlock(x, this.by + h, z + 1, Blocks.glass, 0, 2);
                w.setBlock(x, this.by + h, z, Blocks.water, 0, 2);
            }
            ProbeEntity sinker = new ProbeEntity(w);
            sinker.setPosition(x + 0.5D, this.by + 4.5D, z + 0.5D);
            for (int tick = 0; tick < 60; tick++) {
                sinker.onUpdate();
            }
            check(
                "afunda a 0.025 por tick (ficou em " + String.format("%.4f", sinker.motionY) + ")",
                sinker.isInWater() && Math.abs(sinker.motionY + 0.025D) < 0.002D);
            ProbeEntity swimmer = new ProbeEntity(w);
            swimmer.setPosition(x + 0.5D, this.by + 1.1D, z + 0.5D);
            for (int tick = 0; tick < 22; tick++) {
                swimmer.setJumping(true);
                swimmer.onUpdate();
            }
            // 0.135 is what is left after the tick's drag and sinking; with the next tick's 0.04 it moves 0.175
            check(
                "sobe a 0.175 por tick com o pulo (ficou em " + String.format("%.4f", swimmer.motionY + 0.04D) + ")",
                swimmer.isInWater() && Math.abs(swimmer.motionY - 0.135D) < 0.005D);
        }

        // 12. a holder between two sources, over a solid block, fills up (1.21.1's rule for new sources)
        clear(true);
        x = this.bx + 4;
        z = this.bz + 1;
        w.setBlock(x, y, z, Blocks.stone_slab, 0, 2);
        w.setBlock(x - 1, y, z, Blocks.water, 0, 2);
        WaterTicks.spreadNow(w, x, y, z);
        check("laje com uma fonte ao lado continua seca", !logged(x, y, z));
        w.setBlock(x + 1, y, z, Blocks.water, 0, 2);
        WaterTicks.spreadNow(w, x, y, z);
        check("laje entre duas fontes, sobre bloco sólido, enche", logged(x, y, z));

        // 13. old light under water is brought up to date: 1 less per block of water, straight down and sideways
        clear(true);
        relightCheck();
    }

    /**
     * A shaft of water in stone, open to the sky, with one cell of water in its wall. Its sky light is overwritten
     * with what a world generated with water taking 3 per block has; the relight has to bring it to 1 per block.
     */
    private void relightCheck() {
        World w = this.world;
        int x = this.bx + 4;
        int z = this.bz + 4;
        int top = this.by + 6;
        int opacity = Blocks.water.getLightOpacity();
        if (w.provider.hasNoSky || opacity != 1) {
            this.lines.add("--    luz: sem céu ou água com opacidade " + opacity + ", não se aplica");
            return;
        }
        for (int h = 1; h <= 6; h++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    w.setBlock(x + dx, this.by + h, z + dz, Blocks.stone, 0, 2);
                }
            }
            w.setBlock(x, this.by + h, z, Blocks.water, 0, 2);
        }
        int pocketY = this.by + 2;
        w.setBlock(x - 1, pocketY, z, Blocks.water, 0, 2);
        int open = w.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Sky, x, top + 1, z);
        net.minecraft.world.chunk.Chunk chunk = w.getChunkFromBlockCoords(x, z);
        for (int h = 1; h <= 6; h++) {
            int depth = 7 - h;
            setSky(chunk, x, this.by + h, z, Math.max(0, open - 3 * depth));
        }
        setSky(chunk, x - 1, pocketY, z, 0);
        int raised = com.akashic.neostates.light.WaterRelight.relightNow(w, x >> 4, z >> 4);
        if (raised < 0) {
            this.lines.add("--    luz: os chunks em volta não estão carregados, não se aplica");
        } else {
            boolean column = true;
            StringBuilder got = new StringBuilder();
            for (int h = 6; h >= 1; h--) {
                int light = w.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Sky, x, this.by + h, z);
                got.append(light)
                    .append(h > 1 ? "," : "");
                column &= light == Math.max(0, open - (7 - h));
            }
            check("luz do céu desce 1 por bloco de água (do topo: " + got + ", céu " + open + ")", column);
            int pocket = w.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Sky, x - 1, pocketY, z);
            check("luz entra de lado na água vizinha (ficou " + pocket + ")", pocket == Math.max(0, open - 6));
            check(
                "o chunk refeito fica marcado",
                ((com.akashic.neostates.light.LightMark) chunk).neostates$lightMark() == 1);
        }
        clear(true);
    }

    private static void setSky(net.minecraft.world.chunk.Chunk chunk, int x, int y, int z, int value) {
        net.minecraft.world.chunk.storage.ExtendedBlockStorage section = chunk.getBlockStorageArray()[y >> 4];
        section.setExtSkylightValue(x & 15, y & 15, z & 15, value);
    }
}
