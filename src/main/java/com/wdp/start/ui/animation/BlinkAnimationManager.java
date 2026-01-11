package com.wdp.start.ui.animation;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import com.wdp.start.ui.menu.MenuUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static com.wdp.start.ui.menu.MenuUtils.*;

/**
 * Manages blinking animations for quest menu items.
 * 
 * Pattern: I . I . I . . . . . . I . I . I . . . . . .
 * Where I = no glow, . = glowing
 * 
 * Used for Quest 5 and Quest 6 when they are the current active quest.
 */
public class BlinkAnimationManager {
    
    private final WDPStartPlugin plugin;
    
    // Function to create quest items (injected from QuestMenu)
    private BiFunction<Integer, PlayerData, ItemStack> questItemCreator;
    
    // Track active blinking tasks per player
    private final Map<UUID, Integer> blinkingTasks = new ConcurrentHashMap<>();
    
    // The blinking pattern (true = glow, false = no glow)
    private static final boolean[] BLINK_PATTERN = {
        false, true, false, true, false, true, // 3 blinks
        true, true, true, true, true,          // pause
        false, true, false, true, false, true, // 3 blinks
        true, true, true, true                 // pause
    };
    
    public BlinkAnimationManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Set the quest item creator function (called from QuestMenu initialization)
     */
    public void setQuestItemCreator(BiFunction<Integer, PlayerData, ItemStack> creator) {
        this.questItemCreator = creator;
    }
    
    /**
     * Start blinking animation for a specific slot
     * 
     * @param player The player viewing the menu
     * @param slot The inventory slot to animate
     */
    public void startAnimation(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        
        // Cancel any existing animation first
        stopAnimation(player);
        
        // Ensure we have a quest item creator
        if (questItemCreator == null) {
            plugin.debug("BlinkAnimationManager: No quest item creator set, skipping animation");
            return;
        }
        
        // Create new animation task
        int taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int tick = 0;
            
            @Override
            public void run() {
                try {
                    // Check if player is still online
                    if (!player.isOnline()) {
                        stopAnimation(player);
                        return;
                    }
                    
                    // Check if menu is still open
                    Inventory topInv = player.getOpenInventory().getTopInventory();
                    if (!isWDPMenu(topInv)) {
                        stopAnimation(player);
                        return;
                    }
                    
                    // Get current player data
                    PlayerData data = plugin.getPlayerDataManager().getData(player);
                    
                    // Determine quest from slot
                    int quest = slot == 31 ? 5 : 6;
                    
                    // Check if quest is still active
                    if (data.getCurrentQuest() != quest || data.isQuestCompleted(quest)) {
                        stopAnimation(player);
                        return;
                    }
                    
                    // Get current pattern state
                    boolean shouldGlow = BLINK_PATTERN[tick % BLINK_PATTERN.length];
                    
                    // Create the quest item using the injected creator
                    ItemStack item = questItemCreator.apply(quest, data);
                    
                    // Apply glow based on pattern
                    if (shouldGlow) {
                        addGlow(item);
                    }
                    
                    // Update the item in inventory
                    topInv.setItem(slot, item);
                    
                    // Force client refresh
                    player.updateInventory();
                    
                    tick++;
                    
                } catch (Exception e) {
                    plugin.debug("Blinking animation error: " + e.getMessage());
                    stopAnimation(player);
                }
            }
        }, 0L, 3L).getTaskId(); // Update every 3 ticks (0.15 seconds)
        
        blinkingTasks.put(uuid, taskId);
        plugin.debug("Started blink animation for " + player.getName() + " at slot " + slot);
    }
    
    /**
     * Stop blinking animation for a player
     */
    public void stopAnimation(Player player) {
        stopAnimation(player.getUniqueId());
    }
    
    /**
     * Stop blinking animation by UUID
     */
    public void stopAnimation(UUID uuid) {
        Integer taskId = blinkingTasks.remove(uuid);
        if (taskId != null) {
            try {
                plugin.getServer().getScheduler().cancelTask(taskId);
            } catch (Exception e) {
                // Task may already be cancelled
            }
        }
    }
    
    /**
     * Stop all animations (for plugin disable)
     */
    public void stopAll() {
        for (UUID uuid : blinkingTasks.keySet()) {
            stopAnimation(uuid);
        }
        blinkingTasks.clear();
    }
    
    /**
     * Check if player has an active animation
     */
    public boolean hasActiveAnimation(Player player) {
        return blinkingTasks.containsKey(player.getUniqueId());
    }
    
    /**
     * Get the slot for a quest's blink animation
     */
    public static int getSlotForQuest(int quest) {
        return switch (quest) {
            case 5 -> 31;
            case 6 -> 33;
            default -> -1;
        };
    }
}
