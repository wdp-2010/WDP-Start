package com.wdp.start.ui.animation;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import com.wdp.start.ui.menu.QuestItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.wdp.start.ui.menu.MenuUtils.addGlow;

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
    private final QuestItemBuilder questItemBuilder;
    
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
        this.questItemBuilder = new QuestItemBuilder(plugin);
    }
    
    /**
     * Start blinking animation for a specific slot
     * 
     * @param player The player viewing the menu
     * @param slot The inventory slot to animate
     * @param quest The quest number (5 or 6)
     * @param data Player data for item creation
     */
    public void startBlinking(Player player, int slot, int quest, PlayerData data) {
        UUID uuid = player.getUniqueId();
        
        // Cancel any existing animation first
        stopBlinking(uuid);
        
        // Create new animation task
        int taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int tick = 0;
            
            @Override
            public void run() {
                try {
                    // Check if player is still online
                    if (!player.isOnline()) {
                        stopBlinking(uuid);
                        return;
                    }
                    
                    // Check if menu is still open
                    Inventory topInv = player.getOpenInventory().getTopInventory();
                    if (topInv == null || !isWDPMenu(topInv)) {
                        stopBlinking(uuid);
                        return;
                    }
                    
                    // Get fresh player data
                    PlayerData currentData = plugin.getPlayerDataManager().getData(player);
                    
                    // Check if quest is still active
                    if (currentData.getCurrentQuest() != quest || currentData.isQuestCompleted(quest)) {
                        stopBlinking(uuid);
                        return;
                    }
                    
                    // Get current pattern state
                    boolean shouldGlow = BLINK_PATTERN[tick % BLINK_PATTERN.length];
                    
                    if (shouldGlow) {
                        // Create the quest item and add glow
                        ItemStack item = questItemBuilder.build(quest, currentData, null);
                        addGlow(item);
                        topInv.setItem(slot, item);
                    } else {
                        // Create a ghost item (gray pane) that retains the original's name/lore
                        ItemStack original = questItemBuilder.build(quest, currentData, null);
                        org.bukkit.inventory.meta.ItemMeta origMeta = original.getItemMeta();

                        org.bukkit.inventory.ItemStack ghost = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
                        org.bukkit.inventory.meta.ItemMeta ghostMeta = ghost.getItemMeta();
                        if (ghostMeta != null) {
                            if (origMeta != null) {
                                if (origMeta.hasDisplayName()) ghostMeta.setDisplayName(origMeta.getDisplayName());
                                if (origMeta.hasLore()) ghostMeta.setLore(origMeta.getLore());
                            }
                            ghostMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                            ghost.setItemMeta(ghostMeta);
                        }
                        topInv.setItem(slot, ghost);
                    }
                    
                    // Force client refresh
                    player.updateInventory();
                    
                    tick++;
                    
                } catch (Exception e) {
                    plugin.debug("Blinking animation error: " + e.getMessage());
                    stopBlinking(uuid);
                }
            }
        }, 0L, 3L).getTaskId(); // Update every 3 ticks (0.15 seconds)
        
        blinkingTasks.put(uuid, taskId);
        plugin.debug("Started blink animation for " + player.getName() + " at slot " + slot);
    }
    
    /**
     * Check if inventory is a WDP menu
     */
    private boolean isWDPMenu(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        String title = inv.getViewers().get(0).getOpenInventory().getTitle();
        return title != null && title.startsWith("§8§l");
    }
    
    /**
     * Stop blinking animation for a player
     */
    public void stopBlinking(Player player) {
        stopBlinking(player.getUniqueId());
    }
    
    /**
     * Stop blinking animation by UUID
     */
    public void stopBlinking(UUID uuid) {
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
            stopBlinking(uuid);
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
