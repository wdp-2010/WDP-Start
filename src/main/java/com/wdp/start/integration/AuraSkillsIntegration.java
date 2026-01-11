package com.wdp.start.integration;

import com.wdp.start.WDPStartPlugin;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;
import dev.aurelium.auraskills.api.registry.GlobalRegistry;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * AuraSkills integration for skill tracking and SkillCoins/Token economy.
 * 
 * This integration provides:
 * - Skill level queries
 * - SkillCoins balance management (via SkillCoinsEconomy)
 * - SkillTokens balance management (via SkillCoinsEconomy)
 * - Skill level-up event handling for quest progression
 * 
 * Uses reflection to access SkillCoinsEconomy to avoid compile-time dependency
 * on internal AuraSkills classes while still accessing the full API.
 */
public class AuraSkillsIntegration implements Listener {
    
    private final WDPStartPlugin plugin;
    private AuraSkillsApi auraSkillsApi;
    private Object skillCoinsEconomy; // SkillCoinsEconomy instance via reflection
    private Class<?> currencyTypeClass; // CurrencyType enum class
    private Object coinsType; // CurrencyType.COINS
    private Object tokensType; // CurrencyType.TOKENS
    private boolean enabled;
    private boolean economyAvailable;
    
    public AuraSkillsIntegration(WDPStartPlugin plugin) {
        this.plugin = plugin;
        setup();
    }
    
    private void setup() {
        // Check if AuraSkills plugin is present
        org.bukkit.plugin.Plugin auraPlugin = Bukkit.getPluginManager().getPlugin("AuraSkills");
        if (auraPlugin == null) {
            enabled = false;
            plugin.getLogger().info("AuraSkills not found - integration disabled.");
            return;
        }
        
        // Get AuraSkills API
        try {
            auraSkillsApi = AuraSkillsApi.get();
            if (auraSkillsApi == null) {
                enabled = false;
                plugin.getLogger().warning("AuraSkills API not available.");
                return;
            }
        } catch (Exception e) {
            enabled = false;
            plugin.getLogger().warning("Failed to get AuraSkills API: " + e.getMessage());
            return;
        }
        
        enabled = true;
        
        // Setup SkillCoins economy access via reflection
        setupSkillCoinsEconomy(auraPlugin);
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        plugin.getLogger().info("AuraSkills integration enabled" + 
            (economyAvailable ? " with SkillCoins economy." : " (economy not available)."));
    }
    
    /**
     * Setup SkillCoins economy access via reflection
     * This avoids compile-time dependency on internal classes
     */
    private void setupSkillCoinsEconomy(org.bukkit.plugin.Plugin auraPlugin) {
        try {
            // Get getSkillCoinsEconomy() method
            Method getEconomyMethod = auraPlugin.getClass().getMethod("getSkillCoinsEconomy");
            skillCoinsEconomy = getEconomyMethod.invoke(auraPlugin);
            
            if (skillCoinsEconomy == null) {
                plugin.debug("SkillCoinsEconomy is null - economy features disabled.");
                economyAvailable = false;
                return;
            }
            
            // Get CurrencyType enum
            currencyTypeClass = Class.forName("dev.aurelium.auraskills.common.skillcoins.CurrencyType");
            coinsType = currencyTypeClass.getField("COINS").get(null);
            tokensType = currencyTypeClass.getField("TOKENS").get(null);
            
            economyAvailable = true;
            plugin.debug("SkillCoins economy initialized successfully.");
            
        } catch (NoSuchMethodException e) {
            plugin.debug("getSkillCoinsEconomy method not found - this may be standard AuraSkills without SkillCoins.");
            economyAvailable = false;
        } catch (Exception e) {
            plugin.debug("Failed to setup SkillCoins economy: " + e.getMessage());
            economyAvailable = false;
        }
    }
    
    // ==================== STATUS METHODS ====================
    
    /**
     * Check if AuraSkills integration is enabled
     */
    public boolean isEnabled() {
        return enabled && auraSkillsApi != null;
    }
    
    /**
     * Check if SkillCoins economy is available
     */
    public boolean isEconomyAvailable() {
        return economyAvailable && skillCoinsEconomy != null;
    }
    
    // ==================== SKILL METHODS ====================
    
    /**
     * Get a player's skill level
     * @param player The player
     * @param skillName The skill name (case-insensitive)
     * @return The skill level, or 0 if not found
     */
    public int getSkillLevel(Player player, String skillName) {
        if (!isEnabled()) return 0;
        
        try {
            SkillsUser user = auraSkillsApi.getUser(player.getUniqueId());
            if (user == null || !user.isLoaded()) return 0;
            
            Skill skill = findSkill(skillName);
            if (skill == null) return 0;
            
            return user.getSkillLevel(skill);
        } catch (Exception e) {
            plugin.debug("Failed to get skill level for " + skillName + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Find a skill by name
     * @param skillName The skill name (case-insensitive)
     * @return The Skill, or null if not found
     */
    private Skill findSkill(String skillName) {
        // Try built-in Skills enum first
        try {
            return Skills.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // Not a built-in skill
        }
        
        // Try global registry for custom skills
        try {
            GlobalRegistry registry = auraSkillsApi.getGlobalRegistry();
            return registry.getSkill(NamespacedId.of("auraskills", skillName.toLowerCase()));
        } catch (Exception e) {
            plugin.debug("Skill not found: " + skillName);
            return null;
        }
    }
    
    // ==================== ECONOMY METHODS ====================
    
    /**
     * Get a player's SkillCoins balance
     * @param player The player
     * @return The balance, or 0 if economy unavailable
     */
    public double getCoins(Player player) {
        return getBalance(player.getUniqueId(), coinsType);
    }
    
    /**
     * Get a player's SkillTokens balance
     * @param player The player
     * @return The balance, or 0 if economy unavailable
     */
    public double getTokens(Player player) {
        return getBalance(player.getUniqueId(), tokensType);
    }
    
    /**
     * Give SkillCoins to a player (silent - no message)
     * @param player The player
     * @param amount Amount to give
     * @return true if successful
     */
    public boolean giveCoins(Player player, double amount) {
        return addBalance(player.getUniqueId(), coinsType, amount);
    }
    
    /**
     * Give SkillTokens to a player (silent - no message)
     * @param player The player
     * @param amount Amount to give
     * @return true if successful
     */
    public boolean giveTokens(Player player, double amount) {
        return addBalance(player.getUniqueId(), tokensType, amount);
    }
    
    /**
     * Take SkillCoins from a player (silent - no message)
     * @param player The player
     * @param amount Amount to take
     * @return true if successful
     */
    public boolean takeCoins(Player player, double amount) {
        return subtractBalance(player.getUniqueId(), coinsType, amount);
    }
    
    /**
     * Take SkillTokens from a player (silent - no message)
     * @param player The player
     * @param amount Amount to take
     * @return true if successful
     */
    public boolean takeTokens(Player player, double amount) {
        return subtractBalance(player.getUniqueId(), tokensType, amount);
    }
    
    /**
     * Check if player has enough SkillCoins
     * @param player The player
     * @param amount Amount to check
     * @return true if player has enough
     */
    public boolean hasCoins(Player player, double amount) {
        return hasBalance(player.getUniqueId(), coinsType, amount);
    }
    
    /**
     * Check if player has enough SkillTokens
     * @param player The player
     * @param amount Amount to check
     * @return true if player has enough
     */
    public boolean hasTokens(Player player, double amount) {
        return hasBalance(player.getUniqueId(), tokensType, amount);
    }
    
    // ==================== REFLECTION-BASED ECONOMY ACCESS ====================
    
    private double getBalance(UUID uuid, Object currencyType) {
        if (!isEconomyAvailable() || currencyType == null) return 0;
        
        try {
            Method method = skillCoinsEconomy.getClass().getMethod("getBalance", UUID.class, currencyTypeClass);
            Object result = method.invoke(skillCoinsEconomy, uuid, currencyType);
            return (double) result;
        } catch (Exception e) {
            plugin.debug("Failed to get balance: " + e.getMessage());
            return 0;
        }
    }
    
    private boolean addBalance(UUID uuid, Object currencyType, double amount) {
        if (!isEconomyAvailable() || currencyType == null) return false;
        
        try {
            Method method = skillCoinsEconomy.getClass().getMethod("addBalance", UUID.class, currencyTypeClass, double.class);
            method.invoke(skillCoinsEconomy, uuid, currencyType, amount);
            return true;
        } catch (Exception e) {
            plugin.debug("Failed to add balance: " + e.getMessage());
            return false;
        }
    }
    
    private boolean subtractBalance(UUID uuid, Object currencyType, double amount) {
        if (!isEconomyAvailable() || currencyType == null) return false;
        
        try {
            Method method = skillCoinsEconomy.getClass().getMethod("subtractBalance", UUID.class, currencyTypeClass, double.class);
            method.invoke(skillCoinsEconomy, uuid, currencyType, amount);
            return true;
        } catch (Exception e) {
            plugin.debug("Failed to subtract balance: " + e.getMessage());
            return false;
        }
    }
    
    private boolean hasBalance(UUID uuid, Object currencyType, double amount) {
        if (!isEconomyAvailable() || currencyType == null) return false;
        
        try {
            Method method = skillCoinsEconomy.getClass().getMethod("hasBalance", UUID.class, currencyTypeClass, double.class);
            Object result = method.invoke(skillCoinsEconomy, uuid, currencyType, amount);
            return (boolean) result;
        } catch (Exception e) {
            plugin.debug("Failed to check balance: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== LEGACY COMPATIBILITY METHODS ====================
    
    /**
     * Legacy method - give skill tokens
     * @deprecated Use giveTokens(Player, double) instead
     */
    @Deprecated
    public void giveSkillTokens(Player player, int amount) {
        giveTokens(player, amount);
    }
    
    /**
     * Legacy method - give skill coins
     * @deprecated Use giveCoins(Player, double) instead
     */
    @Deprecated
    public void giveSkillCoins(Player player, int amount) {
        giveCoins(player, amount);
    }
    
    // ==================== EVENT HANDLING ====================
    
    /**
     * Handle skill level up events for quest tracking
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillLevelUp(SkillLevelUpEvent event) {
        try {
            Player player = event.getPlayer();
            Skill skill = event.getSkill();
            int level = event.getLevel();
            
            // Get skill name (handle both enum and custom skills)
            String skillName = skill.getId().getKey().toLowerCase();
            
            plugin.debug("Skill level up: " + player.getName() + " - " + skillName + " -> " + level);
            
            // Delegate to QuestManager
            plugin.getQuestManager().onSkillLevelUp(player, skillName, level);
            
        } catch (Exception e) {
            plugin.debug("Failed to handle skill level up event: " + e.getMessage());
        }
    }
}
