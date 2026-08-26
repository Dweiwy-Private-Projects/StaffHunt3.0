package me.siwannie.staffhunt.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.siwannie.staffhunt.StaffHunt;
import me.siwannie.staffhunt.game.GameManager;
import me.siwannie.staffhunt.player.PlayerProfile;
import me.siwannie.staffhunt.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PAPI_Expansion extends PlaceholderExpansion {

    private final StaffHunt plugin;
    private final PlayerProfileManager profileManager;
    private final GameManager gameManager;

    public PAPI_Expansion(StaffHunt plugin) {
        this.plugin = plugin;
        this.profileManager = plugin.getPlayerProfileManager();
        this.gameManager = plugin.getGameManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "staffhunt";
    }

    @Override
    public @NotNull String getAuthor() {
        return "siwannie";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // The expansion will automatically re-register on /papi reload
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) {
            // Create a profile on-the-fly if one doesn't exist for a joining player
            profileManager.createProfile(player.getUniqueId());
            profile = profileManager.getProfile(player.getUniqueId());
            if (profile == null) return "Error"; // Should not happen
        }


        if (params.equalsIgnoreCase("time_left")) {
            if (!gameManager.isGameActive()) {
                return "00:00";
            }
            int seconds = gameManager.getTimeRemaining();
            int minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }

        // %staffhunt_balance%
        if (params.equalsIgnoreCase("balance")) {
            return String.valueOf(profile.getHuntCoins());
        }

        // %staffhunt_score%
        if (params.equalsIgnoreCase("score")) {
            if (gameManager.isGameActive() && gameManager.isStaff(player.getUniqueId())) {
                return profile.getKills() + " Kills";
            }
            return profile.getPoints() + " Points";
        }

        // %staffhunt_rank%
        if (params.equalsIgnoreCase("rank")) {
            if (!gameManager.isGameActive() || !gameManager.isHunter(player.getUniqueId())) {
                return "N/A"; // Ranks are for hunters in-game
            }
            Set<UUID> hunterParticipants = gameManager.getAllHunterUuids();
            int rank = profileManager.getPlayerRank(player.getUniqueId(), hunterParticipants);
            return rank > 0 ? String.valueOf(rank) : "N/A";
        }

        if (params.equalsIgnoreCase("hunters_alive")) {
            return String.valueOf(gameManager.getOnlineHunters().size());
        }

        if (params.equalsIgnoreCase("staff_alive")) {
            return String.valueOf(gameManager.getOnlineStaff().size());
        }

        if (params.equalsIgnoreCase("status")) {
            return gameManager.isGameActive() ? "Active" : "Inactive";
        }

        // Top player placeholders, e.g., %staffhunt_top_name_1%
        if (params.startsWith("top_")) {
            String[] parts = params.split("_");
            if (parts.length == 3) {
                try {
                    int rank = Integer.parseInt(parts[2]);
                    if (rank < 1 || rank > 10) return "Invalid Rank";

                    // Use the correct method, providing the list of hunter participants
                    List<PlayerProfile> top = profileManager.getTopPlayersFromList(gameManager.getAllHunterUuids(), rank);
                    if (top.size() < rank) return "N/A";

                    PlayerProfile topProfile = top.get(rank - 1);
                    OfflinePlayer topPlayer = Bukkit.getOfflinePlayer(topProfile.getUuid());

                    if (parts[1].equalsIgnoreCase("name")) {
                        return topPlayer.getName() != null ? topPlayer.getName() : "Unknown";
                    }
                    if (parts[1].equalsIgnoreCase("points")) {
                        return String.valueOf(topProfile.getPoints());
                    }

                } catch (NumberFormatException e) {
                    return "Invalid Number";
                }
            }
        }

        return null; // Placeholder not found
    }
}