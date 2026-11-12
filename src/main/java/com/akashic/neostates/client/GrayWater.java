package com.akashic.neostates.client;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.NeoStatesMod;
import com.akashic.neostates.visual.GrayscaleFilter;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The grey water sprites: the still and flowing water textures of whatever resource pack is active, converted while the
 * atlas is stitched. Nothing of Mojang's 1.21.1 art is shipped.
 */
public final class GrayWater {

    private static volatile Sprite still;
    private static volatile Sprite flow;

    /** The grey icon for a side of a water block, or null while the vanilla (blue) one must be used. */
    public static IIcon icon(int side) {
        Sprite s = still;
        Sprite f = flow;
        if (s == null || f == null || !s.loaded || !f.loaded) {
            return null;
        }
        return side != 0 && side != 1 ? f : s;
    }

    /** Whether water is being drawn grey, so the biome colour has to be the whole colour. */
    public static boolean active() {
        return icon(1) != null;
    }

    @SubscribeEvent
    public void onStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) {
            return;
        }
        still = null;
        flow = null;
        if (!NeoConfig.modernWaterColor) {
            return;
        }
        Sprite s = new Sprite("neostates:water_still_gray", "water_still", GrayscaleFilter.STILL_MEAN);
        Sprite f = new Sprite("neostates:water_flow_gray", "water_flow", GrayscaleFilter.FLOW_MEAN);
        // setTextureEntry keeps an entry that is already there (a resource reload): reuse it, it loads again.
        event.map.setTextureEntry(s.getIconName(), s);
        event.map.setTextureEntry(f.getIconName(), f);
        TextureAtlasSprite registeredStill = event.map.getTextureExtry(s.getIconName());
        TextureAtlasSprite registeredFlow = event.map.getTextureExtry(f.getIconName());
        if (registeredStill instanceof Sprite && registeredFlow instanceof Sprite) {
            still = (Sprite) registeredStill;
            flow = (Sprite) registeredFlow;
        }
    }

    static final class Sprite extends TextureAtlasSprite {

        private final String source;
        private final int targetMean;
        volatile boolean loaded;

        Sprite(String name, String source, int targetMean) {
            super(name);
            this.source = source;
            this.targetMean = targetMean;
        }

        @Override
        public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
            return true;
        }

        /** @return false to have the sprite stitched (Forge's contract), true to leave it out */
        @Override
        public boolean load(IResourceManager manager, ResourceLocation location) {
            this.loaded = false;
            try {
                IResource resource = manager
                    .getResource(new ResourceLocation("minecraft", "textures/blocks/" + this.source + ".png"));
                BufferedImage image = ImageIO.read(resource.getInputStream());
                int width = image.getWidth();
                int height = image.getHeight();
                int[] pixels = new int[width * height];
                image.getRGB(0, 0, width, height, pixels, 0, width);
                GrayscaleFilter.apply(pixels, this.targetMean);
                BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                gray.setRGB(0, 0, width, height, pixels, 0, width);

                Minecraft mc = Minecraft.getMinecraft();
                BufferedImage[] levels = new BufferedImage[1 + mc.gameSettings.mipmapLevels];
                levels[0] = gray;
                AnimationMetadataSection animation = (AnimationMetadataSection) resource.getMetadata("animation");
                this.loadSprite(levels, animation, mc.gameSettings.anisotropicFiltering > 1);
                this.loaded = true;
                NeoStatesMod.LOG.info(
                    "Grey water sprite {} from {}: {}x{}, {} frames.",
                    this.getIconName(),
                    this.source,
                    width,
                    height,
                    this.getFrameCount());
                return false;
            } catch (IOException | RuntimeException e) {
                NeoStatesMod.LOG
                    .error("Grey water texture from {} failed; water keeps the vanilla look.", this.source, e);
                return true;
            }
        }
    }
}
