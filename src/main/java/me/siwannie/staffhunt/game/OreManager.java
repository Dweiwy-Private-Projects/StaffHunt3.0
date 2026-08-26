package me.siwannie.staffhunt.game;

import me.siwannie.staffhunt.StaffHunt;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OreManager {

    private final StaffHunt plugin;
    private final Map<Location, Material> originalOres = new ConcurrentHashMap<>();

    public OreManager(StaffHunt plugin) {
        this.plugin = plugin;
    }

    public void startRegeneration(Block block, int regenTimeSeconds) {
        Location loc = block.getLocation();
        Material originalType = block.getType();

        if (originalOres.containsKey(loc)) {
            return; // Already regenerating
        }

        originalOres.put(loc, originalType);
        block.setType(Material.BEDROCK);

        new BukkitRunnable() {
            @Override
            public void run() {
                revertOre(loc);
            }
        }.runTaskLater(plugin, regenTimeSeconds * 20L); // Use the provided time
    }

    private void revertOre(Location loc) {
        Material originalType = originalOres.remove(loc);
        if (originalType != null) {
            loc.getBlock().setType(originalType);
        }
    }

    public void revertAllOres() {
        for (Map.Entry<Location, Material> entry : originalOres.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }
        originalOres.clear();
    }
}