package com.akashic.neostates.debug;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import com.akashic.neostates.NeoStatesMod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * {@code /neostates rastro}: logs where a player is, tick by tick, for a while. A player is moved by its own client,
 * so the differences between the positions the server receives are the client's physics, measured without touching
 * the client.
 */
public final class MotionTrace {

    private static final class Run {

        final EntityPlayerMP player;
        int left;
        double x;
        double y;
        double z;

        Run(EntityPlayerMP player, int ticks) {
            this.player = player;
            this.left = ticks;
            this.x = player.posX;
            this.y = player.posY;
            this.z = player.posZ;
        }
    }

    private static final List<Run> RUNS = new ArrayList<>();

    public static void start(EntityPlayerMP player, int ticks) {
        RUNS.add(new Run(player, ticks));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || RUNS.isEmpty()) {
            return;
        }
        for (Iterator<Run> it = RUNS.iterator(); it.hasNext();) {
            Run run = it.next();
            EntityPlayerMP p = run.player;
            NeoStatesMod.LOG.info(
                String.format(
                    "rastro %s y=%.4f dx=%.4f dy=%.4f dz=%.4f agua=%s olhos=%s chao=%s",
                    p.getCommandSenderName(),
                    p.posY,
                    p.posX - run.x,
                    p.posY - run.y,
                    p.posZ - run.z,
                    p.isInWater(),
                    p.isInsideOfMaterial(net.minecraft.block.material.Material.water),
                    p.onGround));
            run.x = p.posX;
            run.y = p.posY;
            run.z = p.posZ;
            if (--run.left <= 0 || p.isDead) {
                it.remove();
            }
        }
    }
}
