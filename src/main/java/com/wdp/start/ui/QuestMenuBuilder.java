package com.wdp.start.ui;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import com.wdp.start.quest.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds quest menu content for QuestMenu
 * Handles quest item creation and menu layout
 */
public class QuestMenuBuilder {
    
    private final WDPStartPlugin plugin;
    private final MenuHelper helper;
    private final NavbarManager navbarManager;
    
    public QuestMenuBuilder(WDPStartPlugin plugin, MenuHelper helper, NavbarManager navbarManager) {
        this.plugin = plugin;
        this.helper = helper;
        this.navbarManager = navbarManager;
    }
    
    /**
     * Build welcome menu for players who haven't started
     */
    public void buildWelcomeMenu(Inventory inv) {
        List<String> welcomeLore = plugin.getMessageManager().getList("menu.welcome.description");
        String[] welcomeLoreArray = welcomeLore.stream().map(helper::hex).toArray(String[]::new);
        ItemStack welcome = helper.createItem(Material.NETHER_STAR,
            helper.hex(plugin.getMessageManager().get("menu.welcome.title")),
            welcomeLoreArray
        );
        inv.setItem(13, welcome);
        
        List<String> startLore = plugin.getMessageManager().getList("menu.welcome.start-button.lore");
        String[] startLoreArray = startLore.stream().map(helper::hex).toArray(String[]::new);
        ItemStack start = helper.createItem(Material.LIME_CONCRETE,
            helper.hex(plugin.getMessageManager().get("menu.welcome.start-button.name")),
            startLoreArray
        );
        helper.addGlow(start);
        inv.setItem(31, start);
    }
    
    /**
     * Build normal menu with all quests
     */
    public void buildNormalMenu(Inventory inv, Player player, PlayerData data) {
        // Row 1: Quests 1, 2, 3 (slots 11, 13, 15)
        inv.setItem(11, createQuestItem(1, data, null));
        inv.setItem(13, createQuestItem(2, data, player));
        inv.setItem(15, createQuestItem(3, data, null));
        
        // Row 3: Quests 4, 5, 6 (slots 29, 31, 33)
        inv.setItem(29, createQuestItem(4, data, null));
        inv.setItem(31, createQuestItem(5, data, null));
        inv.setItem(33, createQuestItem(6, data, null));
    }
    
    /**
     * Create a quest item based on state
     */
    public ItemStack createQuestItem(int quest, PlayerData data, Player player) {
        QuestManager qm = plugin.getQuestManager();
        PlayerData.QuestProgress progress = data.getQuestProgress(quest);
        
        Material icon = qm.getQuestIcon(quest);
        String name = qm.getQuestName(quest);
        String desc = qm.getQuestDescription(quest);
        int reward = qm.getQuestRewardCoins(quest);
        int totalSteps = qm.getQuestSteps(quest);
        int currentStep = progress.getStep();
        
        List<String> lore = new ArrayList<>();
        lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        
        // Status line
        if (progress.isCompleted()) {
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-status.completed")));
        } else if (data.getCurrentQuest() == quest) {
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-status.in-progress")));
        } else if (data.getCurrentQuest() > quest) {
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-status.completed")));
        } else {
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-status.locked")));
        }
        
        lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        lore.add(" ");
        
        // Objective
        lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.objective")));
        lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.objective-format", "description", desc)));
        lore.add(" ");
        
        // Progress (if in progress)
        if (data.getCurrentQuest() == quest && !progress.isCompleted()) {
            if (quest == 2 && player != null) {
                int targetLevel = plugin.getConfigManager().getQuest2TargetLevel();
                int levelPercent = getLevelProgressPercent(player, data);
                lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.foraging-level", "target", String.valueOf(targetLevel))));
                lore.add(helper.createProgressBar(levelPercent, 100));
                lore.add(" ");
            } else {
                lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.progress", 
                    "current", String.valueOf(currentStep), "total", String.valueOf(totalSteps))));
                lore.add(helper.createProgressBar(currentStep, totalSteps));
                lore.add(" ");
            }
        }
        
        // Rewards
        lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.reward")));
        lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.reward-coins", "amount", String.valueOf(reward))));
        
        // Extra rewards for specific quests
        if (quest == 1) {
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.reward-apples")));
        } else if (quest == 6) {
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.reward-tokens")));
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.reward-items")));
        }
        
        lore.add(" ");
        lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        
        // Action hint
        if (progress.isCompleted()) {
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.quest-completed")));
        } else if (data.getCurrentQuest() == quest) {
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.click-details")));
        } else if (data.getCurrentQuest() > quest) {
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.quest-completed")));
        } else {
            lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.complete-previous")));
        }
        
        lore.add(helper.hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        
        // Build name with color based on state
        String displayName;
        if (progress.isCompleted() || data.getCurrentQuest() > quest) {
            displayName = helper.hex("&#55FF55" + name);
        } else if (data.getCurrentQuest() == quest) {
            displayName = helper.hex("&#FFFF55" + name);
        } else {
            displayName = helper.hex("&#777777" + name);
        }
        
        ItemStack item = helper.createItem(icon, displayName, lore.toArray(new String[0]));
        
        // Add glow if in progress or completed
        if (data.getCurrentQuest() == quest || progress.isCompleted()) {
            helper.addGlow(item);
        }
        
        // Special: Quest 6 gets double glow for blink effect
        if (quest == 6 && data.getCurrentQuest() == 6 && !progress.isCompleted()) {
            helper.addGlow(item);
        }
        
        return item;
    }
    
    /**
     * Add the navbar with quest-specific context
     */
    public void addNavbar(Inventory inv, Player player, PlayerData data) {
        Map<String, Object> context = new HashMap<>();
        context.put("menu_name", "Get Started Quests");
        context.put("menu_description", "Complete starter quests to learn about the server");
        context.put("page", 1);
        context.put("total_pages", 1);
        
        int completed = data.getCompletedQuestCount();
        context.put("completed_quests", completed);
        context.put("total_quests", 6);
        
        context.put("progress_bar", helper.createProgressBar(completed, 6));
        
        String currentQuestName = data.isCompleted() 
            ? plugin.getMessageManager().get("menu.progress.all-complete")
            : plugin.getQuestManager().getQuestName(data.getCurrentQuest());
        context.put("current_quest", currentQuestName);
        
        // Add currency info
        double coins = 0;
        double tokens = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        if (plugin.getAuraSkillsIntegration() != null && plugin.getAuraSkillsIntegration().isEnabled()) {
            try {
                org.bukkit.plugin.Plugin auraSkills = Bukkit.getPluginManager().getPlugin("AuraSkills");
                if (auraSkills != null) {
                    try {
                        Class<?> apiClass = Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
                        Object api = apiClass.getMethod("get").invoke(null);
                        Object economyProvider = apiClass.getMethod("getEconomyProvider").invoke(api);
                        Class<?> currencyTypeClass = Class.forName("dev.aurelium.auraskills.common.skillcoins.CurrencyType");
                        Object tokensEnum = currencyTypeClass.getField("TOKENS").get(null);
                        Object balance = economyProvider.getClass().getMethod("getBalance", java.util.UUID.class, currencyTypeClass)
                            .invoke(economyProvider, player.getUniqueId(), tokensEnum);
                        tokens = ((Number) balance).doubleValue();
                    } catch (Exception reflectionError) {
                        plugin.debug("Could not access AuraSkills economy via reflection: " + reflectionError.getMessage());
                        tokens = 0;
                    }
                }
            } catch (Exception e) {
                plugin.debug("Failed to get token balance: " + e.getMessage());
                tokens = 0;
            }
        }
        context.put("balance", coins);
        context.put("coins", String.format("%.0f", coins));
        context.put("tokens", String.format("%.0f", tokens));
        
        navbarManager.applyNavbar(inv, player, "main", context);
    }
    
    /**
     * Get quest number from slot
     */
    public int getQuestFromSlot(int slot) {
        return switch (slot) {
            case 11 -> 1;
            case 13 -> 2;
            case 15 -> 3;
            case 29 -> 4;
            case 31 -> 5;
            case 33 -> 6;
            default -> 0;
        };
    }
    
    /**
     * Get level progress percentage for Quest 2
     */
    private int getLevelProgressPercent(Player player, PlayerData data) {
        PlayerData.QuestProgress progress = data.getQuestProgress(2);
        if (progress.hasData("level_progress")) {
            Object val = progress.getData("level_progress");
            if (val instanceof Number) {
                return Math.min(100, ((Number) val).intValue());
            }
        }
        
        if (plugin.getAuraSkillsIntegration() != null) {
            try {
                String skill = plugin.getConfigManager().getQuest2Skill();
                int targetLevel = plugin.getConfigManager().getQuest2TargetLevel();
                int currentLevel = plugin.getAuraSkillsIntegration().getSkillLevel(player, skill);
                
                if (currentLevel >= targetLevel) {
                    return 100;
                }
                
                return (int) ((double) currentLevel / targetLevel * 100);
            } catch (Exception e) {
                // Fallback
            }
        }
        
        return 0;
    }
}
