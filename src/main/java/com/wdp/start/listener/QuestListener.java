package com.wdp.start.listener;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks quest-related events and completes quest objectives
 */
public class QuestListener implements Listener {
    
    private final WDPStartPlugin plugin;
    
    // Track players who have entered the spawn exit region
    private final Set<UUID> enteredSpawnExit = new HashSet<>();
    
    // Track players who have opened certain menus
    private final Set<UUID> openedProgress = new HashSet<>();
    private final Set<UUID> openedShop = new HashSet<>();
    
    public QuestListener(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    // ==================== QUEST 1: LEAVE & TELEPORT ====================
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Skip if only head rotation
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Only track for Quest 1
        if (!data.isStarted() || data.getCurrentQuest() != 1) {
            return;
        }
        
        // Check if already completed step 1
        PlayerData.QuestProgress progress = data.getQuestProgress(1);
        if (progress.hasData("entered_region") && (boolean) progress.getData("entered_region")) {
            return;
        }
        
        // Use PortalZoneManager to check zone entry
        plugin.getPortalZoneManager().onPlayerMove(player, event.getTo());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Only track for Quest 1
        if (!data.isStarted() || data.getCurrentQuest() != 1) {
            return;
        }
        
        // Check if step 1 is complete (entered region)
        PlayerData.QuestProgress progress = data.getQuestProgress(1);
        boolean enteredRegion = progress.hasData("entered_region") && (boolean) progress.getData("entered_region");
        boolean enteredZone = progress.hasData("entered_zone") && (boolean) progress.getData("entered_zone");
        
        if (!enteredRegion && !enteredZone) {
            return;
        }
        
        // Check if already completed step 2
        if (progress.hasData("teleported") && (boolean) progress.getData("teleported")) {
            return;
        }
        
        // Console debug message
        plugin.getLogger().info("[DEBUG] [QuestListener] " + player.getName() + 
            " TELEPORTED from " + formatLocation(event.getFrom()) + " to " + formatLocation(event.getTo()));
        plugin.getLogger().info("[DEBUG] [QuestListener] Teleport cause: " + event.getCause().name());
        
        // Notify portal zone manager
        plugin.getPortalZoneManager().onPlayerTeleport(player, event.getFrom(), event.getTo());
        
        // Teleport detected after region entry - complete Quest 1
        plugin.getQuestManager().completeStep(player, 1, 2, "teleported");
        
        // Small delay before completing quest (for dramatic effect)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getQuestManager().completeQuest(player, 1);
        }, 20L);
        
        enteredSpawnExit.remove(player.getUniqueId());
        
        plugin.getLogger().info("[DEBUG] [QuestListener] " + player.getName() + " completed Quest 1 - Teleport detected!");
        plugin.debug("Player " + player.getName() + " was teleported - Quest 1 complete!");
    }
    
    /**
     * Format location for debug output
     */
    private String formatLocation(Location loc) {
        if (loc == null) return "null";
        return String.format("(%.1f, %.1f, %.1f) in %s", 
            loc.getX(), loc.getY(), loc.getZ(), 
            loc.getWorld() != null ? loc.getWorld().getName() : "null");
    }
    
    // ==================== QUEST 3, 4, 5: COMMAND TRACKING ====================
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (!data.isStarted()) return;
        
        String command = event.getMessage().toLowerCase();
        
        // Quest 3: /shop command - INTERCEPT and show simplified shop
        if (data.getCurrentQuest() == 3 && !data.isQuestCompleted(3)) {
            if (command.startsWith("/shop")) {
                event.setCancelled(true); // Cancel so the shop plugin doesn't open
                
                PlayerData.QuestProgress progress = data.getQuestProgress(3);
                
                if (!progress.hasData("opened_shop")) {
                    plugin.getQuestManager().completeStep(player, 3, 1, "opened_shop");
                    openedShop.add(player.getUniqueId());
                    
                    // Auto top-up if needed for shop purchase
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        plugin.getQuestManager().autoTopUpForShop(player);
                    }, 5L);
                }
                
                // Open simplified shop menu (items only, no token exchange)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getQuestMenu().openSimplifiedShopItems(player);
                });
                
                return;
            }
        }
        
        // Quest 4: /shop command - INTERCEPT for Token Exchange
        if (data.getCurrentQuest() == 4 && !data.isQuestCompleted(4)) {
            if (command.startsWith("/shop")) {
                event.setCancelled(true); // Cancel so the shop plugin doesn't open
                
                PlayerData.QuestProgress progress = data.getQuestProgress(4);
                
                if (!progress.hasData("opened_shop")) {
                    plugin.getQuestManager().completeStep(player, 4, 1, "opened_shop");
                    
                    // Auto top-up if needed
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        plugin.getQuestManager().autoTopUpIfNeeded(player);
                    }, 5L);
                }
                
                // Open simplified token exchange menu
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getQuestMenu().openSimplifiedShop(player);
                });
                
                return;
            }
        }
        
        // Quest 5: ONLY /quest or /quests command - INTERCEPT and show WDP-Quest style menu
        // /start still opens normal menu
        if (data.getCurrentQuest() == 5 && !data.isQuestCompleted(5)) {
            if (command.startsWith("/quest ") || command.equals("/quest") ||
                command.startsWith("/quests ") || command.equals("/quests")) {
                event.setCancelled(true); // Cancel to prevent normal menu
                
                // Open WDP-Quest style simplified menu
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getQuestMenu().openSimplifiedQuestView(player);
                });
                
                return;
            }
            // /start, /getstarted, /wdpstart still open normal menu (no interception)
        }
    }
    
    /**
     * Manually trigger shop item purchase for Quest 3
     * Called from menu when player buys an item
     */
    public void onShopItemPurchase(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (!data.isStarted() || data.getCurrentQuest() != 3) return;
        if (data.isQuestCompleted(3)) return;
        
        PlayerData.QuestProgress progress = data.getQuestProgress(3);
        if (!progress.hasData("opened_shop")) return; // Must open shop first
        
        if (!progress.hasData("bought_item")) {
            plugin.getQuestManager().completeStep(player, 3, 2, "bought_item");
            
            // Complete quest
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getQuestManager().completeQuest(player, 3);
            }, 10L);
        }
    }
    
    /**
     * Manually trigger step completion for Quest 3 (stats click)
     * Called from external integration
     */
    public void onProgressStatsClick(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (!data.isStarted() || data.getCurrentQuest() != 3) return;
        if (data.isQuestCompleted(3)) return;
        
        PlayerData.QuestProgress progress = data.getQuestProgress(3);
        if (!progress.hasData("opened_progress")) return; // Must open menu first
        
        if (!progress.hasData("viewed_stats")) {
            plugin.getQuestManager().completeStep(player, 3, 2, "viewed_stats");
            
            // Complete quest
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getQuestManager().completeQuest(player, 3);
            }, 10L);
        }
    }
    
    /**
     * Manually trigger token purchase for Quest 4
     * Called from external integration
     */
    public void onTokenPurchase(Player player, int amount) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (!data.isStarted() || data.getCurrentQuest() != 4) return;
        if (data.isQuestCompleted(4)) return;
        
        PlayerData.QuestProgress progress = data.getQuestProgress(4);
        if (!progress.hasData("opened_shop")) return; // Must open shop first
        
        int required = plugin.getConfigManager().getQuest4TokensRequired();
        int current = progress.hasData("tokens_purchased") 
            ? (int) progress.getData("tokens_purchased") 
            : 0;
        
        current += amount;
        progress.setData("tokens_purchased", current);
        
        if (current >= required) {
            plugin.getQuestManager().completeStep(player, 4, 2, "bought_token");
            
            // Complete quest
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getQuestManager().completeQuest(player, 4);
            }, 10L);
        }
    }
    
    /**
     * Check if player has entered spawn exit (for Quest 1)
     */
    public boolean hasEnteredSpawnExit(UUID uuid) {
        return enteredSpawnExit.contains(uuid);
    }
    
    /**
     * Clean up player tracking on logout
     */
    public void cleanupPlayer(UUID uuid) {
        enteredSpawnExit.remove(uuid);
        openedProgress.remove(uuid);
        openedShop.remove(uuid);
        
        // Also clean up portal zone manager
        plugin.getPortalZoneManager().cleanupPlayer(uuid);
    }
    
    // ==================== QUEST 5: MINE STONE ====================
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Only track for Quest 5
        if (!data.isStarted() || data.getCurrentQuest() != 5 || data.isQuestCompleted(5)) {
            return;
        }
        
        Material blockType = event.getBlock().getType();
        
        // Check if stone type (all variants)
        boolean isStone = blockType == Material.STONE ||
                         blockType == Material.COBBLESTONE ||
                         blockType == Material.DEEPSLATE ||
                         blockType == Material.COBBLED_DEEPSLATE ||
                         blockType == Material.ANDESITE ||
                         blockType == Material.DIORITE ||
                         blockType == Material.GRANITE ||
                         blockType == Material.SMOOTH_STONE ||
                         blockType == Material.STONE_BRICKS ||
                         blockType == Material.INFESTED_STONE;
        
        if (!isStone) {
            return;
        }
        
        PlayerData.QuestProgress progress = data.getQuestProgress(5);
        int currentCount = progress.getCounter("stone_mined", 0);
        
        if (currentCount >= 5) {
            return; // Already completed objective
        }
        
        // Increment counter
        progress.incrementCounter("stone_mined", 1);
        int newCount = currentCount + 1;
        
        // Send progress message
        player.sendMessage(ChatColor.of("#55FF55") + "⛏ Stone Mined: " + 
                         ChatColor.of("#FFFFFF") + newCount + "/5");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        
        // Complete when reached 5
        if (newCount >= 5) {
            player.sendMessage("");
            player.sendMessage(ChatColor.of("#FFD700") + "§l✓ Objective Complete!");
            player.sendMessage(ChatColor.of("#FFFFFF") + "Type " + ChatColor.of("#55FF55") + "/quest" + 
                             ChatColor.of("#FFFFFF") + " to complete Quest 5!");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }
}
