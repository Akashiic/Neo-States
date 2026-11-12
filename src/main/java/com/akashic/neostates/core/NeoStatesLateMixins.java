package com.akashic.neostates.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

/** Mixins into other mods, only when they are there. */
@LateMixin
public class NeoStatesLateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.neostates.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>();
        if (FMLLaunchHandler.side()
            .isClient() && loadedMods.contains("angelica")) {
            mixins.add("angelica.MixinExtendedBlockStorageExt_Water");
            mixins.add("angelica.MixinWorldSlice_Water");
        }
        return mixins;
    }
}
