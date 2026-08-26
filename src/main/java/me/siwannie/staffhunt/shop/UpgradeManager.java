package me.siwannie.staffhunt.shop;

import me.siwannie.staffhunt.StaffHunt;
import me.siwannie.staffhunt.player.PlayerProfile;
import me.siwannie.staffhunt.player.PlayerProfileManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class UpgradeManager {
    private final StaffHunt plugin;
    public UpgradeManager(StaffHunt plugin) {
        this.plugin = plugin;
    }

    // --- UPGRADE LOGIC ---

    public void upgradeSharpness(Player player) {
        ItemStack sword = player.getItemInHand();
        if (sword == null || sword.getType() != Material.DIAMOND_SWORD) {
            player.sendMessage(ChatColor.RED + "You must be holding your diamond sword to upgrade it.");
            return;
        }
        int cost = getSharpnessCost(player);
        if (cost == -1) {
            player.sendMessage(ChatColor.RED + "Your sword is already max level!");
            return;
        }
        if (tryPurchase(player, cost)) {
            int currentLevel = sword.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
            sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, currentLevel + 1);
            player.sendMessage(ChatColor.GREEN + "Upgraded Sharpness to level " + (currentLevel + 1) + "!");
        }
    }

    public void upgradeProtection(Player player) {
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        boolean hasArmor = false;

        for (ItemStack armor : armorContents) {
            if (armor != null && armor.getType().name().contains("DIAMOND_")) {
                hasArmor = true;
                break;
            }
        }

        if (!hasArmor) {
            player.sendMessage(ChatColor.RED + "You must be wearing diamond armor to upgrade it.");
            return;
        }

        int cost = getProtectionCost(player);
        if (cost == -1) {
            player.sendMessage(ChatColor.RED + "Your armor is already max level!");
            return;
        }

        if (tryPurchase(player, cost)) {
            int currentLevel = 0;
            // Find the highest current protection level among all pieces
            for (ItemStack armor : armorContents) {
                if (armor != null && armor.getType().name().contains("DIAMOND_")) {
                    currentLevel = Math.max(currentLevel, armor.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL));
                }
            }

            // Apply the new level to all valid armor pieces
            for (ItemStack armor : armorContents) {
                if (armor != null && armor.getType().name().contains("DIAMOND_")) {
                    armor.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, currentLevel + 1);
                }
            }
            player.sendMessage(ChatColor.GREEN + "Upgraded Protection on all armor pieces to level " + (currentLevel + 1) + "!");
        }
    }

    public void upgradePower(Player player) {
        ItemStack bow = Arrays.stream(player.getInventory().getContents()).filter(i -> i != null && i.getType() == Material.BOW).findFirst().orElse(null);
        if (bow == null) {
            player.sendMessage(ChatColor.RED + "You do not have a bow to upgrade.");
            return;
        }
        int cost = getPowerCost(player);
        if (cost == -1) {
            player.sendMessage(ChatColor.RED + "Your bow is already max level!");
            return;
        }
        if (tryPurchase(player, cost)) {
            int currentLevel = bow.getEnchantmentLevel(Enchantment.ARROW_DAMAGE);
            bow.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, currentLevel + 1);
            player.sendMessage(ChatColor.GREEN + "Upgraded Power to level " + (currentLevel + 1) + "!");
        }
    }

    public void purchaseUpgradeShield(Player player) {
        PlayerProfile profile = plugin.getPlayerProfileManager().getProfile(player.getUniqueId());
        if (profile.hasUpgradeShield()) {
            player.sendMessage(ChatColor.RED + "You already have an active Upgrade Shield!");
            return;
        }
        if (tryPurchase(player, 500)) { // Fixed at 500 coins per your request
            profile.setUpgradeShield(true);
            player.sendMessage(ChatColor.AQUA + "You purchased an Upgrade Shield! Your enchants are safe for one death.");
        }
    }

    public void purchaseGoldenApple(Player player) {
        int cost = getGoldenAppleCost();
        if (tryPurchase(player, cost)) {
            player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE));
            player.sendMessage(ChatColor.GREEN + "You purchased a Golden Apple!");
        }
    }

    public void purchaseTracker(Player player) {
        int cost = getTrackerCost();
        if (tryPurchase(player, cost)) {
            player.getInventory().addItem(new ItemStack(Material.COMPASS));
            player.sendMessage(ChatColor.GREEN + "You purchased a Tracker Compass!");
        }
    }

    // --- DYNAMIC COST CALCULATION ---
    public int getSharpnessCost(Player player) {
        ItemStack sword = player.getItemInHand();
        if (sword == null || sword.getType() != Material.DIAMOND_SWORD) return 0;
        int level = sword.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
        if (level >= 5) return -1;
        return plugin.getConfig().getInt("shop-prices.sharpness-base", 150) + (level * plugin.getConfig().getInt("shop-prices.sharpness-multiplier", 100));
    }

    public int getProtectionCost(Player player) {
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        int level = 0;
        boolean hasArmor = false;

        for (ItemStack armor : armorContents) {
            if (armor != null && armor.getType().name().contains("DIAMOND_")) {
                level = Math.max(level, armor.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL));
                hasArmor = true;
            }
        }

        if (!hasArmor) return 0;
        if (level >= 4) return -1; // Max level is Protection 4
        return plugin.getConfig().getInt("shop-prices.protection-base", 200) + (level * plugin.getConfig().getInt("shop-prices.protection-multiplier", 150));
    }

    public int getPowerCost(Player player) {
        ItemStack bow = Arrays.stream(player.getInventory().getContents()).filter(i -> i != null && i.getType() == Material.BOW).findFirst().orElse(null);
        if (bow == null) return 0;
        int level = bow.getEnchantmentLevel(Enchantment.ARROW_DAMAGE);
        if (level >= 5) return -1;
        return plugin.getConfig().getInt("shop-prices.power-base", 100) + (level * plugin.getConfig().getInt("shop-prices.power-multiplier", 75));
    }

    public int getGoldenAppleCost() { return plugin.getConfig().getInt("shop-prices.golden-apple", 75); }
    public int getTrackerCost() { return plugin.getConfig().getInt("shop-prices.tracker", 50); }

    private boolean tryPurchase(Player player, int cost) {
        PlayerProfile profile = plugin.getPlayerProfileManager().getProfile(player.getUniqueId());
        if (profile.getHuntCoins() >= cost) {
            profile.removeHuntCoins(cost);
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "You don't have enough Hunt Coins! You need " + cost + ".");
            player.closeInventory();
            return false;
        }
    }
}