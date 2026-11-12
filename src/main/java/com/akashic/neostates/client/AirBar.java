package com.akashic.neostates.client;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.akashic.neostates.NeoConfig;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.21.1 keeps the bubbles on screen while the air is coming back, not only under water. Forge draws them only
 * while the eyes are in water, so out of it this draws the same row, in the same place.
 */
public final class AirBar {

    @SubscribeEvent
    public void afterAir(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.AIR || !NeoConfig.modernAirRecovery) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.isInsideOfMaterial(Material.water)) {
            return; // under water Forge has drawn it
        }
        int air = player.getAir();
        if (air >= 300) {
            return;
        }
        int left = event.resolution.getScaledWidth() / 2 + 91;
        int top = event.resolution.getScaledHeight() - GuiIngameForge.right_height;
        int full = MathHelper.ceiling_double_int((double) (air - 2) * 10.0D / 300.0D);
        int partial = MathHelper.ceiling_double_int((double) air * 10.0D / 300.0D) - full;
        GL11.glEnable(GL11.GL_BLEND);
        for (int i = 0; i < full + partial; i++) {
            mc.ingameGUI.drawTexturedModalRect(left - i * 8 - 9, top, i < full ? 16 : 25, 18, 9, 9);
        }
        GL11.glDisable(GL11.GL_BLEND);
        GuiIngameForge.right_height += 10;
    }
}
