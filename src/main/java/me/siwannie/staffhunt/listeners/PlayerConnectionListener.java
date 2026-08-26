package me.siwannie.staffhunt.listeners;

import me.siwannie.staffhunt.StaffHunt;
import me.siwannie.staffhunt.game.GameManager;
import me.siwannie.staffhunt.player.PlayerProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final PlayerProfileManager profileManager;
    private final GameManager gameManager;

    public PlayerConnectionListener(StaffHunt plugin) {
        this.profileManager = plugin.getPlayerProfileManager(); //
        this.gameManager = plugin.getGameManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        profileManager.createProfile(player.getUniqueId());

        if (gameManager.isGameActive()) {
            // Add late joiners to the game lists if they aren't already in them
            if (!gameManager.isStaff(player) && !gameManager.isHunter(player)) {
                gameManager.addLateJoiner(player);
            }

            // Delay the teleport and kit application to bypass Essentials/Multiverse
            org.bukkit.Bukkit.getScheduler().runTaskLater(StaffHunt.getInstance(), () -> {
                if (!player.isOnline()) return;

                if (gameManager.isStaff(player)) {
                    gameManager.teleportToStaffSpawn(player);
                    String rank = gameManager.getEffectiveRank(player);
                    StaffHunt.getInstance().getKitManager().applyKit(player, rank);
                    player.sendMessage(org.bukkit.ChatColor.GREEN + "You have been teleported to the Staff spawn with your kit.");
                } else if (gameManager.isHunter(player)) {
                    gameManager.teleportToHunterSpawn(player);
                    StaffHunt.getInstance().getKitManager().applyKit(player, null);
                    player.sendMessage(org.bukkit.ChatColor.RED + "You have been teleported to the Hunter spawn with your kit.");
                }
            }, 10L); // 10 ticks = 0.5 second delay
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // If a game is active, handle removing the player from it.
        if (gameManager.isGameActive()) {
            gameManager.handlePlayerDisconnect(player);
        }

        // Clean up the player's session profile regardless of game state.
        profileManager.removeProfile(player.getUniqueId()); //
    }
}