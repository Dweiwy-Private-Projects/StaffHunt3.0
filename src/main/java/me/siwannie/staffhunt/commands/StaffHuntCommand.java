package me.siwannie.staffhunt.commands;

import me.siwannie.staffhunt.StaffHunt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StaffHuntCommand implements CommandExecutor {

    private final StaffHunt plugin;

    public StaffHuntCommand(StaffHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("staffhunt.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                int duration = 3600; // Default to 1 hour (3600 seconds)
                if (args.length > 1) {
                    try {
                        duration = Integer.parseInt(args[1]);
                        if (duration <= 0) {
                            sender.sendMessage(ChatColor.RED + "Duration must be a positive number.");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid duration specified. Please use seconds (e.g., 600 for 10 minutes).");
                        return true;
                    }
                }
                plugin.getGameManager().startGame(duration);
                break;
            case "stop":
                plugin.getGameManager().endGame(false); // End game, indicating staff did not win by survival
                sender.sendMessage(ChatColor.YELLOW + "You have manually stopped the game.");
                break;
            case "setspawn":
                handleSetSpawn(sender, args);
                break;
            case "reload":
                plugin.reloadConfig();
                plugin.getGameManager().loadSpawns();
                plugin.getKitManager().reloadKits();
                sender.sendMessage(ChatColor.GREEN + "Configuration files (config.yml, kits.yml) and spawns have been reloaded.");
                break;
            case "give":
                handleGiveItem(sender, args);
                break;
            case "currency":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /sh currency <player> <amount>");
                    return true;
                }
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer != null) {
                    int amountToAdd = Integer.parseInt(args[2]);
                    plugin.getPlayerProfileManager().getProfile(targetPlayer.getUniqueId()).addHuntCoins(amountToAdd);
                    sender.sendMessage(ChatColor.GREEN + "Gave " + amountToAdd + " coins to " + targetPlayer.getName());
                }
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void handleGiveItem(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /sh give <player> <item> [amount]");
            sender.sendMessage(ChatColor.GRAY + "Items: medkit, propulsor, invis_cloak, summoner, shadow_blade, beach_ball, nitro_speed, flash_bomb, gravity_launch, lifesaver, ender_totem, cobweb_ball, burrow, leviathan_axe, arrow_rain, phantom_phase, chaos_blade");            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or is offline.");
            return;
        }

        String itemName = args[2].toLowerCase();
        int amount = args.length > 3 ? Integer.parseInt(args[3]) : 1;

        if (itemName.equals("currency")) {
            plugin.getPlayerProfileManager().getProfile(target.getUniqueId()).addHuntCoins(amount);
            sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " Hunt Coins to " + target.getName() + ".");
            target.sendMessage(ChatColor.YELLOW + "You received " + amount + " Hunt Coins from an admin.");
            return;
        }

        ItemStack item = getCustomItem(itemName, amount);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Unknown item: " + itemName);
            return;
        }

        target.getInventory().addItem(item);
        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + itemName + " to " + target.getName() + ".");
    }

    private ItemStack getCustomItem(String name, int amount) {
        ItemStack item = null;
        ItemMeta meta;

        switch (name) {
            case "medkit":
                item = new ItemStack(Material.SULPHUR, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "Medkit");
                item.setItemMeta(meta);
                break;
            case "propulsor":
                item = new ItemStack(Material.FIREWORK, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "Propulsor");
                item.setItemMeta(meta);
                break;
            case "invis_cloak":
                item = new ItemStack(Material.NETHER_STAR, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.DARK_GRAY + "Invisibility Cloak");
                item.setItemMeta(meta);
                break;
            case "summoner":
                item = new ItemStack(Material.ENDER_PEARL, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Summoner");
                item.setItemMeta(meta);
                break;
            case "shadow_blade":
                item = new ItemStack(Material.IRON_SWORD, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.DARK_GRAY + "Shadow Blade");
                item.setItemMeta(meta);
                break;
            case "beach_ball":
                item = new ItemStack(Material.SKULL_ITEM, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + "Beach Ball");
                item.setItemMeta(meta);
                break;
            case "nitro_speed":
                item = new ItemStack(Material.GLASS_BOTTLE, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "Nitro Speed");
                item.setItemMeta(meta);
                break;
            case "flash_bomb":
                item = new ItemStack(Material.FLINT_AND_STEEL, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "Flash Bomb");
                item.setItemMeta(meta);
                break;
            case "gravity_launch":
                item = new ItemStack(Material.FISHING_ROD, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + "Gravity Launch");
                item.setItemMeta(meta);
                break;
            case "lifesaver":
                item = new ItemStack(Material.RED_ROSE, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "Lifesaver");
                item.setItemMeta(meta);
                break;
            case "ender_totem":
                item = new ItemStack(Material.ENDER_PEARL, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Ender Totem");
                item.setItemMeta(meta);
                break;
            case "cobweb_ball":
                item = new ItemStack(Material.SNOW_BALL, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.WHITE + "Cobweb Ball");
                item.setItemMeta(meta);
                break;
            case "burrow":
                item = new ItemStack(Material.WEB, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GRAY + "Burrow");
                item.setItemMeta(meta);
                break;
            case "leviathan_axe":
                item = new ItemStack(Material.DIAMOND_AXE, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + "Leviathan Axe");
                item.setItemMeta(meta);
                break;
            case "arrow_rain":
                item = new ItemStack(Material.ARROW, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "Arrow Rain");
                item.setItemMeta(meta);
                break;
            case "phantom_phase":
                item = new ItemStack(Material.MONSTER_EGG, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.DARK_PURPLE + "Phantom Phase");
                item.setItemMeta(meta);
                break;
            case "chaos_blade":
                item = new ItemStack(Material.GOLD_SWORD, amount);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.DARK_PURPLE + "Chaos Blade");
                item.setItemMeta(meta);
                break;
        }
        return item;
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sh setspawn <hunter|staff>");
            return;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();
        String type = args[1].toLowerCase();

        if (type.equals("hunter")) {
            plugin.getGameManager().setPlayerSpawn(location);
            sender.sendMessage(ChatColor.GREEN + "Hunter spawn point has been set to your location.");
        } else if (type.equals("staff")) {
            plugin.getGameManager().setStaffSpawn(location);
            sender.sendMessage(ChatColor.GREEN + "Staff spawn point has been set to your location.");
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /sh setspawn <hunter|staff>");
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- StaffHunt Admin Commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/sh start [seconds]" + ChatColor.GRAY + " - Starts a game (defaults to 1hr).");
        sender.sendMessage(ChatColor.YELLOW + "/sh stop" + ChatColor.GRAY + " - Forcibly stops the current game.");
        sender.sendMessage(ChatColor.YELLOW + "/sh setspawn <hunter|staff>" + ChatColor.GRAY + " - Sets the spawn points.");
        sender.sendMessage(ChatColor.YELLOW + "/sh give <player> <item> [amount]" + ChatColor.GRAY + " - Gives a custom event item.");
        sender.sendMessage(ChatColor.YELLOW + "/sh reload" + ChatColor.GRAY + " - Reloads the plugin's configuration files.");
    }
}