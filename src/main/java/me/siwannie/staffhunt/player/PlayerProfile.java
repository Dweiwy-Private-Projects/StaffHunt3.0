package me.siwannie.staffhunt.player;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;
    private int huntCoins;
    private int points;
    private int kills;
    private int savedSharpness = 0;
    private int savedProtection = 0;
    private int savedPower = 0;
    private boolean upgradeShield = false;

    public boolean hasUpgradeShield() { return upgradeShield; }
    public void setUpgradeShield(boolean shield) { this.upgradeShield = shield; }

    public void saveCurrentLoadout(org.bukkit.entity.Player player) {
        savedSharpness = 0;
        savedProtection = 0;
        savedPower = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                if (item.getType().name().contains("SWORD")) {
                    savedSharpness = Math.max(savedSharpness, item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DAMAGE_ALL));
                } else if (item.getType() == org.bukkit.Material.BOW) {
                    savedPower = Math.max(savedPower, item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.ARROW_DAMAGE));
                }
            }
        }
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType().name().contains("DIAMOND_")) {
                savedProtection = Math.max(savedProtection, item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL));
            }
        }
    }

    public void applyDeathDowngrade() {
        if (savedSharpness > 0) savedSharpness--;
        if (savedProtection > 0) savedProtection--;
        if (savedPower > 0) savedPower--;
    }

    public void restoreLoadout(org.bukkit.entity.Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                if (item.getType().name().contains("SWORD") && savedSharpness > 0) {
                    item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, savedSharpness);
                } else if (item.getType() == org.bukkit.Material.BOW && savedPower > 0) {
                    item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.ARROW_DAMAGE, savedPower);
                }
            }
        }
        org.bukkit.inventory.ItemStack[] armor = player.getInventory().getArmorContents();
        for (org.bukkit.inventory.ItemStack item : armor) {
            if (item != null && item.getType().name().contains("DIAMOND_") && savedProtection > 0) {
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, savedProtection);
            }
        }
        player.getInventory().setArmorContents(armor);
    }

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        // HuntCoins are persistent, so they are not initialized to 0 here unless it's a new profile.
        // For a real-world scenario, you would load this from a database.
        this.huntCoins = 0;
        this.points = 0;
        this.kills = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getHuntCoins() {
        return huntCoins;
    }

    public void setHuntCoins(int huntCoins) {
        this.huntCoins = huntCoins;
    }

    public void addHuntCoins(int amount) {
        this.huntCoins += amount;
    }

    public boolean removeHuntCoins(int amount) {
        if (this.huntCoins >= amount) {
            this.huntCoins -= amount;
            return true;
        }
        return false;
    }

    public int getPoints() {
        return points;
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    public int getKills() {
        return kills;
    }

    public void incrementKills() {
        this.kills++;
    }

    /**
     * Resets only the stats for a single game (points, kills).
     * HuntCoins are persistent and are not reset.
     */
    public void resetStats() {
        this.points = 0;
        this.kills = 0;
    }
}