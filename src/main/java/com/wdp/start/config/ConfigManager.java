package com.wdp.start.config;

import com.wdp.start.WDPStartPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.List;

/**
 * Manages the main configuration file
 */
public class ConfigManager {
    
    private final WDPStartPlugin plugin;
    private final ConfigMigration migration;
    private FileConfiguration config;
    private static final int CONFIG_VERSION = 2;
    
    public ConfigManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
        this.migration = new ConfigMigration(plugin);
        reload();
    }
    
    public void reload() {
        // Migrate configs if needed
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        migration.migrateConfig(configFile, "config.yml", CONFIG_VERSION);
        
        File navbarFile = new File(plugin.getDataFolder(), "navbar.yml");
        migration.migrateConfig(navbarFile, "navbar.yml", 1);
        
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    // ==================== GENERAL SETTINGS ====================
    
    public boolean isWelcomeMessage() {
        return config.getBoolean("general.welcome-message", true);
    }
    
    public boolean isAutoStart() {
        return config.getBoolean("general.auto-start", false);
    }
    
    public boolean isSoundsEnabled() {
        return config.getBoolean("general.enable-sounds", true);
    }
    
    public boolean isParticlesEnabled() {
        return config.getBoolean("general.enable-particles", true);
    }
    
    public boolean isBroadcastEnabled() {
        return config.getBoolean("general.broadcast-completions", false);
    }
    
    public boolean isDebug() {
        return config.getBoolean("general.debug", false);
    }
    
    // ==================== PARTICLE PATH ====================
    
    public boolean isPathEnabled() {
        return config.getBoolean("particle-path.enabled", true);
    }
    
    public Location getPathTarget() {
        String worldName = config.getString("particle-path.target.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        
        double x = config.getDouble("particle-path.target.x", 0);
        double y = config.getDouble("particle-path.target.y", 64);
        double z = config.getDouble("particle-path.target.z", 0);
        
        return new Location(world, x, y, z);
    }
    
    public String getParticleType() {
        return config.getString("particle-path.particle-type", "TOTEM_OF_UNDYING");
    }
    
    public double getParticleHeight() {
        return config.getDouble("particle-path.particle-height", 1.5);
    }
    
    public int getParticleCount() {
        return config.getInt("particle-path.particle-count", 3);
    }
    
    public int getAnimationSpeed() {
        return config.getInt("particle-path.animation-speed", 2);
    }
    
    public int getPathLength() {
        return config.getInt("particle-path.path-length", 999);
    }
    
    public boolean isShowFullPath() {
        return config.getBoolean("particle-path.show-full-path", true);
    }
    
    public int getPathMinimumRuntime() {
        return config.getInt("particle-path.minimum-runtime", 20);
    }
    
    // ==================== QUEST 1: LEAVE & TELEPORT ====================
    
    public int getQuest1SkillCoins() {
        return config.getInt("quest1.rewards.skillcoins", 100);
    }
    
    public List<String> getQuest1Items() {
        return config.getStringList("quest1.rewards.items");
    }
    
    // ==================== PORTAL ZONE (Quest 1) ====================
    
    public boolean isPortalZoneUseWorldGuard() {
        return config.getBoolean("portal-zone.use-worldguard", false);
    }
    
    public String getPortalZoneWorldGuardRegion() {
        return config.getString("portal-zone.worldguard-region", "portal_zone");
    }
    
    public String getPortalZoneWorld() {
        return config.getString("portal-zone.world", "world");
    }
    
    public int getPortalZoneMinX() {
        return config.getInt("portal-zone.min-x", 120);
    }
    
    public int getPortalZoneMaxX() {
        return config.getInt("portal-zone.max-x", 130);
    }
    
    public int getPortalZoneMinY() {
        return config.getInt("portal-zone.min-y", 170);
    }
    
    public int getPortalZoneMaxY() {
        return config.getInt("portal-zone.max-y", 180);
    }
    
    public int getPortalZoneMinZ() {
        return config.getInt("portal-zone.min-z", 200);
    }
    
    public int getPortalZoneMaxZ() {
        return config.getInt("portal-zone.max-z", 210);
    }
    
    // ==================== QUEST 2: WOODCUTTING ====================
    
    public int getQuest2TargetLevel() {
        return config.getInt("quest2.target-level", 2);
    }
    
    public String getQuest2Skill() {
        return config.getString("quest2.skill", "foraging");
    }
    
    public boolean isQuest2OverrideReward() {
        return config.getBoolean("quest2.override-reward", true);
    }
    
    public int getQuest2SkillCoins() {
        return config.getInt("quest2.rewards.skillcoins", 100);
    }
    
    public String getQuest2CompletionMessage() {
        return config.getString("quest2.completion-message", 
            "&a&l✓ &aGreat job! Use your &e100 SkillCoins &ato buy something from &6/shop&a!");
    }
    
    // ==================== QUEST 3: SHOP TUTORIAL ====================
    
    public int getQuest3MinItemCost() {
        return config.getInt("quest3.min-item-cost", 20);
    }
    
    public boolean isQuest3EnsureBalance() {
        return config.getBoolean("quest3.ensure-balance", true);
    }
    
    public int getQuest3EnsureBalanceAmount() {
        return config.getInt("quest3.ensure-balance-amount", 40);
    }
    
    public int getQuest3SkillCoins() {
        return config.getInt("quest3.rewards.skillcoins", 50);
    }
    
    // ==================== QUEST 4: TOKEN PURCHASE ====================
    
    public int getQuest4TokensRequired() {
        return config.getInt("quest4.tokens-required", 1);
    }
    
    public boolean isQuest4AutoTopup() {
        return config.getBoolean("quest4.auto-topup", true);
    }
    
    public int getQuest4TokenCost() {
        return config.getInt("quest4.token-cost", 100);
    }
    
    public int getQuest4SkillCoins() {
        return config.getInt("quest4.rewards.skillcoins", 200);
    }
    
    // ==================== QUEST 5: MENU ORIENTATION ====================
    
    public int getQuest5SkillCoins() {
        return config.getInt("quest5.rewards.skillcoins", 50);
    }
    
    // ==================== QUEST 6: DISCORD ====================
    
    public String getDiscordLink() {
        return config.getString("quest6.discord-link", "https://discord.gg/yourserver");
    }
    
    public int getQuest6SkillCoins() {
        return config.getInt("quest6.final-rewards.skillcoins", 500);
    }
    
    public int getQuest6SkillTokens() {
        return config.getInt("quest6.final-rewards.skilltokens", 25);
    }
    
    public List<String> getQuest6Items() {
        return config.getStringList("quest6.final-rewards.items");
    }
    
    // ==================== CANCELLATION ====================
    
    public boolean isCancellationEnabled() {
        return config.getBoolean("cancellation.enabled", true);
    }
    
    public boolean isRefundUnspent() {
        return config.getBoolean("cancellation.refund-unspent", true);
    }
    
    public int getSpendTrackingSeconds() {
        return config.getInt("cancellation.spend-tracking-seconds", 600);
    }
    
    // ==================== GUI ====================
    
    public int getGuiUpdateInterval() {
        return config.getInt("gui.update-interval", 20);
    }
    
    public String getColorPrimary() {
        return config.getString("gui.colors.primary", "#FFD700");
    }
    
    public String getColorSecondary() {
        return config.getString("gui.colors.secondary", "#55FFFF");
    }
    
    public String getColorSuccess() {
        return config.getString("gui.colors.success", "#55FF55");
    }
    
    public String getColorWarning() {
        return config.getString("gui.colors.warning", "#FFFF55");
    }
    
    public String getColorError() {
        return config.getString("gui.colors.error", "#FF5555");
    }
    
    public String getColorInfo() {
        return config.getString("gui.colors.info", "#AAAAAA");
    }
    
    // ==================== STORAGE ====================
    
    public String getStorageType() {
        return config.getString("storage.type", "YAML");
    }
    
    public int getSaveInterval() {
        return config.getInt("storage.save-interval", 300);
    }
    
    // ==================== MYSQL ====================
    
    public String getMysqlHost() {
        return config.getString("mysql.host", "localhost");
    }
    
    public int getMysqlPort() {
        return config.getInt("mysql.port", 3306);
    }
    
    public String getMysqlDatabase() {
        return config.getString("mysql.database", "wdpstart");
    }
    
    public String getMysqlUsername() {
        return config.getString("mysql.username", "root");
    }
    
    public String getMysqlPassword() {
        return config.getString("mysql.password", "password");
    }
    
    public int getMysqlPoolSize() {
        return config.getInt("mysql.pool-size", 10);
    }
}
