package com.akashic.neostates.water;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import net.minecraft.world.World;

import com.akashic.neostates.NeoStatesMod;

/**
 * On a Bukkit server (Crucible), vanilla's flowing water asks plugins before it enters a cell, with
 * {@code BlockFromToEvent}; protection plugins cancel it at the border of a region. Hosted water spreads by our own
 * code, so it asks the same question here. Everything is looked up by method handle (the pack's PlayerContainerFix
 * makes reflection over {@code org.bukkit.World} unsafe), and a server without Bukkit always answers yes.
 */
public final class BukkitFlow {

    private static final MethodHandle GET_WORLD;
    private static final MethodHandle GET_BLOCK_AT;
    private static final MethodHandle NEW_EVENT;
    private static final MethodHandle PLUGIN_MANAGER;
    private static final MethodHandle CALL_EVENT;
    private static final MethodHandle IS_CANCELLED;
    private static final Object[] FACES = new Object[6];
    private static final boolean AVAILABLE;
    private static boolean warned;
    private static int fired;

    static {
        MethodHandle getWorld = null;
        MethodHandle getBlockAt = null;
        MethodHandle newEvent = null;
        MethodHandle pluginManager = null;
        MethodHandle callEvent = null;
        MethodHandle isCancelled = null;
        boolean ok = false;
        try {
            Class<?> craftWorld = Class.forName("org.bukkit.craftbukkit.v1_7_R4.CraftWorld");
            Class<?> block = Class.forName("org.bukkit.block.Block");
            Class<?> face = Class.forName("org.bukkit.block.BlockFace");
            Class<?> event = Class.forName("org.bukkit.event.block.BlockFromToEvent");
            Class<?> baseEvent = Class.forName("org.bukkit.event.Event");
            Class<?> manager = Class.forName("org.bukkit.plugin.PluginManager");
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            getWorld = lookup.findVirtual(World.class, "getWorld", MethodType.methodType(craftWorld))
                .asType(MethodType.methodType(Object.class, World.class));
            getBlockAt = lookup
                .findVirtual(craftWorld, "getBlockAt", MethodType.methodType(block, int.class, int.class, int.class))
                .asType(MethodType.methodType(Object.class, Object.class, int.class, int.class, int.class));
            newEvent = lookup.findConstructor(event, MethodType.methodType(void.class, block, face))
                .asType(MethodType.methodType(Object.class, Object.class, Object.class));
            pluginManager = lookup.findStatic(bukkit, "getPluginManager", MethodType.methodType(manager))
                .asType(MethodType.methodType(Object.class));
            callEvent = lookup.findVirtual(manager, "callEvent", MethodType.methodType(void.class, baseEvent))
                .asType(MethodType.methodType(void.class, Object.class, Object.class));
            isCancelled = lookup.findVirtual(event, "isCancelled", MethodType.methodType(boolean.class))
                .asType(MethodType.methodType(boolean.class, Object.class));
            // the order of Forge's sides: down, up, north, south, west, east
            String[] names = { "DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST" };
            for (int i = 0; i < names.length; i++) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                Object value = Enum.valueOf((Class) face, names[i]);
                FACES[i] = value;
            }
            ok = true;
        } catch (ClassNotFoundException e) {
            // not a Bukkit server
        } catch (ReflectiveOperationException | LinkageError e) {
            NeoStatesMod.LOG.error(
                "Bukkit is present but its flow event could not be linked: hosted water will not ask plugins.",
                e);
        }
        GET_WORLD = getWorld;
        GET_BLOCK_AT = getBlockAt;
        NEW_EVENT = newEvent;
        PLUGIN_MANAGER = pluginManager;
        CALL_EVENT = callEvent;
        IS_CANCELLED = isCancelled;
        AVAILABLE = ok;
    }

    private BukkitFlow() {}

    public static boolean available() {
        return AVAILABLE;
    }

    /** How many events were handed to Bukkit since the server started (for the self-test). */
    public static int fired() {
        return fired;
    }

    /**
     * @param side Forge's side of the source cell the water leaves through: 0 down, 2 north, 3 south, 4 west, 5 east
     * @return false when a plugin cancelled the flow
     */
    public static boolean allows(World world, int x, int y, int z, int side) {
        if (!AVAILABLE) {
            return true;
        }
        try {
            Object craftWorld = GET_WORLD.invokeExact(world);
            if (craftWorld == null) {
                return true;
            }
            Object source = GET_BLOCK_AT.invokeExact(craftWorld, x, y, z);
            Object event = NEW_EVENT.invokeExact(source, FACES[side]);
            Object manager = PLUGIN_MANAGER.invokeExact();
            CALL_EVENT.invokeExact(manager, event);
            fired++;
            return !(boolean) IS_CANCELLED.invokeExact(event);
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                NeoStatesMod.LOG.error("Asking Bukkit about hosted water flow failed; the flow is refused.", t);
            }
            // fail closed: water that cannot ask does not cross
            return false;
        }
    }
}
