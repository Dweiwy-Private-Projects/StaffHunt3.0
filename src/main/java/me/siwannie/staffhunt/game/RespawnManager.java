package me.siwannie.staffhunt.game;

import me.siwannie.staffhunt.StaffHunt;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World; // <-- ADD THIS IMPORT
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

public class RespawnManager {

    private final StaffHunt plugin;

    public RespawnManager(StaffHunt plugin) {
        this.plugin = plugin;
    }

    public void handlePlayerDeath(Player player) {
        // This is called by our FatalDamageListener while the player is still technically alive.
        // We reset their stats to ensure a clean transition into spectator mode.
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setFireTicks(0);

        // Now, we can safely put them in spectator mode and start the countdown.
        player.setGameMode(GameMode.SPECTATOR);

        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!player.isOnline()) { // Cancel if player logs off
                    this.cancel();
                    return;
                }

                if (countdown > 0) {
                    sendTitle(player, ChatColor.RED + "Respawning in...", ChatColor.YELLOW.toString() + countdown);
                    countdown--;
                } else {
                    respawnPlayer(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void respawnPlayer(Player player) {
        GameManager gameManager = plugin.getGameManager();
        Location respawnLocation = gameManager.isStaff(player) ? gameManager.getStaffSpawn() : gameManager.getPlayerSpawn();

        if (respawnLocation == null) {
            respawnLocation = player.getWorld().getSpawnLocation();
        }

        GameMode newMode = gameManager.isHunter(player) ? GameMode.ADVENTURE : GameMode.SURVIVAL;
        player.setGameMode(newMode);
        player.teleport(respawnLocation);
        String rank = gameManager.isStaff(player) ? gameManager.getEffectiveRank(player) : null;
        plugin.getKitManager().applyKit(player, rank);

        // --- NEW: Restore saved enchants (minus 1 if they didn't have a shield) ---
        me.siwannie.staffhunt.player.PlayerProfile profile = plugin.getPlayerProfileManager().getProfile(player.getUniqueId());
        if (profile != null) {
            profile.restoreLoadout(player);
        }

        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // --- NEW INVINCIBILITY LOGIC ---
        int invincibilityTime = plugin.getConfig().getInt("respawn-invincibility-seconds", 5);
        if (invincibilityTime > 0) {
            player.setMetadata("respawn_invincibility", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
            player.sendMessage(ChatColor.AQUA + "You have respawn invincibility for " + invincibilityTime + " seconds!");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.hasMetadata("respawn_invincibility")) {
                        player.removeMetadata("respawn_invincibility", plugin);
                        plugin.getUiManager().sendActionBar(player, ChatColor.YELLOW + "Your respawn invincibility has worn off.");
                    }
                }
            }.runTaskLater(plugin, invincibilityTime * 20L);
        }

        sendTitle(player, ChatColor.GREEN + "Respawned!", "");
    }

    /**
     * Sends a title to a player, but temporarily disables command feedback
     * to prevent spamming the server console.
     * @param player The player to send the title to.
     * @param title The main title text.
     * @param subtitle The subtitle text.
     */
    private void sendTitle(Player player, String title, String subtitle) {
        if (title == null) title = "";
        if (subtitle == null) subtitle = "";

        // Temporarily disable command feedback to prevent console spam
        World world = player.getWorld();
        String originalGameRule = world.getGameRuleValue("sendCommandFeedback");
        world.setGameRuleValue("sendCommandFeedback", "false");

        // Dispatch the title commands
        String timesCommand = String.format("title %s times 10 40 10", player.getName());
        String subtitleCommand = String.format("title %s subtitle {\"text\":\"%s\"}", player.getName(), subtitle.replace("\"", "\\\""));
        String titleCommand = String.format("title %s title {\"text\":\"%s\"}", player.getName(), title.replace("\"", "\\\""));

        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), timesCommand);
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), subtitleCommand);
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), titleCommand);

        // Restore the original game rule value
        world.setGameRuleValue("sendCommandFeedback", originalGameRule);
    }
}