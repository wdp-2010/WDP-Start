package com.wdp.start.listener;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player join/quit events and welcome messages
 */
public class PlayerListener implements Listener {
    
    private final WDPStartPlugin plugin;
    
    public PlayerListener(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player data
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Check if should show welcome message
        if (shouldShowWelcome(player, data)) {
            // Delayed welcome message
            int delay = plugin.getMessageManager().getWelcomeDelayTicks();
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    sendWelcomeMessage(player);
                }
            }, delay);
        }
        
        // If player is on Quest 1 and it's not completed, restart particle path
        if (data.isStarted() && data.getCurrentQuest() == 1 && !data.isQuestCompleted(1)) {
            if (plugin.getPathGuideManager() != null) {
                plugin.debug("Restarting particle path guide for " + player.getName() + " on rejoin");
                // Delay slightly to ensure player is fully loaded
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getPathGuideManager().startPath(player);
                    }
                }, 20L); // 1 second delay
            }
        }
        
        plugin.debug("Player " + player.getName() + " joined. Quest started: " + data.isStarted());
        
        // Update boss bar if player has active quests
        plugin.getBossBarManager().updateBossBar(player);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Hide boss bar
        plugin.getBossBarManager().hideBossBar(player);
        
        // Save and unload player data
        plugin.getPlayerDataManager().unloadData(player.getUniqueId());
        
        plugin.debug("Player " + player.getName() + " quit. Data saved and unloaded.");
    }
    
    /**
     * Check if we should show the welcome message
     */
    private boolean shouldShowWelcome(Player player, PlayerData data) {
        // Don't show if disabled
        if (!plugin.getConfigManager().isWelcomeMessage()) {
            return false;
        }
        
        // Don't show if message is disabled in messages.yml
        if (!plugin.getMessageManager().isWelcomeEnabled()) {
            return false;
        }
        
        // Don't show if already started or completed quests
        if (data.isStarted() || data.isCompleted()) {
            return false;
        }
        
        // Show to new players or players who haven't started
        return true;
    }
    
    /**
     * Send the welcome message to a player
     */
    private void sendWelcomeMessage(Player player) {
        plugin.getMessageManager().sendList(player, "welcome.messages");
    }
}
