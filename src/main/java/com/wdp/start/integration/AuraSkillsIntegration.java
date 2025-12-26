package com.wdp.start.integration;

import com.wdp.start.WDPStartPlugin;


import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.user.SkillsUser;
import dev.aurelium.auraskills.api.registry.GlobalRegistry;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;


/**
 * AuraSkills integration for skill level tracking
 */
public class AuraSkillsIntegration implements Listener {
    
    private final WDPStartPlugin plugin;
    private AuraSkillsApi auraSkills;

    private boolean enabled;

    public AuraSkillsIntegration(WDPStartPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        // Ensure AuraSkills plugin is present
        if (Bukkit.getPluginManager().getPlugin("AuraSkills") == null) {
            enabled = false;
            return;
        }

        // Get API instance
        auraSkills = AuraSkillsApi.get();
        if (auraSkills == null) {
            enabled = false;
            plugin.getLogger().warning("AuraSkills API not available.");
            return;
        }

        enabled = true;

        // Register as listener for skill events
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("AuraSkills integration enabled.");
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
            SkillsUser user = auraSkills.getUser(player.getUniqueId());
            if (user == null) return 0;

            // First try the default Skills enum
            try {
                dev.aurelium.auraskills.api.skill.Skills s = dev.aurelium.auraskills.api.skill.Skills.valueOf(skillName.toUpperCase());
                return user.getSkillLevel(s);
            } catch (IllegalArgumentException ignored) {
                // Not a default skill, try global registry for custom skills
                GlobalRegistry registry = auraSkills.getGlobalRegistry();
                Skill skill = registry.getSkill(NamespacedId.of("auraskills", skillName.toLowerCase()));
                if (skill == null) return 0;
                return user.getSkillLevel(skill);
            }

        } catch (Exception e) {
            plugin.debug("Failed to get skill level: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Give skill tokens to a player
     * Uses Vault economy to interact with SkillCoins tokens
     */
    public void giveSkillTokens(Player player, int amount) {
        if (!isEnabled()) return;

        try {
            // Use Vault economy to give tokens
            // Note: SkillCoins exposes tokens via Vault with currency name "🎟"
            Economy economy = getVaultEconomy();
            if (economy == null) {
                plugin.debug("Vault economy not available");
                return;
            }

            // SkillCoins uses Vault integration - tokens are handled through the economy
            // Since SkillCoins supports both COINS and TOKENS, we need to use the specific command
            // or access the internal API if available
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "skillcoins give " + player.getName() + " tokens " + amount);
            plugin.debug("Gave " + amount + " skill tokens to " + player.getName());

        } catch (Exception e) {
            plugin.debug("Failed to give skill tokens: " + e.getMessage());
        }
    }

    /**
     * Give skill coins to a player
     * Uses Vault economy to interact with SkillCoins
     */
    public void giveSkillCoins(Player player, int amount) {
        if (!isEnabled()) return;

        try {
            // Use Vault economy to give coins
            Economy economy = getVaultEconomy();
            if (economy == null) {
                plugin.debug("Vault economy not available");
                return;
            }

            // Deposit coins directly via Vault
            economy.depositPlayer(player, amount);
            plugin.debug("Gave " + amount + " skill coins to " + player.getName());

        } catch (Exception e) {
            plugin.debug("Failed to give skill coins: " + e.getMessage());
        }
    }

    /**
     * Get the Vault economy provider
     */
    private Economy getVaultEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return null;
        }
        
        return rsp.getProvider();
    }

    /**
     * Listen for AuraSkills SkillLevelUpEvent and handle quest updates
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSkillLevelUp(SkillLevelUpEvent event) {
        try {
            Player player = event.getPlayer();

            Object skillObj = event.getSkill();
            String skillName;
            if (skillObj instanceof Enum) {
                skillName = ((Enum<?>) skillObj).name().toLowerCase();
            } else {
                skillName = skillObj.toString().toLowerCase();
            }

            int level = event.getLevel();

            handleSkillLevelUp(player, skillName, level);
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
