package me.siwannie.staffhunt;

import me.siwannie.staffhunt.commands.ShopCommand;
import me.siwannie.staffhunt.commands.StaffHuntCommand;
import me.siwannie.staffhunt.game.*;
import me.siwannie.staffhunt.integrations.PAPI_Expansion;
import me.siwannie.staffhunt.listeners.CustomItemListener;
import me.siwannie.staffhunt.listeners.FatalDamageListener;
import me.siwannie.staffhunt.listeners.MiningListener;
import me.siwannie.staffhunt.listeners.PlayerConnectionListener;
import me.siwannie.staffhunt.player.PlayerProfileManager;
import me.siwannie.staffhunt.shop.ShopGUI;
import me.siwannie.staffhunt.ui.UIManager;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaffHunt extends JavaPlugin {

    private static StaffHunt instance;
    private GameManager gameManager;
    private PlayerProfileManager playerProfileManager;
    private OreManager oreManager;
    private RespawnManager respawnManager;
    private ShopGUI shopGUI;
    private LuckPerms luckPerms;
    private KitManager kitManager;
    private DataManager dataManager;
    private UIManager uiManager;

    @Override
    public void onEnable() {
        instance = this;

        // --- CONFIGURATION ---
        saveDefaultConfig();

        // --- DEPENDENCY HOOKS ---
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("Could not find PlaceholderAPI! This plugin is required.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            getLogger().severe("Could not find LuckPerms! This plugin is required.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.luckPerms = provider.getProvider();
        getLogger().info("Successfully hooked into LuckPerms and PlaceholderAPI.");

        // --- MANAGERS ---
        this.playerProfileManager = new PlayerProfileManager();
        this.uiManager = new UIManager(this);
        this.kitManager = new KitManager(this);
        this.oreManager = new OreManager(this);
        this.respawnManager = new RespawnManager(this);
        this.gameManager = new GameManager(this);
        this.shopGUI = new ShopGUI(this);
        this.dataManager = new DataManager(this);


        // --- LISTENERS ---
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(new FatalDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomItemListener(this), this);
        getServer().getPluginManager().registerEvents(shopGUI, this);

        // --- COMMANDS ---
        getCommand("staffhunt").setExecutor(new StaffHuntCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));

        // --- PAPI EXPANSION ---
        new PAPI_Expansion(this).register();
        getLogger().info("StaffHunt has been enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isGameActive()) {
            // Calling endGame(false) indicates staff did not survive, ending the game immediately.
            gameManager.endGame(false);
        }
        if (oreManager != null) {
            oreManager.revertAllOres(); // Revert any bedrock to ore on shutdown
        }
        getLogger().info("StaffHunt has been disabled.");
    }

    // --- GETTERS ---
    public static StaffHunt getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public PlayerProfileManager getPlayerProfileManager() {
        return playerProfileManager;
    }

    public OreManager getOreManager() {
        return oreManager;
    }

    public RespawnManager getRespawnManager() {
        return respawnManager;
    }

    public ShopGUI getShopGUI() {
        return shopGUI;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public UIManager getUiManager() {
        return uiManager;
    }
}