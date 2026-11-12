package com.akashic.neostates.debug;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * A living entity with no mind of its own, for the self-test: it is never added to the world, only updated by hand,
 * so what moves it is the game's physics and nothing else.
 */
public class ProbeEntity extends EntityLivingBase {

    private final ItemStack[] none = new ItemStack[5];

    private final boolean likeClientPlayer;

    public ProbeEntity(World world) {
        this(world, false);
    }

    /**
     * @param likeClientPlayer as a 1.7.10 client keeps its own player: {@code posY} at the eyes ({@code yOffset}
     *                         1.62) and {@code getEyeHeight()} 0.12
     */
    public ProbeEntity(World world, boolean likeClientPlayer) {
        super(world);
        this.likeClientPlayer = likeClientPlayer;
        if (likeClientPlayer) {
            this.yOffset = 1.62F;
        }
    }

    @Override
    public float getEyeHeight() {
        return this.likeClientPlayer ? 0.12F : super.getEyeHeight();
    }

    @Override
    public ItemStack getHeldItem() {
        return null;
    }

    @Override
    public ItemStack getEquipmentInSlot(int slot) {
        return null;
    }

    @Override
    public void setCurrentItemOrArmor(int slot, ItemStack stack) {}

    @Override
    public ItemStack[] getLastActiveItems() {
        return this.none;
    }

    @Override
    protected void updateEntityActionState() {}

    @Override
    protected void collideWithNearbyEntities() {}
}
