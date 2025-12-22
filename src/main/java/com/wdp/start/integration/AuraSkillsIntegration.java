package com.wdp.start.integration;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * AuraSkills integration for skill level tracking
 */
public class AuraSkillsIntegration implements Listener {
    
    private final WDPStartPlugin plugin;
    private Plugin auraSkills;
    private boolean enabled;
    
    // Reflection cache
    private Object skillsApi;
    private Method getSkillLevelMethod;
    private Method addSkillTokensMethod;
    
    public AuraSkillsIntegration(WDPStartPlugin plugin) {
        this.plugin = plugin;
        setup();
    }
    
    private void setup() {
        auraSkills = Bukkit.getPluginManager().getPlugin("AuraSkills");
        
        if (auraSkills == null) {
            enabled = false;
            return;
        }
        
        try {
            // Get AuraSkills API via reflection
            Class<?> auraSkillsApiClass = Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
            Method getMethod = auraSkillsApiClass.getMethod("get");
            skillsApi = getMethod.invoke(null);
            
            enabled = true;
            
            // Register as listener for skill events
            Bukkit.getPluginManager().registerEvents(this, plugin);
            
            plugin.getLogger().info("AuraSkills integration enabled.");
            
        } catch (Exception e) {
            enabled = false;
            plugin.getLogger().warning("Failed to setup AuraSkills integration: " + e.getMessage());
        }
    }
    
    /**
     * Check if AuraSkills integration is enabled
     */
    public boolean isEnabled() {
        return enabled && auraSkills != null;
    }
    
    /**
     * Get a player's skill level
     */
    public int getSkillLevel(Player player, String skillName) {
        if (!isEnabled()) return 0;
        
        try {
            // Get the skill registry
            Method getSkillRegistryMethod = skillsApi.getClass().getMethod("getSkillRegistry");
            Object skillRegistry = getSkillRegistryMethod.invoke(skillsApi);
            
            // Get the skill by name
            Method getSkillMethod = skillRegistry.getClass().getMethod("getOrNull", 
                Class.forName("dev.aurelium.auraskills.api.registry.NamespacedId"));
            
            // Create NamespacedId
            Class<?> namespacedIdClass = Class.forName("dev.aurelium.auraskills.api.registry.NamespacedId");
            Method fromStringMethod = namespacedIdClass.getMethod("fromString", String.class);
            Object skillId = fromStringMethod.invoke(null, "auraskills/" + skillName.toLowerCase());
            
            Object skill = getSkillMethod.invoke(skillRegistry, skillId);
            if (skill == null) return 0;
            
            // Get user
            Method getUserMethod = skillsApi.getClass().getMethod("getUser", java.util.UUID.class);
            Object user = getUserMethod.invoke(skillsApi, player.getUniqueId());
            if (user == null) return 0;
            
            // Get skill level
            Method getLevelMethod = user.getClass().getMethod("getSkillLevel", 
                Class.forName("dev.aurelium.auraskills.api.skill.Skill"));
            Object level = getLevelMethod.invoke(user, skill);
            
            return (int) level;
            
        } catch (Exception e) {
            plugin.debug("Failed to get skill level: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Give skill tokens to a player
     */
    public void giveSkillTokens(Player player, int amount) {
        if (!isEnabled()) return;
        
        try {
            // This would interact with the SkillCoins system
            // For now, we'll use a command fallback
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                "skills user " + player.getName() + " modifier add wdpstart_tokens stat_modifier " + amount);
            
            plugin.debug("Gave " + amount + " skill tokens to " + player.getName());
            
        } catch (Exception e) {
            plugin.debug("Failed to give skill tokens: " + e.getMessage());
        }
    }
    
    /**
     * Listen for skill level up events
     * This uses reflection to avoid compile-time dependency
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGenericEvent(org.bukkit.event.Event event) {
        // Check if this is a skill level up event
        String eventClassName = event.getClass().getName();
        
        if (!eventClassName.contains("SkillLevelUpEvent")) {
            return;
        }
        
        try {
            // Get player from event
            Method getPlayerMethod = event.getClass().getMethod("getPlayer");
            Player player = (Player) getPlayerMethod.invoke(event);
            
            // Get skill from event
            Method getSkillMethod = event.getClass().getMethod("getSkill");
            Object skill = getSkillMethod.invoke(event);
            
            // Get skill name
            Method getNameMethod = skill.getClass().getMethod("name");
            String skillName = (String) getNameMethod.invoke(skill);
            
            // Get new level
            Method getLevelMethod = event.getClass().getMethod("getLevel");
            int level = (int) getLevelMethod.invoke(event);
            
            // Handle Quest 2 (Foraging level 2)
            handleSkillLevelUp(player, skillName.toLowerCase(), level);
            
        } catch (Exception e) {
            plugin.debug("Failed to handle skill level event: " + e.getMessage());
        }
    }
    
    /**
     * Handle skill level up for quest tracking
     */
    private void handleSkillLevelUp(Player player, String skillName, int level) {
        // Delegate to QuestManager for proper handling
        plugin.getQuestManager().onSkillLevelUp(player, skillName, level);
    }
}
