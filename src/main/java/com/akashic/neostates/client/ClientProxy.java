package com.akashic.neostates.client;

import net.minecraftforge.common.MinecraftForge;

import com.akashic.neostates.CommonProxy;

import cpw.mods.fml.common.FMLCommonHandler;

public class ClientProxy extends CommonProxy {

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(new GrayWater());
        MinecraftForge.EVENT_BUS.register(new WaterColorHandler());
        UnderwaterFog fog = new UnderwaterFog();
        MinecraftForge.EVENT_BUS.register(fog);
        FMLCommonHandler.instance()
            .bus()
            .register(fog);
    }

    @Override
    public void postInit() {
        BiomeWaterColors.rebuild();
    }
}
