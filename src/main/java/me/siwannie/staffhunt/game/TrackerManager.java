package me.siwannie.staffhunt.game;

import me.siwannie.staffhunt.StaffHunt;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TrackerManager extends BukkitRunnable {
    private final GameManager gameManager;

    public TrackerManager(StaffHunt plugin) {
        this.gameManager = plugin.getGameManager();
    }

    @Override
    public void run() {
        if (!gameManager.isGameActive()) return;

        for (Player hunter : gameManager.getOnlineHunters()) {
            Player nearestStaff = null;
            double nearestDistance = Double.MAX_VALUE;

            for (Player staff : gameManager.getOnlineStaff()) {
                if (hunter.getWorld().equals(staff.getWorld())) {
                    double distance = hunter.getLocation().distanceSquared(staff.getLocation());
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestStaff = staff;
                    }
                }
            }

            // Update the compass target if a staff member was found
            if (nearestStaff != null) {
                hunter.setCompassTarget(nearestStaff.getLocation());

                // --- NEW: Tracker UI ---
                // Only show the action bar if they are actively holding the compass
                if (hunter.getItemInHand() != null && hunter.getItemInHand().getType() == org.bukkit.Material.COMPASS) {
                    long dist = Math.round(Math.sqrt(nearestDistance));
                    String msg = org.bukkit.ChatColor.YELLOW + "Tracking: " + org.bukkit.ChatColor.RED + nearestStaff.getName() + org.bukkit.ChatColor.GRAY + " (" + dist + "m)";
                    StaffHunt.getInstance().getUiManager().sendActionBar(hunter, msg);
                }
            }
        }
    }
}