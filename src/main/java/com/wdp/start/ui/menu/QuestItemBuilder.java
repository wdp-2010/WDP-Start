package com.wdp.start.ui.menu;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import com.wdp.start.quest.QuestManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

import static com.wdp.start.ui.menu.MenuUtils.*;

/**
 * Builds individual quest items for the quest menu.
 * Handles quest state (locked/active/completed) and progress display.
 */
public class QuestItemBuilder {
    
    private final WDPStartPlugin plugin;
    
    public QuestItemBuilder(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Create a quest item based on state
     * 
     * @param quest The quest number (1-6)
     * @param data The player's data
     * @return The built ItemStack
     */
    public ItemStack build(int quest, PlayerData data) {
        return build(quest, data, null);
    }
    
    /**
     * Create a quest item based on state with optional player for level progress
     * 
     * @param quest The quest number (1-6)
     * @param data The player's data
     * @param player Optional player for level progress display
     * @return The built ItemStack
     */
    public ItemStack build(int quest, PlayerData data, Player player) {
        QuestManager qm = plugin.getQuestManager();
        PlayerData.QuestProgress progress = data.getQuestProgress(quest);
        
        Material icon = qm.getQuestIcon(quest);
        String name = qm.getQuestName(quest);
        String desc = qm.getQuestDescription(quest);
        int reward = qm.getQuestRewardCoins(quest);
        int totalSteps = qm.getQuestSteps(quest);
        int currentStep = progress.getStep();
        
        List<String> lore = new ArrayList<>();
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        
        // Status line
        if (progress.isCompleted()) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-status.completed")));
        } else if (data.getCurrentQuest() == quest) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-status.in-progress")));
        } else if (data.getCurrentQuest() > quest) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-status.completed")));
        } else {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-status.locked")));
        }
        
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        lore.add(" ");
        
        // Objective
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.objective")));
        // Support multiline descriptions
        for (String descLine : desc.split("\n")) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.objective-format", "description", descLine)));
        }
        lore.add(" ");
        
        // Progress (if in progress)
        if (data.getCurrentQuest() == quest && !progress.isCompleted()) {
            // Quest 2: Show level percentage instead of step progress
            if (quest == 2 && player != null) {
                int targetLevel = plugin.getConfigManager().getQuest2TargetLevel();
                int levelPercent = getLevelProgressPercent(player, data);
                lore.add(hex(plugin.getMessageManager().get("menu.quest-item.foraging-level", "target", String.valueOf(targetLevel))));
                lore.add(createProgressBar(levelPercent, 100));
                lore.add(" ");
            } else {
                lore.add(hex(plugin.getMessageManager().get("menu.quest-item.progress", 
                    "current", String.valueOf(currentStep), "total", String.valueOf(totalSteps))));
                lore.add(createProgressBar(currentStep, totalSteps));
                lore.add(" ");
            }
        }
        
        // Rewards
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.reward")));
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.reward-coins", "amount", String.valueOf(reward))));
        
        // Extra rewards for specific quests
        if (quest == 1) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.reward-apples")));
        } else if (quest == 6) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.reward-tokens")));
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.reward-items")));
        }
        
        lore.add(" ");
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        
        // Action hint
        if (progress.isCompleted()) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.quest-completed")));
        } else if (data.getCurrentQuest() == quest) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.click-details")));
        } else if (data.getCurrentQuest() > quest) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.quest-completed")));
        } else {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.complete-previous")));
        }
        
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        
        // Build name with color based on state
        String displayName;
        if (progress.isCompleted() || data.getCurrentQuest() > quest) {
            displayName = hex("&#55FF55" + name);
        } else if (data.getCurrentQuest() == quest) {
            displayName = hex("&#FFFF55" + name);
        } else {
            displayName = hex("&#777777" + name);
        }
        
        ItemStack item = createItem(icon, displayName, lore.toArray(new String[0]));
        
        // Add glow if in progress or completed
        if (data.getCurrentQuest() == quest || progress.isCompleted()) {
            addGlow(item);
        }
        
        return item;
    }
    
    /**
     * Get level progress percentage for Quest 2
     */
    private int getLevelProgressPercent(Player player, PlayerData data) {
        // Check if we have stored level progress
        PlayerData.QuestProgress progress = data.getQuestProgress(2);
        if (progress.hasData("level_progress")) {
            Object val = progress.getData("level_progress");
            if (val instanceof Number) {
                return Math.min(100, ((Number) val).intValue());
            }
        }
        
        // Try to get from AuraSkills
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
    
    /**
     * Get the slot for a quest number
     */
    public static int getSlotForQuest(int quest) {
        return switch (quest) {
            case 1 -> 11;  // Row 1, left
            case 2 -> 13;  // Row 1, center
            case 3 -> 15;  // Row 1, right
            case 4 -> 29;  // Row 3, left
            case 5 -> 31;  // Row 3, center
            case 6 -> 33;  // Row 3, right
            default -> -1;
        };
    }
    
    /**
     * Get the quest number from a slot
     */
    public static int getQuestFromSlot(int slot) {
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
}
