package noflow;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Test only: stands in for a protection plugin. While on, no liquid may flow anywhere. */
public class NoFlow extends JavaPlugin implements Listener {

    private boolean on;
    private int cancelled;
    private int seen;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onFlow(BlockFromToEvent event) {
        seen++;
        if (on) {
            event.setCancelled(true);
            cancelled++;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            on = "on".equals(args[0]);
        }
        sender.sendMessage("[NoFlow] on=" + on + " seen=" + seen + " cancelled=" + cancelled);
        return true;
    }
}
