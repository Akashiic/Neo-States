package com.akashic.neostates.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * Loads the early mixins (vanilla and Forge classes). Every injection is required ({@code defaultRequire = 1}): a
 * target that moved fails the start instead of running half applied.
 */
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("NeoStatesCore")
@IFMLLoadingPlugin.SortingIndex(1100)
public class NeoStatesCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.neostates.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        List<String> mixins = new ArrayList<>();
        mixins.add("common.MixinExtendedBlockStorage_Water");
        mixins.add("common.MixinBlock_Holder");
        mixins.add("common.MixinChunkCache_Water");
        mixins.add("common.BlockDynamicLiquidInvoker");
        mixins.add("common.BlockLiquidInvoker");
        mixins.add("common.MixinChunk_Water");
        mixins.add("common.MixinWorld_Water");
        mixins.add("common.MixinEntity_Water");
        mixins.add("common.MixinBlockLiquid_Water");
        mixins.add("common.MixinEntity_ModernWater");
        mixins.add("common.MixinEntityLivingBase_ModernWater");
        mixins.add("common.MixinEntityItem_Float");
        mixins.add("common.MixinEntityItem_OneFloat");
        mixins.add("common.MixinEntityXPOrb_Float");
        mixins.add("common.MixinEntityLivingBase_Air");
        mixins.add("common.MixinBlockDynamicLiquid_Holders");
        if (FMLLaunchHandler.side()
            .isClient()) {
            mixins.add("client.MixinBlockLiquid_GrayIcon");
            mixins.add("client.MixinActiveRenderInfo_Water");
            mixins.add("client.MixinBlock_WaterPass");
            // Vanilla's chunk renderer. Angelica replaces it (and rewrites the very call this mixin wraps), so the
            // mixin is for the game without Angelica only.
            if (!present("com/gtnewhorizons/angelica/AngelicaMod.class")) {
                mixins.add("client.MixinWorldRenderer_WaterPass");
            }
            mixins.add("client.MixinRenderBlocks_Water");
            mixins.add("client.MixinBlockLiquid_WaterFaces");
        }
        return mixins;
    }

    private static boolean present(String resource) {
        return NeoStatesCore.class.getClassLoader()
            .getResource(resource) != null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
