package com.akashic.neostates.debug;

import net.minecraft.block.material.Material;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;

import com.akashic.neostates.api.FluidView;
import com.akashic.neostates.api.NeoStates;

/**
 * {@code /neostates sonda}: what Neo States reads at the player against what vanilla decided this tick. It is the
 * seed of the telemetry in docs/adr/ADR-0003-pipeline-de-fluidos.md: a line where the two disagree is a delta.
 */
public class CommandNeoStates extends CommandBase {

    @Override
    public String getCommandName() {
        return "neostates";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/neostates sonda | agua <x> <y> <z> <on|off> | ler <x> <y> <z> | autoteste [<x> <y> <z>]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 5 && "agua".equals(args[0])) {
            int bx = parseInt(sender, args[1]);
            int by = parseInt(sender, args[2]);
            int bz = parseInt(sender, args[3]);
            boolean on = "on".equals(args[4]);
            boolean changed = NeoStates.setWaterlogged(sender.getEntityWorld(), bx, by, bz, on);
            say(
                sender,
                "água hospedada em " + bx
                    + " "
                    + by
                    + " "
                    + bz
                    + ": "
                    + (on ? "ligada" : "desligada")
                    + (changed ? "" : " (sem mudança)"));
            return;
        }
        if (args.length == 1 && "autoteste".equals(args[0])) {
            net.minecraft.util.ChunkCoordinates spawn = sender.getEntityWorld()
                .getSpawnPoint();
            // inside one chunk of the spawn area, which the server keeps loaded
            int sx = (spawn.posX >> 4 << 4) + 3;
            int sz = (spawn.posZ >> 4 << 4) + 3;
            for (String line : WaterSelfTest.run(sender.getEntityWorld(), sx, 120, sz)) {
                say(sender, line);
            }
            return;
        }
        if (args.length == 4 && "autoteste".equals(args[0])) {
            for (String line : WaterSelfTest.run(
                sender.getEntityWorld(),
                parseInt(sender, args[1]),
                parseInt(sender, args[2]),
                parseInt(sender, args[3]))) {
                say(sender, line);
            }
            return;
        }
        if (args.length == 4 && "ler".equals(args[0])) {
            int bx = parseInt(sender, args[1]);
            int by = parseInt(sender, args[2]);
            int bz = parseInt(sender, args[3]);
            net.minecraft.world.World world = sender.getEntityWorld();
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(bx, by, bz);
            say(
                sender,
                bx + " "
                    + by
                    + " "
                    + bz
                    + ": bloco="
                    + net.minecraft.block.Block.blockRegistry.getNameForObject(world.getBlock(bx, by, bz))
                    + " meta="
                    + world.getBlockMetadata(bx, by, bz)
                    + " tile="
                    + (tile == null ? "-"
                        : tile.getClass()
                            .getSimpleName())
                    + " hospedada="
                    + NeoStates.isWaterlogged(world, bx, by, bz)
                    + " fluido="
                    + describe(NeoStates.fluidAt(world, bx, by, bz)));
            return;
        }
        if (args.length != 1 || !"sonda".equals(args[0])) {
            throw new WrongUsageException(this.getCommandUsage(sender));
        }
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        int x = MathHelper.floor_double(player.posX);
        int z = MathHelper.floor_double(player.posZ);
        double feetY = player.boundingBox.minY;
        double eyeY = player.posY + player.getEyeHeight();
        int feet = MathHelper.floor_double(feetY + 0.001D);
        int eyes = MathHelper.floor_double(eyeY);

        double feetSurface = NeoStates.surfaceY(player.worldObj, x, feet, z);
        double eyeSurface = NeoStates.surfaceY(player.worldObj, x, eyes, z);
        boolean modernInWater = feetSurface >= feetY + 0.001D;
        boolean modernEyes = eyeSurface > eyeY;

        say(
            sender,
            "pés  y=" + feet
                + " "
                + describe(NeoStates.fluidAt(player.worldObj, x, feet, z))
                + " superfície="
                + format(feetSurface));
        say(
            sender,
            "olhos y=" + eyes
                + " "
                + describe(NeoStates.fluidAt(player.worldObj, x, eyes, z))
                + " superfície="
                + format(eyeSurface));
        say(
            sender,
            "na água: vanilla=" + player.isInWater()
                + " 1.21.1="
                + modernInWater
                + (player.isInWater() != modernInWater ? "  <- delta" : ""));
        boolean vanillaEyes = player.isInsideOfMaterial(Material.water);
        say(
            sender,
            "olhos na água: vanilla=" + vanillaEyes
                + " 1.21.1="
                + modernEyes
                + (vanillaEyes != modernEyes ? "  <- delta" : ""));
    }

    private static String describe(int view) {
        if (FluidView.isEmpty(view)) {
            return "sem fluido";
        }
        return (FluidView.isWater(view) ? "água" : "lava") + " nível "
            + FluidView.amount(view)
            + (FluidView.isSource(view) ? " fonte" : "")
            + (FluidView.isFalling(view) ? " queda" : "")
            + (FluidView.isHosted(view) ? " hospedada" : "");
    }

    private static String format(double y) {
        return Double.isNaN(y) ? "-" : String.format("%.4f", y);
    }

    private static void say(ICommandSender sender, String line) {
        sender.addChatMessage(new ChatComponentText("[Neo States] " + line));
    }
}
