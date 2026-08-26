package me.siwannie.staffhunt.commands;

import me.siwannie.staffhunt.StaffHunt;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final StaffHunt plugin;

    public ShopCommand(StaffHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getGameManager().isGameActive()) {
            player.sendMessage(ChatColor.RED + "The shop is only available during a game.");
            return true;
        }

        plugin.getShopGUI().open(player);
        return true;
    }
}