package me.siwannie.staffhunt.game;

import me.siwannie.staffhunt.StaffHunt;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KitManager {

    private final StaffHunt plugin;
    private FileConfiguration kitConfig;

    // Reflection objects for performance
    private static Method asNMSCopyMethod;
    private static Method getTagMethod;
    private static Method setTagMethod;
    private static Method asBukkitCopyMethod;
    private static Constructor<?> nbtTagStringConstructor;
    private static Method nbtTagListAddMethod;
    private static Method nbtTagCompoundSetMethod;
    private static Class<?> nbtTagCompoundClass;
    private static Class<?> nbtTagListClass;

    static {
        try {
            // Initialize all reflection objects once
            String version = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
            Class<?> nmsItemStackClass = Class.forName("net.minecraft.server." + version + ".ItemStack");
            nbtTagCompoundClass = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
            nbtTagListClass = Class.forName("net.minecraft.server." + version + ".NBTTagList");
            Class<?> nbtBaseClass = Class.forName("net.minecraft.server." + version + ".NBTBase");
            Class<?> nbtTagStringClass = Class.forName("net.minecraft.server." + version + ".NBTTagString");

            asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", nmsItemStackClass);
            getTagMethod = nmsItemStackClass.getMethod("getTag");
            setTagMethod = nmsItemStackClass.getMethod("setTag", nbtTagCompoundClass);
            nbtTagStringConstructor = nbtTagStringClass.getConstructor(String.class);
            nbtTagListAddMethod = nbtTagListClass.getMethod("add", nbtBaseClass);
            nbtTagCompoundSetMethod = nbtTagCompoundClass.getMethod("set", String.class, nbtBaseClass);
        } catch (Exception e) {
            e.printStackTrace();
            org.bukkit.Bukkit.getLogger().severe("StaffHunt failed to initialize NBT reflection. The 'can-destroy' feature will not work.");
        }
    }


    public KitManager(StaffHunt plugin) {
        this.plugin = plugin;
        loadKits();
    }

    public void reloadKits() {
        loadKits(); // Calling the existing load method re-reads the file
        plugin.getLogger().info("kits.yml has been reloaded.");
    }

    public void loadKits() {
        File kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!kitsFile.exists()) {
            plugin.getLogger().info("kits.yml not found, creating from defaults...");
            plugin.saveResource("kits.yml", false);
        }
        kitConfig = YamlConfiguration.loadConfiguration(kitsFile);
        if (kitConfig == null) {
            plugin.getLogger().severe("CRITICAL: kitConfig object failed to load. The kits.yml file may be corrupt.");
        } else {
            plugin.getLogger().info("kits.yml has been loaded successfully.");
        }
    }

    public void applyKit(Player player, String effectiveRank) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        String kitType;
        boolean isStaff = effectiveRank != null;

        if (isStaff) {
            kitType = "staff-kits." + effectiveRank;
        } else {
            kitType = "hunter-kit";
        }

        ConfigurationSection kitSection = kitConfig.getConfigurationSection(kitType);
        if (kitSection == null) {
            if (isStaff) {
                plugin.getLogger().warning("Could not find kit for effective rank: " + effectiveRank + ". Applying hunter kit as failsafe.");
                kitSection = kitConfig.getConfigurationSection("hunter-kit");
            }
            if (kitSection == null) {
                plugin.getLogger().severe("FATAL: Could not find any kit configuration, not even hunter-kit.");
                return;
            }
        }

        if (isStaff && kitSection.contains("health")) {
            int health = kitSection.getInt("health");
            player.setMaxHealth(health);
            player.setHealth(health);
        } else {
            player.setMaxHealth(20.0);
            player.setHealth(20.0);
        }

        ConfigurationSection armorSection = kitSection.getConfigurationSection("armor");
        if (armorSection != null) {
            ItemStack[] armor = new ItemStack[4];
            for (String slot : armorSection.getKeys(false)) {
                ItemStack item = parseItem(armorSection.getConfigurationSection(slot));
                int armorSlot = Integer.parseInt(slot);
                if (armorSlot == 103) armor[3] = item;
                else if (armorSlot == 102) armor[2] = item;
                else if (armorSlot == 101) armor[1] = item;
                else if (armorSlot == 100) armor[0] = item;
            }
            player.getInventory().setArmorContents(armor);
        }

        ConfigurationSection inventorySection = kitSection.getConfigurationSection("inventory");
        if (inventorySection != null) {
            for (String slot : inventorySection.getKeys(false)) {
                ItemStack item = parseItem(inventorySection.getConfigurationSection(slot));
                player.getInventory().setItem(Integer.parseInt(slot), item);
            }
        }
    }

    private ItemStack parseItem(ConfigurationSection section) {
        if (section == null) return null;

        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        int amount = section.getInt("amount", 1);
        short data = (short) section.getInt("data", 0); // --- NEW: Supports item data! ---
        ItemStack item = new ItemStack(material, amount, data);

        // ***** CORRECTED ORDER OF OPERATIONS *****

        // 1. APPLY NBT TAGS FIRST. This may return a new item instance.
        if (section.contains("can-destroy")) {
            List<String> canDestroyMaterials = section.getStringList("can-destroy");
            if (!canDestroyMaterials.isEmpty()) {
                item = applyCanDestroyTags(item, canDestroyMaterials);
            }
        }

        // 2. NOW, GET THE META FROM THE FINAL, CORRECT ITEM AND APPLY VISUALS.
        ItemMeta meta = item.getItemMeta();

        if (section.contains("display-name")) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("display-name")));
        }

        List<String> lore = new ArrayList<>();
        if (section.contains("lore")) {
            for (String line : section.getStringList("lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }

        if (section.contains("enchantments")) {
            for (String ench : section.getStringList("enchantments")) {
                String[] parts = ench.split(":");
                Enchantment enchantment = Enchantment.getByName(parts[0].toUpperCase());
                int level = Integer.parseInt(parts[1]);
                if (enchantment != null) {
                    meta.addEnchant(enchantment, level, true);
                }
            }
        }

        // Add the "Can break" visual lore (DISABLED FOR QoL)

        if (section.contains("can-destroy")) {
            List<String> canDestroyMaterials = section.getStringList("can-destroy");
            if (!canDestroyMaterials.isEmpty()) {
                if (!lore.isEmpty()) {
                    lore.add(""); // Add a space for readability
                }
                lore.add(ChatColor.GRAY + "Can break:");
                for (String mat : canDestroyMaterials) {
                    String friendlyName = toTitleCase(mat.replace('_', ' '));
                    lore.add(ChatColor.DARK_GRAY + friendlyName);
                }
            }
        }
        meta.setLore(lore);

        if (material.toString().contains("SWORD") || material.toString().contains("PICKAXE") || material.toString().contains("BOW") || material.toString().contains("HELMET") || material.toString().contains("CHESTPLATE") || material.toString().contains("LEGGINGS") || material.toString().contains("BOOTS")) {
            meta.spigot().setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }

        // 3. APPLY THE FINAL META TO THE FINAL ITEM
        item.setItemMeta(meta);
        return item;
    }

    private String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;
        for (char c : input.toLowerCase().toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }

    private ItemStack applyCanDestroyTags(ItemStack item, List<String> materials) {
        if (asNMSCopyMethod == null) {
            return item;
        }
        try {
            Object nmsItemStack = asNMSCopyMethod.invoke(null, item);
            Object nbtTagCompound = getTagMethod.invoke(nmsItemStack);
            if (nbtTagCompound == null) {
                nbtTagCompound = nbtTagCompoundClass.newInstance();
                setTagMethod.invoke(nmsItemStack, nbtTagCompound);
            }

            Object canDestroyList = nbtTagListClass.newInstance();
            for (String materialName : materials) {
                Object nbtTagString = nbtTagStringConstructor.newInstance(materialName.toLowerCase());
                nbtTagListAddMethod.invoke(canDestroyList, nbtTagString);
            }

            nbtTagCompoundSetMethod.invoke(nbtTagCompound, "CanDestroy", canDestroyList);
            return (ItemStack) asBukkitCopyMethod.invoke(null, nmsItemStack);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply 'CanDestroy' tags to an item: " + item.getType());
            e.printStackTrace();
            return item;
        }
    }


    public Set<String> getDefinedStaffKits() {
        if (kitConfig == null) {
            plugin.getLogger().severe("getDefinedStaffKits() was called, but kitConfig is null! The YML file likely failed to load.");
            return new HashSet<>();
        }
        ConfigurationSection staffKitsSection = kitConfig.getConfigurationSection("staff-kits");

        if (staffKitsSection == null) {
            return new HashSet<>();
        }

        return staffKitsSection.getKeys(false);
    }
}