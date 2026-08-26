package me.siwannie.staffhunt.ui;

import me.siwannie.staffhunt.StaffHunt;
import me.siwannie.staffhunt.player.PlayerProfile;
import me.siwannie.staffhunt.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class UIManager {

    private final StaffHunt plugin;

    public UIManager(StaffHunt plugin) {
        this.plugin = plugin;
    }

    // ---- Generic Messages ----
    public void sendMessage(CommandSender sender, String message) {
        PluginUtils.sendMessage(sender, plugin.getConfig(), message);
    }

    public void broadcastMessage(String message) {
        PluginUtils.broadcastMessage(plugin.getConfig(), message);
    }

    public void sendActionBar(Player player, String message) {
        PluginUtils.sendActionBar(player, message);
    }

    // ---- Game Event Announcements ----
    public void announceGameStart() {
        broadcastMessage(ChatColor.GOLD + "Staff Hunt has begun! Good luck.");
    }

    public void announceGameEnd(List<PlayerProfile> topHunters, List<PlayerProfile> staffRanks) {
        String border = ChatColor.GOLD + "========================================";
        Bukkit.broadcastMessage(border);
        Bukkit.broadcastMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "           Staff Hunt Results");
        Bukkit.broadcastMessage(" ");

        // Announce Top Hunters
        Bukkit.broadcastMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "Top Hunters (by Points)");
        if (topHunters.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "No points were scored.");
        } else {
            for (int i = 0; i < topHunters.size(); i++) {
                PlayerProfile profile = topHunters.get(i);
                OfflinePlayer player = plugin.getServer().getOfflinePlayer(profile.getUuid());
                String name = player.getName() != null ? player.getName() : "Unknown";
                String message = String.format("%s #%d %s - %s%d Points", ChatColor.YELLOW, (i + 1), name, ChatColor.WHITE, profile.getPoints());
                Bukkit.broadcastMessage(message);
            }
        }
        Bukkit.broadcastMessage(" ");

        // Announce Staff Performance
        Bukkit.broadcastMessage(ChatColor.RED.toString() + ChatColor.BOLD + "Staff Performance (by Kills)");
        if (staffRanks.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "No staff participated.");
        } else {
            for (PlayerProfile profile : staffRanks) {
                OfflinePlayer player = plugin.getServer().getOfflinePlayer(profile.getUuid());
                String name = player.getName() != null ? player.getName() : "Unknown";
                String message = String.format("%s - %s: %s%d Kills", ChatColor.YELLOW, name, ChatColor.WHITE, profile.getKills());
                Bukkit.broadcastMessage(message);
            }
        }
        Bukkit.broadcastMessage(border);
    }
}