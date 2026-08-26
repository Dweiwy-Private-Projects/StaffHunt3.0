package me.siwannie.staffhunt.game;

import me.siwannie.staffhunt.StaffHunt;
import me.siwannie.staffhunt.player.PlayerProfile;
import me.siwannie.staffhunt.player.PlayerProfileManager;
import me.siwannie.staffhunt.ui.UIManager;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GameManager {

    private final StaffHunt plugin;
    private final PlayerProfileManager profileManager;
    private final UIManager uiManager;
    private boolean gameActive = false;
    private final Set<UUID> staffMembers = new HashSet<>();
    private final Set<UUID> hunters = new HashSet<>();

    private Location staffSpawn;
    private Location playerSpawn; // Renamed from legacy 'playerSpawn' to 'hunterSpawn' for clarity
    private BukkitTask gameTimerTask;
    private int timeRemaining;
    private BukkitTask trackerTask;

    public static class DamageRecord {
        public double damage;
        public long lastHitTime;
        public DamageRecord(double damage, long lastHitTime) {
            this.damage = damage;
            this.lastHitTime = lastHitTime;
        }
    }
    private final Map<UUID, Map<UUID, DamageRecord>> staffDamageContributions = new ConcurrentHashMap<>();
    public GameManager(StaffHunt plugin) {
        this.plugin = plugin;
        this.profileManager = plugin.getPlayerProfileManager();
        this.uiManager = plugin.getUiManager();
        loadSpawns();
    }

    public void startGame(int durationInSeconds) {
        if (gameActive) {
            uiManager.broadcastMessage(ChatColor.RED + "A game is already in progress!");
            return;
        }
        if (staffSpawn == null || playerSpawn == null) {
            uiManager.broadcastMessage(ChatColor.RED + "Both hunter and staff spawn points must be set before starting!");
            return;
        }

        gameActive = true;
        staffMembers.clear();
        hunters.clear();
        profileManager.resetAllProfilesForNewGame(); // Use the new reset method
        staffDamageContributions.clear();

        // Create profiles for all online players first
        for (Player player : Bukkit.getOnlinePlayers()) {
            profileManager.createProfile(player.getUniqueId());
        }

        // Now assign roles
        for (Player player : Bukkit.getOnlinePlayers()) {
            String effectiveRank = getEffectiveRank(player);

            if (effectiveRank != null) {
                staffMembers.add(player.getUniqueId());
                player.teleport(staffSpawn);
                player.setGameMode(GameMode.ADVENTURE);
                uiManager.sendMessage(player, ChatColor.RED + "You are a Staff member! (Rank: " + effectiveRank + ")");
            } else {
                hunters.add(player.getUniqueId());
                player.teleport(playerSpawn);
                player.setGameMode(GameMode.ADVENTURE);
                uiManager.sendMessage(player, ChatColor.GREEN + "You are a Hunter! Hunt the staff!");
            }

            plugin.getKitManager().applyKit(player, effectiveRank);
        }

        if (staffMembers.isEmpty()) {
            uiManager.broadcastMessage(ChatColor.YELLOW + "No staff members are online. The game cannot start.");
            endGame(false); // Use a new method to properly clear state
            return;
        }

        this.timeRemaining = durationInSeconds;
        gameTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (timeRemaining > 0) {
                if (timeRemaining % 300 == 0 || timeRemaining <= 10) { // Example announcements
                    uiManager.broadcastMessage(ChatColor.YELLOW + "Time remaining: " + timeRemaining / 60 + " minutes.");
                }
                timeRemaining--;
            } else {
                uiManager.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Time is up! Staff have survived!");
                endGame(true); // Staff wins
            }
        }, 20L, 20L);

        plugin.getDataManager().startPeriodicSaving();
        trackerTask = new TrackerManager(plugin).runTaskTimer(plugin, 20L, 20L);
        uiManager.announceGameStart();
    }

    /**
     * Ends the game and determines the winner.
     * @param staffSurvived True if the game ended because time ran out, false otherwise.
     */
    public void endGame(boolean staffSurvived) {
        if (!gameActive) return;

        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }

        if (trackerTask != null) {
            trackerTask.cancel();
            trackerTask = null;
        }

        // Announce winner before cleaning up
        if (staffSurvived) {
            uiManager.broadcastMessage(ChatColor.GREEN + "The Staff team has won by surviving!");
        } else {
            uiManager.broadcastMessage(ChatColor.RED + "The Hunter team has won by eliminating all staff!");
        }

        plugin.getDataManager().stopPeriodicSaving();
        plugin.getDataManager().saveData(); // Final save

        // Use the master lists of participants, not just online players
        Set<UUID> allParticipants = Stream.concat(staffMembers.stream(), hunters.stream()).collect(Collectors.toSet());

        for (UUID participantUUID : allParticipants) {
            Player player = Bukkit.getPlayer(participantUUID);
            if (player != null && player.isOnline()) {
                // Reset player state
                player.setMaxHealth(20.0);
                player.setHealth(20.0);
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                player.setGameMode(Bukkit.getDefaultGameMode());
                // Teleport to a lobby or default spawn if available
            }
        }

        // Generate leaderboards from the complete participant lists
        List<PlayerProfile> topHunters = profileManager.getTopPlayersFromList(hunters, 10);
        List<PlayerProfile> staffRanks = staffMembers.stream()
                .map(uuid -> profileManager.getProfile(uuid))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(PlayerProfile::getKills).reversed())
                .collect(Collectors.toList());

        uiManager.announceGameEnd(topHunters, staffRanks);

        // Finally, clear all game data
        gameActive = false;
        staffMembers.clear();
        hunters.clear();
        staffDamageContributions.clear();
    }


    // This method is called by the listener when a player quits
    public void handlePlayerDisconnect(Player player) {
        // No longer need to remove them from the list for leaderboard purposes.
        // Just broadcast a message.
        if (isStaff(player) || isHunter(player)) {
            uiManager.broadcastMessage(ChatColor.GRAY + player.getName() + " has disconnected.");
            // Check win conditions if a staff member disconnects
            if (isStaff(player)) {
                checkWinConditions();
            }
        }
    }

    public void addLateJoiner(Player player) {
        if (isStaff(player) || isHunter(player)) return; // Already in game

        String effectiveRank = getEffectiveRank(player);
        if (effectiveRank != null) {
            staffMembers.add(player.getUniqueId());
            player.setGameMode(GameMode.ADVENTURE);
            uiManager.sendMessage(player, ChatColor.RED + "You late-joined as a Staff member! (Rank: " + effectiveRank + ")");
        } else {
            hunters.add(player.getUniqueId());
            player.setGameMode(GameMode.ADVENTURE);
            uiManager.sendMessage(player, ChatColor.GREEN + "You late-joined as a Hunter! Hunt the staff!");
        }
    }

    public void checkWinConditions() {
        if (!gameActive) return;
        // Check if all staff members are offline or eliminated.
        if (getOnlineStaff().isEmpty()) {
            endGame(false); // Hunters win
        }
    }

    public String getEffectiveRank(Player player) {
        User user = plugin.getLuckPerms().getPlayerAdapter(Player.class).getUser(player);
        Set<String> definedStaffKits = plugin.getKitManager().getDefinedStaffKits();

        return user.getInheritedGroups(user.getQueryOptions()).stream()
                .filter(group -> definedStaffKits.contains(group.getName()))
                .max(Comparator.comparingInt(g -> g.getWeight().orElse(0)))
                .map(Group::getName)
                .orElse(null);
    }

    public void addDamageContribution(Player staff, Player hunter, double damage) {
        long expirationTime = plugin.getConfig().getLong("damage-expiration-seconds", 60) * 1000L;

        staffDamageContributions.computeIfAbsent(staff.getUniqueId(), k -> new ConcurrentHashMap<>())
                .compute(hunter.getUniqueId(), (uuid, record) -> {
                    if (record == null) {
                        return new DamageRecord(damage, System.currentTimeMillis());
                    } else {
                        // If their last hit was over the configured time ago, reset their damage pool
                        if (System.currentTimeMillis() - record.lastHitTime > expirationTime) {
                            record.damage = damage;
                        } else {
                            record.damage += damage;
                        }
                        record.lastHitTime = System.currentTimeMillis();
                        return record;
                    }
                });
    }

    public Map<UUID, Double> processAndClearContributions(Player staff) {
        Map<UUID, DamageRecord> records = staffDamageContributions.remove(staff.getUniqueId());
        Map<UUID, Double> validContributions = new java.util.HashMap<>();

        if (records == null) return validContributions;

        long now = System.currentTimeMillis();
        long expirationTime = plugin.getConfig().getLong("damage-expiration-seconds", 60) * 1000L;

        // Only process hunters who have dealt damage within the configured time window
        for (Map.Entry<UUID, DamageRecord> entry : records.entrySet()) {
            if (now - entry.getValue().lastHitTime <= expirationTime) {
                validContributions.put(entry.getKey(), entry.getValue().damage);
            }
        }
        return validContributions;
    }

    public boolean isGameActive() {
        return gameActive;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public boolean isStaff(UUID uuid) {
        return staffMembers.contains(uuid);
    }

    public boolean isHunter(UUID uuid) {
        return hunters.contains(uuid);
    }

    public boolean isStaff(Player player) {
        return isStaff(player.getUniqueId());
    }

    public boolean isHunter(Player player) {
        return isHunter(player.getUniqueId());
    }

    public Set<UUID> getAllStaffUuids() {
        return Collections.unmodifiableSet(staffMembers);
    }

    public Set<UUID> getAllHunterUuids() {
        return Collections.unmodifiableSet(hunters);
    }

    public Set<Player> getOnlineStaff() {
        return staffMembers.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toSet());
    }

    public Set<Player> getOnlineHunters() {
        return hunters.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toSet());
    }

    // --- Spawn Getters ---
    public Location getStaffSpawn() {
        return staffSpawn;
    }

    public Location getPlayerSpawn() {
        return playerSpawn;
    }

    // --- Spawn Setters & Savers ---
    public void setStaffSpawn(Location location) {
        this.staffSpawn = location;
        plugin.getConfig().set("spawns.staff", locationToString(location));
        plugin.saveConfig();
    }

    public void setPlayerSpawn(Location location) {
        this.playerSpawn = location;
        plugin.getConfig().set("spawns.player", locationToString(location));
        plugin.saveConfig();
    }

    public void teleportToStaffSpawn(Player player) {
        if (staffSpawn != null) {
            player.teleport(staffSpawn);
        }
    }

    public void teleportToHunterSpawn(Player player) {
        if (playerSpawn != null) {
            player.teleport(playerSpawn);
        }
    }

    public void loadSpawns() {
        String staffLocStr = plugin.getConfig().getString("spawns.staff");
        if (staffLocStr != null && !staffLocStr.isEmpty()) {
            this.staffSpawn = stringToLocation(staffLocStr);
        }
        String playerLocStr = plugin.getConfig().getString("spawns.player");
        if (playerLocStr != null && !playerLocStr.isEmpty()) {
            this.playerSpawn = stringToLocation(playerLocStr);
        }
    }

    private String locationToString(Location loc) {
        if (loc == null) return "";
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private Location stringToLocation(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        String[] parts = s.split(",");
        try {
            String worldName = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load a spawn location from config: " + s);
            return null;
        }
    }
}