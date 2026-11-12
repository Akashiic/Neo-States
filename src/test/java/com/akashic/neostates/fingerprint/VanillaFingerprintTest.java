package com.akashic.neostates.fingerprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Pins the shape of the vanilla methods the seams of docs/adr will target, against the real 1.7.10 + Forge classes
 * (MCP names, as on the development classpath). A count that changes here means a seam would land somewhere else.
 * The same counts have to be checked against Crucible's server jar before a seam ships (docs/COMPATIBILIDADE.md).
 */
class VanillaFingerprintTest {

    private static final String WORLD = "net/minecraft/world/World";
    private static final String ACCESS = "net/minecraft/world/IBlockAccess";
    private static final String GET_BLOCK = "getBlock";
    private static final String GET_META = "getBlockMetadata";

    private static MethodNode method(String owner, String name, String desc) throws IOException {
        ClassNode node = new ClassNode();
        try (InputStream in = VanillaFingerprintTest.class.getClassLoader()
            .getResourceAsStream(owner + ".class")) {
            assertNotNull(in, owner + " is not on the test classpath");
            new ClassReader(in).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        MethodNode found = null;
        for (MethodNode m : node.methods) {
            if (m.name.equals(name) && m.desc.equals(desc)) {
                assertEquals(null, found, owner + "#" + name + desc + " is not unique");
                found = m;
            }
        }
        assertNotNull(found, owner + "#" + name + desc);
        return found;
    }

    private static int calls(MethodNode m, String owner, String name) {
        int n = 0;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) {
                n++;
            }
        }
        return n;
    }

    /** {@code LDC value} whose next real instruction is {@code opcode} (-1: any). */
    private static int doubles(MethodNode m, double value, int opcode) {
        int n = 0;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (!(i instanceof LdcInsnNode ldc) || !(ldc.cst instanceof Double d) || d != value) {
                continue;
            }
            AbstractInsnNode next = i.getNext();
            while (next != null && next.getOpcode() < 0) {
                next = next.getNext();
            }
            if (opcode < 0 || next != null && next.getOpcode() == opcode) {
                n++;
            }
        }
        return n;
    }

    // --- contracts of other mods Neo States must keep (docs/COMPATIBILIDADE.md, rules 1 to 3) ---

    /** Akashic Player Backport: exactly three 0.8 water-drag multiplies. */
    @Test
    void moveEntityWithHeadingKeepsThreeWaterDrags() throws IOException {
        MethodNode m = method("net/minecraft/entity/EntityLivingBase", "moveEntityWithHeading", "(FF)V");
        assertEquals(3, doubles(m, 0.800000011920929D, Opcodes.DMUL));
        // the water and lava gravity (delta D-3 changes the first)
        assertEquals(2, doubles(m, 0.02D, Opcodes.DSUB));
    }

    /** BelowCore: the only 4.0D of setupFog. */
    @Test
    void setupFogHasOneFour() throws IOException {
        MethodNode m = method("net/minecraft/client/renderer/EntityRenderer", "setupFog", "(IF)V");
        assertEquals(1, doubles(m, 4.0D, -1));
    }

    /** Kunftig: the only isAABBInMaterial call of the fish hook. */
    @Test
    void fishHookAsksOnceForTheMaterial() throws IOException {
        MethodNode m = method("net/minecraft/entity/projectile/EntityFishHook", "onUpdate", "()V");
        assertEquals(1, calls(m, WORLD, "isAABBInMaterial"));
    }

    // --- the read seams (docs/adr/ADR-0001, "Leitura") ---

    @Test
    void handleMaterialAcceleration() throws IOException {
        MethodNode m = method(
            WORLD,
            "handleMaterialAcceleration",
            "(Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/block/material/Material;Lnet/minecraft/entity/Entity;)Z");
        assertEquals(1, calls(m, WORLD, GET_BLOCK));
        assertEquals(1, calls(m, WORLD, GET_META));
        assertEquals(1, calls(m, "net/minecraft/block/Block", "velocityToAddToEntity"));
        assertEquals(1, calls(m, "net/minecraft/util/Vec3", "normalize"));
        assertEquals(1, doubles(m, 0.014D, -1));
    }

    @Test
    void handleWaterMovementCutsTheBox() throws IOException {
        MethodNode m = method("net/minecraft/entity/Entity", "handleWaterMovement", "()Z");
        assertEquals(1, calls(m, WORLD, "handleMaterialAcceleration"));
        assertEquals(1, doubles(m, -0.4000000059604645D, -1));
    }

    @Test
    void boxQueriesReadTheBlockOnce() throws IOException {
        String box = "(Lnet/minecraft/util/AxisAlignedBB;)Z";
        String boxMaterial = "(Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/block/material/Material;)Z";
        assertEquals(1, calls(method(WORLD, "isAnyLiquid", box), WORLD, GET_BLOCK));
        assertEquals(1, calls(method(WORLD, "isMaterialInBB", boxMaterial), WORLD, GET_BLOCK));
        MethodNode inMaterial = method(WORLD, "isAABBInMaterial", boxMaterial);
        assertEquals(1, calls(inMaterial, WORLD, GET_BLOCK));
        assertEquals(1, calls(inMaterial, WORLD, GET_META));
    }

    @Test
    void eyesAndCamera() throws IOException {
        MethodNode eyes = method(
            "net/minecraft/entity/Entity",
            "isInsideOfMaterial",
            "(Lnet/minecraft/block/material/Material;)Z");
        assertEquals(1, calls(eyes, WORLD, GET_BLOCK));
        MethodNode camera = method(
            "net/minecraft/client/renderer/ActiveRenderInfo",
            "getBlockAtEntityViewpoint",
            "(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)Lnet/minecraft/block/Block;");
        assertEquals(2, calls(camera, WORLD, GET_BLOCK));
        assertEquals(1, calls(camera, WORLD, GET_META));
    }

    @Test
    void liquidBlockReads() throws IOException {
        String liquid = "net/minecraft/block/BlockLiquid";
        MethodNode decay = method(liquid, "getEffectiveFlowDecay", "(Lnet/minecraft/world/IBlockAccess;III)I");
        assertEquals(1, calls(decay, ACCESS, GET_BLOCK));
        assertEquals(1, calls(decay, ACCESS, GET_META));
        MethodNode raw = method(liquid, "func_149804_e", "(Lnet/minecraft/world/World;III)I");
        assertEquals(1, calls(raw, WORLD, GET_BLOCK));
        assertEquals(1, calls(raw, WORLD, GET_META));
        MethodNode side = method(liquid, "shouldSideBeRendered", "(Lnet/minecraft/world/IBlockAccess;IIII)Z");
        assertEquals(1, calls(side, ACCESS, GET_BLOCK));
        MethodNode corner = method(
            "net/minecraft/client/renderer/RenderBlocks",
            "getLiquidHeight",
            "(IIILnet/minecraft/block/material/Material;)F");
        assertEquals(2, calls(corner, ACCESS, GET_BLOCK));
        assertEquals(1, calls(corner, ACCESS, GET_META));
    }

    /** The vanilla pass loop: one getRenderBlockPass and one canRenderInPass (docs/adr/ADR-0002, V1). */
    @Test
    void worldRendererPassLoop() throws IOException {
        MethodNode m = method(
            "net/minecraft/client/renderer/WorldRenderer",
            "updateRenderer",
            "(Lnet/minecraft/entity/EntityLivingBase;)V");
        assertEquals(1, calls(m, "net/minecraft/block/Block", "getRenderBlockPass"));
        assertEquals(1, calls(m, "net/minecraft/block/Block", "canRenderInPass"));
    }
}
