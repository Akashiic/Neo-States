package com.akashic.neostates.client;

import net.minecraftforge.event.terraingen.BiomeEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Forge asks every biome for its water colour through this event, and Angelica keeps asking from its meshing
 * threads: no state here, only a table read.
 */
public final class WaterColorHandler {

    @SubscribeEvent
    public void onWaterColor(BiomeEvent.GetWaterColor event) {
        if (GrayWater.active()) {
            event.newColor = BiomeWaterColors.water(event.biome, event.originalColor);
        }
    }
}
