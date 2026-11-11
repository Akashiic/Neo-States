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

    public ProbeEntity(World world) {
        super(world);
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
