package com.wdp.start.api;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Public API for other plugins to interact with WDP-Start
 * 
 * CRITICAL INTEGRATION GUIDELINES:
 * =================================
 * 
 * For /shop command plugins (SkillCoins, Economy plugins):
 * --------------------------------------------------------
 * BEFORE opening your normal shop menu, check:
 *   if (WDPStartAPI.shouldShowSimplifiedShop(player)) {
 *       // DO NOT OPEN YOUR MENU - WDP-Start will handle it
 *       return;
 *   }
 * 
 * When a token is purchased:
 *   WDPStartAPI.notifyTokenPurchase(player, amount);
 * 
 * When coins are spent:
 *   WDPStartAPI.trackCoinSpending(player, amount);
 * 
 * For /quests or /progress command plugins:
 * -----------------------------------------
 * BEFORE opening your menu, check:
 *   if (WDPStartAPI.shouldShowSimplifiedQuestMenu(player)) {
 *       // DO NOT OPEN YOUR MENU - WDP-Start will handle it
 *       return;
 *   }
 * 
 * When player clicks Statistics in WDP-Progress:
 *   WDPStartAPI.notifyProgressStatsClick(player);
 * 
 * DEFAULT BEHAVIOR:
 * -----------------
 * If WDP-Start is NOT loaded or API returns false, ALWAYS open your normal menu.
 * The simplified view is ONLY shown when explicitly requested by WDP-Start.
 * 
 * Example Implementation:
 * ----------------------
 * public void onShopCommand(Player player) {
 *     // Check if WDP-Start wants simplified view
 *     if (WDPStartAPI.isAvailable() && WDPStartAPI.shouldShowSimplifiedShop(player)) {
 *         // Don't open - WDP-Start handles it
 *         return;
 *     }
 *     
 *     // Default: Open normal shop menu
 *     openNormalShopMenu(player);
 * }
 */
public class WDPStartAPI {
    
    private static WDPStartPlugin plugin;
    
    /**
     * Initialize the API (called internally)
     */
    public static void init(WDPStartPlugin instance) {
        plugin = instance;
    }
    
    /**
     * Get the plugin instance
     */
    public static WDPStartPlugin getPlugin() {
        return plugin;
    }
    
    /**
     * Check if the API is available
     */
    public static boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }
    
    // ==================== PLAYER DATA ====================
    
    /**
     * Check if a player has started the quest chain
     */
    public static boolean hasStartedQuests(Player player) {
        if (!isAvailable()) return false;
        return plugin.getPlayerDataManager().getData(player).isStarted();
    }
    
    /**
     * Check if a player has completed all quests
     */
    public static boolean hasCompletedAllQuests(Player player) {
        if (!isAvailable()) return false;
        return plugin.getPlayerDataManager().getData(player).isCompleted();
    }
    
    /**
     * Get the current quest number (1-6, or 0 if not started)
     */
    public static int getCurrentQuest(Player player) {
        if (!isAvailable()) return 0;
        return plugin.getPlayerDataManager().getData(player).getCurrentQuest();
    }
    
    /**
     * Check if a specific quest is completed
     */
    public static boolean isQuestCompleted(Player player, int quest) {
        if (!isAvailable()) return false;
        return plugin.getPlayerDataManager().getData(player).isQuestCompleted(quest);
    }
    
    // ==================== QUEST TRIGGERS ====================
    
    /**
     * Notify that a player clicked the Statistics category in WDP-Progress
     * Used for Quest 3 completion
     */
    public static void notifyProgressStatsClick(Player player) {
        if (!isAvailable()) return;
        
        // Use the stored listener reference
        if (plugin.getQuestListener() != null) {
            plugin.getQuestListener().onProgressStatsClick(player);
        }
    }
    
    /**
     * Notify that a player purchased SkillTokens
     * Used for Quest 4 completion
     */
    public static void notifyTokenPurchase(Player player, int amount) {
        if (!isAvailable()) return;
        
        if (plugin.getQuestListener() != null) {
            plugin.getQuestListener().onTokenPurchase(player, amount);
        }
    }
    
    /**
     * Track coin spending for refund calculation
     * Call this when a player spends SkillCoins in the shop
     */
    public static void trackCoinSpending(Player player, int amount) {
        if (!isAvailable()) return;
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Only track if within the spending window
        long spendWindow = plugin.getConfigManager().getSpendTrackingSeconds() * 1000L;
        if (System.currentTimeMillis() - data.getLastCoinGrantTime() < spendWindow) {
            data.addCoinsSpent(amount);
            plugin.debug("Tracked " + amount + " coins spent by " + player.getName());
        }
    }
    
    // ==================== MENU CONTROL ====================
    
    /**
     * Check if a player is currently on Quest 4 (Token purchase)
     * Used to trigger simplified shop menu
     */
    public static boolean shouldShowSimplifiedShop(Player player) {
        if (!isAvailable()) return false;
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        return data.isStarted() && 
               data.getCurrentQuest() == 4 && 
               !data.isQuestCompleted(4);
    }
    
    /**
     * Check if a player is currently on Quest 3 (Shop - buy items)
     * Used to trigger simplified shop menu with items (NOT token exchange)
     */
    public static boolean shouldShowSimplifiedShopItems(Player player) {
        if (!isAvailable()) return false;
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        return data.isStarted() && 
               data.getCurrentQuest() == 3 && 
               !data.isQuestCompleted(3);
    }
    
    /**
     * Check if a player is currently on Quest 5 (Menu orientation)
     * Used to trigger simplified quest menu
     */
    public static boolean shouldShowSimplifiedQuestMenu(Player player) {
        if (!isAvailable()) return false;
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        return data.isStarted() && 
               data.getCurrentQuest() == 5 && 
               !data.isQuestCompleted(5);
    }
    
    /**
     * Check if a player is currently on Quest 2 (Foraging level)
     * Used to suppress level up messages from AuraSkills/SkillCoins
     * When true, the calling plugin should NOT show level up titles/sounds
     */
    public static boolean shouldSuppressLevelUpMessages(Player player) {
        if (!isAvailable()) {
            plugin.getLogger().info("[DEBUG] WDP-Start API: Not available");
            return false;
        }
        
        try {
            PlayerData data = plugin.getPlayerDataManager().getData(player);
            boolean transientSuppress = data.isSuppressLevelUpActive();
            if (transientSuppress) {
                plugin.getLogger().info("[DEBUG] WDP-Start API: Transient suppression active for " + player.getName());
            }
            boolean shouldSuppress = transientSuppress || (data.isStarted() && 
                                    data.getCurrentQuest() == 2 && 
                                    !data.isQuestCompleted(2));
            
            plugin.getLogger().info("[DEBUG] WDP-Start API: Level up check for " + player.getName() + " at " + System.currentTimeMillis());
            plugin.getLogger().info("  - Started: " + data.isStarted());
            plugin.getLogger().info("  - Current Quest: " + data.getCurrentQuest());
            plugin.getLogger().info("  - Quest 2 Completed: " + data.isQuestCompleted(2));
            plugin.getLogger().info("  - Transient: " + transientSuppress);
            plugin.getLogger().info("  - Should Suppress: " + shouldSuppress);
            
            if (shouldSuppress) {
                plugin.getLogger().info("[DEBUG] WDP-Start API: Suppressing level up messages for " + player.getName());
            }
            
            return shouldSuppress;
        } catch (Exception e) {
            plugin.getLogger().warning("[DEBUG] WDP-Start API: Error checking player data for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Open the simplified shop menu for Quest 4
     * Call this when the player uses /shop and shouldShowSimplifiedShop() returns true
     */
    public static void openSimplifiedShop(Player player) {
        if (!isAvailable()) return;
        plugin.getQuestMenu().openSimplifiedShop(player);
    }
    
    /**
     * Open the simplified shop menu for Quest 3 (items only, no token exchange)
     * Call this when the player uses /shop and shouldShowSimplifiedShopItems() returns true
     */
    public static void openSimplifiedShopItems(Player player) {
        if (!isAvailable()) return;
        plugin.getQuestMenu().openSimplifiedShopItems(player);
    }
    
    /**
     * Open the simplified quest menu for Quest 5
     * Call this when the player uses /quests and shouldShowSimplifiedQuestMenu() returns true
     */
    public static void openSimplifiedQuestMenu(Player player) {
        if (!isAvailable()) return;
        plugin.getQuestMenu().openSimplifiedQuestView(player);
    }
    
    // ==================== ADMIN ====================
    
    /**
     * Force complete a quest for a player
     */
    public static void forceCompleteQuest(Player player, int quest) {
        if (!isAvailable()) return;
        if (quest < 1 || quest > 6) return;
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (!data.isStarted()) {
            data.setStarted(true);
            data.setCurrentQuest(1);
        }
        
        // Only complete if they're on this quest
        if (data.getCurrentQuest() == quest) {
            plugin.getQuestManager().completeQuest(player, quest);
        }
    }
    
    /**
     * Reset a player's quest progress
     */
    public static void resetPlayer(Player player) {
        if (!isAvailable()) return;
        plugin.getPlayerDataManager().getData(player).reset();
    }
    
    /**
     * Reset a player's quest progress by UUID
     */
    public static void resetPlayer(UUID uuid) {
        if (!isAvailable()) return;
        plugin.getPlayerDataManager().deleteData(uuid);
    }
}
