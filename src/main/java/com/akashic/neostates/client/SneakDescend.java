package com.akashic.neostates.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.event.entity.living.LivingEvent;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.physics.WaterPhysics;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.21.1's {@code LocalPlayer#aiStep}: the local player sinks faster while sneaking in water. Forge posts this event
 * at the start of the entity's update, before its movement, which is where 1.21.1 applies it.
 */
public final class SneakDescend {

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!NeoConfig.modernWaterPhysics || event.entityLiving != Minecraft.getMinecraft().thePlayer) {
            return;
        }
        EntityPlayerSP player = (EntityPlayerSP) event.entityLiving;
        if (player.isInWater() && player.movementInput != null
            && player.movementInput.sneak
            && !player.capabilities.isFlying) {
            player.motionY -= WaterPhysics.SNEAK;
        }
    }
}
