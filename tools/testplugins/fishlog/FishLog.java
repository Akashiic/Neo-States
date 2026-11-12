package fishlog;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.akashic.kunftig.api.bukkit.fishing.KunftigFishEvent;

/** Test only: writes Kunftig's and Bukkit's fishing events to the console, and makes the wait short. */
public class FishLog extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onKunftig(KunftigFishEvent event) {
        org.bukkit.Location at = event.getHookEntity().getLocation();
        getLogger().info("KUNFTIG " + event.getState() + " hook=" + at.getBlockX() + "," + String.format("%.2f", at.getY()) + "," + at.getBlockZ()
            + " block=" + at.getBlock().getType());
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        getLogger().info("BUKKIT " + event.getState() + (event.getCaught() != null ? " caught=" + event.getCaught().getType() : ""));
    }
}
