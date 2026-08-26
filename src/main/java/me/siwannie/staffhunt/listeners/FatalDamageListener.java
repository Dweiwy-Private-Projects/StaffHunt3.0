package me.siwannie.staffhunt.listeners;

import me.siwannie.staffhunt.StaffHunt;
import me.siwannie.staffhunt.game.GameManager;
import me.siwannie.staffhunt.player.PlayerProfile;
import me.siwannie.staffhunt.player.PlayerProfileManager;
import me.siwannie.staffhunt.ui.UIManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class FatalDamageListener implements Listener {

    private final StaffHunt plugin;
    private final GameManager gameManager;
    private final PlayerProfileManager profileManager;
    private final UIManager uiManager;
    private final Random random = new Random();

    public FatalDamageListener(StaffHunt plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.profileManager = plugin.getPlayerProfileManager();
        this.uiManager = plugin.getUiManager();
    }

    // --- NEW: Prevent the fake Phantom Phase bat from taking damage ---
    @EventHandler
    public void onBatDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Bat && event.getEntity().getVehicle() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        // --- NEW: Prevent suffocation damage while burrowed ---
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION && victim.hasMetadata("staffhunt_burrowed")) {
            event.setCancelled(true);
            return;
        }

        // --- NEW: Cancel damage if invincible ---
        if (victim.hasMetadata("respawn_invincibility")) {
            event.setCancelled(true);
            return;
        }
        if (!gameManager.isGameActive() || (!gameManager.isHunter(victim) && !gameManager.isStaff(victim))) return;

        // Record damage contributions if the cause was another entity
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent pvpEvent = (EntityDamageByEntityEvent) event;
            Player damager = getDamagerFromEvent(pvpEvent);

            // --- NEW: Friendly Fire Prevention ---
            if (damager != null) {
                boolean bothHunters = gameManager.isHunter(victim) && gameManager.isHunter(damager);
                boolean bothStaff = gameManager.isStaff(victim) && gameManager.isStaff(damager);

                if (bothHunters || bothStaff) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (damager != null && gameManager.isHunter(damager) && gameManager.isStaff(victim)) {
                gameManager.addDamageContribution(victim, damager, event.getFinalDamage());
            }
        }

        // Intercept fatal damage from any source (PvP, fall, etc.)
        if (victim.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);

            Player killer = victim.getKiller(); // Bukkit's way of finding the last player who caused damage

            // Broadcast a randomized death message
            broadcastKillMessage(victim, killer);

            // Handle points and kill logic if there was a killer
            if (killer != null) {
                if (gameManager.isHunter(killer) && gameManager.isStaff(victim)) {
                    processStaffKill(victim, killer);
                } else if (gameManager.isStaff(killer) && gameManager.isHunter(victim)) {
                    PlayerProfile staffProfile = profileManager.getProfile(killer.getUniqueId());
                    if (staffProfile != null) {
                        staffProfile.incrementKills();
                        int coinsEarned = plugin.getConfig().getInt("staff-kill-reward-coins", 15);
                        if (coinsEarned > 0) {
                            staffProfile.addHuntCoins(coinsEarned);
                            uiManager.sendMessage(killer, ChatColor.YELLOW + "+ " + coinsEarned + " Hunt Coins" + ChatColor.GRAY + " for killing a Hunter.");
                        }
                    }
                }
            }
            // --- NEW: Handle Death Penalty & Shield ---
            PlayerProfile victimProfile = profileManager.getProfile(victim.getUniqueId());
            if (victimProfile != null) {
                victimProfile.saveCurrentLoadout(victim);
                if (victimProfile.hasUpgradeShield()) {
                    victimProfile.setUpgradeShield(false); // Consume the shield
                    uiManager.sendMessage(victim, ChatColor.AQUA + "Your Upgrade Shield protected your enchants but was consumed!");
                } else {
                    victimProfile.applyDeathDowngrade();
                    uiManager.sendMessage(victim, ChatColor.RED + "You died and lost 1 level on all your upgrades!");
                }
            }

            plugin.getRespawnManager().handlePlayerDeath(victim);
        }
    }

    private void processStaffKill(Player victim, Player killer) {
        Map<UUID, Double> contributions = gameManager.processAndClearContributions(victim);
        if (contributions == null || contributions.isEmpty()) return;

        String effectiveRank = gameManager.getEffectiveRank(victim);
        if (effectiveRank == null) effectiveRank = "trial"; // Failsafe

        // --- REBALANCED POINT CALCULATION LOGIC ---
        // Fetch the dynamic point pool size based on the victim's rank
        int totalPointPool = plugin.getConfig().getInt("max-damage-distribution-points." + effectiveRank, 35);
        double totalDamageDealt = contributions.values().stream().mapToDouble(Double::doubleValue).sum();

        if (totalDamageDealt <= 0) return; // Avoid division by zero

        // Award points to all contributors based on their percentage of damage
        for (Map.Entry<UUID, Double> entry : contributions.entrySet()) {
            UUID contributorUUID = entry.getKey();
            double damageDealt = entry.getValue();

            PlayerProfile contributorProfile = profileManager.getProfile(contributorUUID);
            if (contributorProfile == null) continue;

            // Calculate points based on damage percentage
            int pointsEarned = (int) Math.round(totalPointPool * (damageDealt / totalDamageDealt));
            contributorProfile.addPoints(pointsEarned);

            // Send messages
            Player contributor = plugin.getServer().getPlayer(contributorUUID);
            if (contributor != null) {
                // Killer gets a separate message later, so we skip them here.
                if (!contributor.equals(killer)) {
                    double damagePercent = (damageDealt / victim.getMaxHealth()) * 100.0;
                    uiManager.sendMessage(contributor, ChatColor.GOLD + "+ " + pointsEarned + " points" + ChatColor.GRAY + " for assisting (" + String.format("%.1f", damagePercent) + "%) in the kill of " + victim.getName() + ".");
                }
            }
        }

        // Award the killer the final blow and rank bonuses
        PlayerProfile killerProfile = profileManager.getProfile(killer.getUniqueId());
        if (killerProfile != null) {
            int finalBlowBonus = plugin.getConfig().getInt("kill-bonus-points", 0);
            int rankBonus = plugin.getConfig().getInt("rank-kill-bonus." + effectiveRank, 0);
            int totalBonus = finalBlowBonus + rankBonus;

            killerProfile.addPoints(totalBonus);

            int killerDamageShare = (int) Math.round(totalPointPool * (contributions.getOrDefault(killer.getUniqueId(), 0.0) / totalDamageDealt));
            int totalGained = killerDamageShare + totalBonus;

            uiManager.sendMessage(killer, ChatColor.GOLD + "+ " + totalGained + " points" + ChatColor.GRAY + " for eliminating " + victim.getName() + "!");
            // --- NEW: Give Hunt Coins for Staff Kills ---
            int coinsToGive = 0;
            switch(effectiveRank.toLowerCase()) {
                case "trial": coinsToGive = 50; break;
                case "helper": coinsToGive = 100; break;
                case "moderator": coinsToGive = 150; break;
                case "srmod": coinsToGive = 200; break;
                case "admin": coinsToGive = 300; break;
                case "manager": coinsToGive = 500; break;
            }
            if (coinsToGive > 0) {
                killerProfile.addHuntCoins(coinsToGive);
                uiManager.sendMessage(killer, ChatColor.YELLOW + "+ " + coinsToGive + " Hunt Coins" + ChatColor.GRAY + " for the kill.");
            }
        }
    }

    /**
     * Selects a random kill message from the config, formats it, and broadcasts it.
     * @param victim The player who died.
     * @param killer The player who got the kill, or null if it was an environmental death.
     */
    private void broadcastKillMessage(Player victim, @Nullable Player killer) {
        String format;
        String message;

        if (killer != null) {
            // PvP Kill
            List<String> formats = plugin.getConfig().getStringList("kill-messages.pvp");
            if (formats.isEmpty()) {
                format = "{victim} was slain by {killer}."; // Default fallback message
            } else {
                format = formats.get(random.nextInt(formats.size()));
            }
            // Format the message with colored names
            message = ChatColor.YELLOW + format
                    .replace("{victim}", ChatColor.RED + victim.getName() + ChatColor.YELLOW)
                    .replace("{killer}", ChatColor.GREEN + killer.getName() + ChatColor.YELLOW);
        } else {
            // General Death
            List<String> formats = plugin.getConfig().getStringList("kill-messages.general");
            if (formats.isEmpty()) {
                format = "{victim} died."; // Default fallback message
            } else {
                format = formats.get(random.nextInt(formats.size()));
            }
            // Format the message with the victim's name
            message = ChatColor.YELLOW + format
                    .replace("{victim}", ChatColor.RED + victim.getName() + ChatColor.YELLOW);
        }

        plugin.getServer().broadcastMessage(message);
    }

    private Player getDamagerFromEvent(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        if (damagerEntity instanceof Player) {
            return (Player) damagerEntity;
        }
        if (damagerEntity instanceof Projectile && ((Projectile) damagerEntity).getShooter() instanceof Player) {
            return (Player) ((Projectile) damagerEntity).getShooter();
        }
        return null;
    }
}