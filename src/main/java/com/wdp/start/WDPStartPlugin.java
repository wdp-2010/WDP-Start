package com.wdp.start;

import com.wdp.start.command.QuestCommand;
import com.wdp.start.config.ConfigManager;
import com.wdp.start.config.MessageManager;
import com.wdp.start.integration.AuraSkillsIntegration;
import com.wdp.start.integration.VaultIntegration;
import com.wdp.start.integration.WorldGuardIntegration;
import com.wdp.start.listener.QuestMenuListener;
import com.wdp.start.listener.ShopMenuListener;
import com.wdp.start.listener.PlayerListener;
import com.wdp.start.listener.QuestListener;
import com.wdp.start.path.PathGuideManager;
import com.wdp.start.path.PortalZoneManager;
import com.wdp.start.path.RTPManager;
import com.wdp.start.player.PlayerDataManager;
import com.wdp.start.quest.QuestManager;
import com.wdp.start.quest.BossBarManager;
import com.wdp.start.shop.SimpleShopMenu;
import com.wdp.start.storage.DatabaseManager;
import com.wdp.start.ui.menu.QuestMenuCoordinator;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WDP-Start - Professional Get Started Quest System
 * Guides new players through their first experience on the server
 * 
 * @author WDP Development Team
 * @version 1.0.0
 */
public class WDPStartPlugin extends JavaPlugin {
    
    private static WDPStartPlugin instance;
    
    // Managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private PlayerDataManager playerDataManager;
    private DatabaseManager databaseManager;
    private QuestManager questManager;
    private BossBarManager bossBarManager;
    private QuestMenuCoordinator questMenuCoordinator;
    private SimpleShopMenu simpleShopMenu;
    private PathGuideManager pathGuideManager;
    private PortalZoneManager portalZoneManager;
    private RTPManager rtpManager;
    
    // Integrations
    private VaultIntegration vaultIntegration;
    private AuraSkillsIntegration auraSkillsIntegration;
    private WorldGuardIntegration worldGuardIntegration;
    
    // Listeners (for API access)
    private QuestListener questListener;
    
    // Hex color pattern
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    @Override
    public void onEnable() {
        instance = this;
        
        // ASCII Art Banner
        getLogger().info("");
        getLogger().info("╔═══════════════════════════════════════╗");
        getLogger().info("║         WDP-Start v" + getDescription().getVersion() + "            ║");
        getLogger().info("║   Get Started Quest System            ║");
        getLogger().info("╚═══════════════════════════════════════╝");
        getLogger().info("");
        
        // Initialize managers
        initializeManagers();
        
        // Extract SkillCoinsShop resources
        extractSkillCoinsShopResources();
        
        // Initialize API (must be after managers)
        com.wdp.start.api.WDPStartAPI.init(this);
        getLogger().info("WDP-Start API initialized.");
        
        // Setup integrations
        setupIntegrations();
        
        // Register listeners
        registerListeners();
        
        // Register commands
        registerCommands();
        
        // Schedule auto-save
        scheduleAutoSave();
        
        getLogger().info("WDP-Start has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Shutdown portal zone manager
        if (portalZoneManager != null) {
            portalZoneManager.shutdown();
        }
        
        // Shutdown path guide
        if (pathGuideManager != null) {
            pathGuideManager.shutdown();
        }
        
        // Save all player data to database
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        // Save all player data (legacy)
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        
        // Clean up boss bars
        if (bossBarManager != null) {
            bossBarManager.cleanup();
        }
        
        getLogger().info("WDP-Start has been disabled.");
    }
    
    private void initializeManagers() {
        // Config
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        
        // Extract navbar.yml if it doesn't exist
        java.io.File navbarFile = new java.io.File(getDataFolder(), "navbar.yml");
        if (!navbarFile.exists()) {
            saveResource("navbar.yml", false);
            getLogger().info("Extracted navbar.yml configuration.");
        }
        
        // Messages
        messageManager = new MessageManager(this);
        
        // Database (SQLite)
        databaseManager = new DatabaseManager(this);
        
        // Player data (uses database now)
        playerDataManager = new PlayerDataManager(this, databaseManager);
        
        // Quest manager
        questManager = new QuestManager(this);
        
        // Boss bar manager
        bossBarManager = new BossBarManager(this);
        
        // Quest menu
        questMenuCoordinator = new QuestMenuCoordinator(this);
        
        // Simple shop menu (for Quest 3 & 4)
        simpleShopMenu = new SimpleShopMenu(this);
        simpleShopMenu.initialize();
        
        // Path guide manager
        pathGuideManager = new PathGuideManager(this);
        
        // Portal zone manager
        portalZoneManager = new PortalZoneManager(this);
        
        // RTP manager (for teleporting after portal zone)
        rtpManager = new RTPManager(this);
        
        getLogger().info("Managers initialized successfully.");
        getLogger().info("Database: SQLite (playerdata.db)");
    }
    
    private void extractSkillCoinsShopResources() {
        try {
            // Extract sections - ALL sections including EconomyShopGUI
            String[] sectionFiles = {
                "SkillCoinsShop/sections/Blocks.yml",
                "SkillCoinsShop/sections/Combat.yml", 
                "SkillCoinsShop/sections/Enchantments.yml",
                "SkillCoinsShop/sections/Farming.yml",
                "SkillCoinsShop/sections/Food.yml",
                "SkillCoinsShop/sections/Miscellaneous.yml",
                "SkillCoinsShop/sections/Potions.yml",
                "SkillCoinsShop/sections/Redstone.yml",
                "SkillCoinsShop/sections/Resources.yml",
                "SkillCoinsShop/sections/SkillLevels.yml",
                "SkillCoinsShop/sections/Token_Exchange.yml",
                "SkillCoinsShop/sections/Tokens.yml",
                "SkillCoinsShop/sections/Tools.yml",
                "SkillCoinsShop/sections/Decoration.yml",
                "SkillCoinsShop/sections/Dyes.yml",
                "SkillCoinsShop/sections/Enchanting.yml",
                "SkillCoinsShop/sections/Mobs.yml",
                "SkillCoinsShop/sections/Music.yml",
                "SkillCoinsShop/sections/Ores.yml",
                "SkillCoinsShop/sections/SpawnEggs.yml",
                "SkillCoinsShop/sections/Spawners.yml",
                "SkillCoinsShop/sections/Workstations.yml",
                "SkillCoinsShop/sections/Z_EverythingElse.yml"
            };
            
            for (String file : sectionFiles) {
                saveResource(file, true);
            }
            
            // Extract shops - ALL shops including EconomyShopGUI
            String[] shopFiles = {
                "SkillCoinsShop/shops/Blocks.yml",
                "SkillCoinsShop/shops/Combat.yml",
                "SkillCoinsShop/shops/Enchantments.yml", 
                "SkillCoinsShop/shops/Farming.yml",
                "SkillCoinsShop/shops/Food.yml",
                "SkillCoinsShop/shops/Miscellaneous.yml",
                "SkillCoinsShop/shops/Potions.yml",
                "SkillCoinsShop/shops/Redstone.yml",
                "SkillCoinsShop/shops/Resources.yml",
                "SkillCoinsShop/shops/SkillLevels.yml",
                "SkillCoinsShop/shops/Token_Exchange.yml",
                "SkillCoinsShop/shops/Tokens.yml",
                "SkillCoinsShop/shops/Tools.yml",
                "SkillCoinsShop/shops/Decoration.yml",
                "SkillCoinsShop/shops/Dyes.yml",
                "SkillCoinsShop/shops/Enchanting.yml",
                "SkillCoinsShop/shops/Mobs.yml",
                "SkillCoinsShop/shops/Music.yml",
                "SkillCoinsShop/shops/Ores.yml",
                "SkillCoinsShop/shops/SpawnEggs.yml",
                "SkillCoinsShop/shops/Spawners.yml",
                "SkillCoinsShop/shops/Workstations.yml",
                "SkillCoinsShop/shops/Z_EverythingElse.yml"
            };
            
            for (String file : shopFiles) {
                saveResource(file, true);
            }
            
            getLogger().info("SkillCoinsShop resources extracted successfully.");
        } catch (Exception e) {
            getLogger().warning("Failed to extract SkillCoinsShop resources: " + e.getMessage());
        }
    }
    
    private void setupIntegrations() {
        // Vault
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultIntegration = new VaultIntegration(this);
            if (vaultIntegration.isEnabled()) {
                getLogger().info("Vault integration enabled.");
            }
        } else {
            getLogger().warning("Vault not found! Economy features will be limited.");
        }
        
        // AuraSkills
        if (Bukkit.getPluginManager().getPlugin("AuraSkills") != null) {
            auraSkillsIntegration = new AuraSkillsIntegration(this);
            getLogger().info("AuraSkills integration enabled.");
        } else {
            getLogger().warning("AuraSkills not found! Skill-related quests will be limited.");
        }
        
        // WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardIntegration = new WorldGuardIntegration(this);
            if (worldGuardIntegration.isEnabled()) {
                getLogger().info("WorldGuard integration enabled.");
            }
        } else {
            getLogger().warning("WorldGuard not found! Region-based portal zones will not work.");
        }
    }
    
    private void registerListeners() {
        // Player events (join, quit)
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Quest tracking events
        questListener = new QuestListener(this);
        Bukkit.getPluginManager().registerEvents(questListener, this);
        
        // Menu click events (separated for clarity)
        Bukkit.getPluginManager().registerEvents(new QuestMenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ShopMenuListener(this), this);
        
        getLogger().info("Listeners registered successfully.");
    }
    
    private void registerCommands() {
        QuestCommand questCommand = new QuestCommand(this);
        getCommand("start").setExecutor(questCommand);
        getCommand("start").setTabCompleter(questCommand);
        
        getLogger().info("Commands registered successfully.");
    }
    
    private void scheduleAutoSave() {
        int saveInterval = configManager.getSaveInterval();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            playerDataManager.saveAll();
            if (configManager.isDebug()) {
                getLogger().info("Auto-saved player data.");
            }
        }, saveInterval * 20L, saveInterval * 20L);
    }
    
    /**
     * Reload the plugin configuration
     */
    public void reload() {
        reloadConfig();
        configManager.reload();
        messageManager.reload();
        if (simpleShopMenu != null) {
            simpleShopMenu.reload();
        }
        getLogger().info("Configuration reloaded.");
    }
    
    // ==================== GETTERS ====================
    
    public static WDPStartPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public QuestManager getQuestManager() {
        return questManager;
    }
    
    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }
    
    public QuestMenuCoordinator getQuestMenuCoordinator() {
        return questMenuCoordinator;
    }
    
    public SimpleShopMenu getSimpleShopMenu() {
        return simpleShopMenu;
    }
    
    public VaultIntegration getVaultIntegration() {
        return vaultIntegration;
    }
    
    public AuraSkillsIntegration getAuraSkillsIntegration() {
        return auraSkillsIntegration;
    }
    
    public WorldGuardIntegration getWorldGuardIntegration() {
        return worldGuardIntegration;
    }
    
    public QuestListener getQuestListener() {
        return questListener;
    }
    
    public PathGuideManager getPathGuideManager() {
        return pathGuideManager;
    }
    
    public PortalZoneManager getPortalZoneManager() {
        return portalZoneManager;
    }
    
    public RTPManager getRtpManager() {
        return rtpManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Translate hex color codes in a string
     * Supports &#RRGGBB format
     */
    public static String hex(String message) {
        if (message == null) return "";
        
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        
        while (matcher.find()) {
            String color = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + color).toString());
        }
        matcher.appendTail(buffer);
        
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    /**
     * Log a debug message if debug mode is enabled
     */
    public void debug(String message) {
        if (configManager != null && configManager.isDebug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * Log an error with exception details
     */
    public void logError(String message, Throwable throwable) {
        getLogger().log(Level.SEVERE, message, throwable);
    }
}
