package com.akashic.neostates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.akashic.neostates.debug.CommandNeoStates;
import com.akashic.neostates.fluid.FluidPipeline;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = NeoStatesMod.MODID, name = "Neo States", version = Tags.VERSION, acceptedMinecraftVersions = "[1.7.10]")
public class NeoStatesMod {

    public static final String MODID = "neostates";
    public static final Logger LOG = LogManager.getLogger("NeoStates");

    @SidedProxy(
        clientSide = "com.akashic.neostates.client.ClientProxy",
        serverSide = "com.akashic.neostates.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NeoConfig.load(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        FluidPipeline.freeze();
        proxy.postInit();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandNeoStates());
    }
}
