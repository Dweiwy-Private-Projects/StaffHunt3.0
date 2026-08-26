package me.siwannie.staffhunt.listeners;

import me.siwannie.staffhunt.StaffHunt;
import me.siwannie.staffhunt.game.GameManager;
import me.siwannie.staffhunt.game.OreManager;
import me.siwannie.staffhunt.player.PlayerProfile;
import me.siwannie.staffhunt.player.PlayerProfileManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class MiningListener implements Listener {

    private final StaffHunt plugin;
    private final GameManager gameManager;
    private final OreManager oreManager;
    private final PlayerProfileManager profileManager;
    private final ConfigurationSection currencyOres;

    public MiningListener(StaffHunt plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.oreManager = plugin.getOreManager();
        this.profileManager = plugin.getPlayerProfileManager();
        this.currencyOres = plugin.getConfig().getConfigurationSection("currency-ores");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!gameManager.isGameActive()) return;

        Player player = event.getPlayer();
        if (!gameManager.isHunter(player)) return;

        // ALWAYS cancel the break event for hunters to prevent map griefing!
        event.setCancelled(true);

        Block block = event.getBlock();
        Material type = block.getType();

        // Check if the broken ore is one that gives currency
        if (currencyOres != null && currencyOres.isSet(type.name())) {

            // --- NEW: Randomize Mining Coins ---
            int min = currencyOres.getInt(type.name() + ".min-value", 5);
            int max = currencyOres.getInt(type.name() + ".max-value", 35);
            int amount = min + new java.util.Random().nextInt((max - min) + 1);

            int regenTime = currencyOres.getInt(type.name() + ".regen-time", 30);

            PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
            if (profile != null) {
                profile.addHuntCoins(amount);

                // Send feedback to the player
                String message = ChatColor.GOLD + "+ " + amount + " Hunt Coins";
                plugin.getUiManager().sendActionBar(player, message);
            }

            // Start the regeneration process using the per-ore time
            oreManager.startRegeneration(block, regenTime);
            event.setExpToDrop(0);
        }
    }
}