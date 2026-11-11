package com.akashic.neostates.client;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.EntityViewRenderEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.visual.UnderwaterMath;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * 1.21.1's underwater fog through Forge's two view events, so nothing is injected into the fog methods of
 * {@code EntityRenderer} (other mods anchor there; see docs/COMPATIBILIDADE.md).
 */
public final class UnderwaterFog {

    private int visionTicks;
    private boolean eyesInWater;

    private int targetColor = -1;
    private int previousColor = -1;
    private long changedAt = -1L;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            this.visionTicks = 0;
            this.eyesInWater = false;
            return;
        }
        if (mc.isGamePaused()) {
            return;
        }
        this.eyesInWater = mc.thePlayer.isInsideOfMaterial(Material.water);
        this.visionTicks = UnderwaterMath.tickVision(this.visionTicks, this.eyesInWater);
    }

    private float vision() {
        return UnderwaterMath.vision(this.visionTicks, this.eyesInWater);
    }

    private static BiomeGenBase biomeAt(EntityLivingBase entity) {
        return entity.worldObj
            .getBiomeGenForCoords(MathHelper.floor_double(entity.posX), MathHelper.floor_double(entity.posZ));
    }

    /** Fired at the end of {@code EntityRenderer.updateFogColor}: what is set here is the final colour. */
    @SubscribeEvent
    public void onFogColor(EntityViewRenderEvent.FogColors event) {
        if (!NeoConfig.modernWaterFog || event.block.getMaterial() != Material.water) {
            this.changedAt = -1L;
            return;
        }
        int color = BiomeWaterColors.fog(biomeAt(event.entity));
        long now = Minecraft.getSystemTime();
        if (this.changedAt < 0L) {
            this.targetColor = color;
            this.previousColor = color;
            this.changedAt = now;
        }
        long elapsed = now - this.changedAt;
        float[] rgb = new float[3];
        for (int i = 0; i < 3; i++) {
            int shift = 16 - 8 * i;
            rgb[i] = UnderwaterMath.fade(this.previousColor >> shift & 255, this.targetColor >> shift & 255, elapsed);
        }
        if (this.targetColor != color) {
            this.targetColor = color;
            this.previousColor = MathHelper.floor_float(rgb[0]) << 16 | MathHelper.floor_float(rgb[1]) << 8
                | MathHelper.floor_float(rgb[2]);
            this.changedAt = now;
        }
        for (int i = 0; i < 3; i++) {
            rgb[i] /= 255.0F;
        }
        UnderwaterMath.brighten(rgb, this.vision());
        event.red = rgb[0];
        event.green = rgb[1];
        event.blue = rgb[2];
    }

    /**
     * Fired at the start of {@code EntityRenderer.setupFog}. Cancelling it makes vanilla skip its own fog setup and
     * only set the density, so the linear fog configured here stands. LOWEST and not receiving cancelled events: a
     * mod that already took the fog (Et Futurum's spectator mode) keeps it.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
        if (!NeoConfig.modernWaterFog || event.block.getMaterial() != Material.water) {
            return;
        }
        // vanilla gives blindness its own fog before it looks at water; cancelling here would take that away
        if (event.entity.isPotionActive(Potion.blindness)) {
            return;
        }
        float renderDistance = Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16.0F;
        float end = UnderwaterMath.fogEnd(this.vision(), BiomeWaterColors.closerFog(biomeAt(event.entity)));
        if (end > renderDistance) {
            end = renderDistance;
        }
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        GL11.glFogf(GL11.GL_FOG_START, UnderwaterMath.FOG_START);
        GL11.glFogf(GL11.GL_FOG_END, end);
        if (GLContext.getCapabilities().GL_NV_fog_distance) {
            GL11.glFogi(34138, 34139);
        }
        event.density = 0.1F;
        event.setCanceled(true);
    }
}
