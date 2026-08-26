package me.siwannie.staffhunt.shop;

import me.siwannie.staffhunt.StaffHunt;
import me.siwannie.staffhunt.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ShopGUI implements Listener {

    private final StaffHunt plugin;
    private final String inventoryName = ChatColor.DARK_AQUA + "Hunter's Upgrade Shop";
    private final UpgradeManager upgradeManager;

    public ShopGUI(StaffHunt plugin) {
        this.plugin = plugin;
        this.upgradeManager = new UpgradeManager(plugin);
    }

    public void open(Player player) {
        Inventory shop = Bukkit.createInventory(null, 27, inventoryName);

        // --- Sword Upgrades ---
        shop.setItem(10, createUpgradeItem(
                player,
                Material.DIAMOND_SWORD,
                Enchantment.DAMAGE_ALL,
                "Sharpness",
                upgradeManager.getSharpnessCost(player)
        ));

        // --- Armor Upgrades ---
        shop.setItem(12, createUpgradeItem(
                player,
                Material.DIAMOND_CHESTPLATE,
                Enchantment.PROTECTION_ENVIRONMENTAL,
                "Protection",
                upgradeManager.getProtectionCost(player)
        ));

        // --- Bow Upgrades ---
        shop.setItem(14, createUpgradeItem(
                player,
                Material.BOW,
                Enchantment.ARROW_DAMAGE,
                "Power",
                upgradeManager.getPowerCost(player)
        ));

        shop.setItem(13, createConsumableItem(Material.GHAST_TEAR, "Upgrade Shield", 500, "Protects your enchants from dropping 1 level on death."));
        shop.setItem(15, createConsumableItem(Material.COMPASS, "Tracker Compass", upgradeManager.getTrackerCost(), "Points to the nearest staff member."));

        // --- Special Items ---
        shop.setItem(16, createConsumableItem(
                Material.GOLDEN_APPLE,
                "Golden Apple",
                upgradeManager.getGoldenAppleCost(),
                "A tasty, life-saving snack."
        ));

        player.openInventory(shop);
    }

    private ItemStack createUpgradeItem(Player player, Material material, Enchantment ench, String name, int cost) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        int currentLevel = 0;
        if (ench.equals(Enchantment.DAMAGE_ALL)) currentLevel = player.getItemInHand().getEnchantmentLevel(ench);
        else if (ench.equals(Enchantment.PROTECTION_ENVIRONMENTAL)) currentLevel = player.getInventory().getChestplate().getEnchantmentLevel(ench);
        else if (ench.equals(Enchantment.ARROW_DAMAGE)) {
            ItemStack bow = Arrays.stream(player.getInventory().getContents()).filter(i -> i != null && i.getType() == Material.BOW).findFirst().orElse(null);
            if (bow != null) currentLevel = bow.getEnchantmentLevel(ench);
        }

        meta.setDisplayName(ChatColor.GREEN + "Upgrade " + name);
        if (cost == -1) { // -1 indicates max level
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Current Level: " + ChatColor.YELLOW + "Maxed Out!",
                    ChatColor.RED + "You cannot upgrade this further."
            ));
        } else {
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Current Level: " + ChatColor.YELLOW + currentLevel,
                    ChatColor.GRAY + "Next Level: " + ChatColor.YELLOW + (currentLevel + 1),
                    "",
                    ChatColor.GOLD + "Cost: " + cost + " Hunt Coins"
            ));
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConsumableItem(Material material, String name, int cost, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Purchase " + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + description,
                "",
                ChatColor.GOLD + "Cost: " + cost + " Hunt Coins"
        ));
        item.setItemMeta(meta);
        return item;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(inventoryName)) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Find which item was clicked and process the upgrade
        switch (clickedItem.getType()) {
            case DIAMOND_SWORD:
                upgradeManager.upgradeSharpness(player);
                break;
            case DIAMOND_CHESTPLATE:
                upgradeManager.upgradeProtection(player);
                break;
            case BOW:
                upgradeManager.upgradePower(player);
                break;
            case GHAST_TEAR:
                upgradeManager.purchaseUpgradeShield(player);
                break;
            case GOLDEN_APPLE:
                upgradeManager.purchaseGoldenApple(player);
                break;
            case COMPASS:
                upgradeManager.purchaseTracker(player);
                break;
            default:
                return;
        }

        // Refresh the GUI to show new costs and levels
        open(player);
    }
}