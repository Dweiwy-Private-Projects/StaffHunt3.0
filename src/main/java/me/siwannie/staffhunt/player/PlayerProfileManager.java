package me.siwannie.staffhunt.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerProfileManager {

    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();

    public void createProfile(UUID uuid) {
        // Use computeIfAbsent to avoid overwriting an existing profile if the player rejoins
        profiles.computeIfAbsent(uuid, PlayerProfile::new);
    }

    /**
     * This method is no longer called on player quit during a game.
     * It should be called during a full server shutdown or when a game completely resets.
     * @param uuid The UUID of the player profile to remove.
     */
    public void removeProfile(UUID uuid) {
        profiles.remove(uuid);
    }

    public PlayerProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    /**
     * Resets the game-specific stats for all currently loaded profiles.
     * This is called at the start of a new game.
     */
    public void resetAllProfilesForNewGame() {
        profiles.values().forEach(PlayerProfile::resetStats);
    }

    /**
     * Gets a list of all player profiles, regardless of their online status.
     * @return A collection of all player profiles.
     */
    public Collection<PlayerProfile> getAllProfiles() {
        return profiles.values();
    }

    /**
     * Gets the top players from a given list of participants.
     * @param participantUuids The UUIDs of players who were in the game.
     * @param limit The maximum number of players to return.
     * @return A sorted list of top player profiles.
     */
    public List<PlayerProfile> getTopPlayersFromList(Collection<UUID> participantUuids, int limit) {
        return participantUuids.stream()
                .map(this::getProfile)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(PlayerProfile::getPoints).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Calculates a player's rank from a specific list of participants.
     * @param uuid The UUID of the player to rank.
     * @param participantUuids The collection of UUIDs to rank against.
     * @return The player's rank (1-based), or -1 if not found.
     */
    public int getPlayerRank(UUID uuid, Collection<UUID> participantUuids) {
        List<PlayerProfile> sortedProfiles = participantUuids.stream()
                .map(this::getProfile)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(PlayerProfile::getPoints).reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < sortedProfiles.size(); i++) {
            if (sortedProfiles.get(i).getUuid().equals(uuid)) {
                return i + 1; // Return 1-based rank
            }
        }
        return -1; // Player not found
    }
}