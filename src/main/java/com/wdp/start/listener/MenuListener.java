package com.wdp.start.listener;

import com.wdp.start.WDPStartPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/**
 * Handles GUI click events for the quest menu
 */
public class MenuListener implements Listener {
    
    private final WDPStartPlugin plugin;
    
    public MenuListener(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        Inventory inv = event.getInventory();
        
        // Check if it's our menu
        if (!plugin.getQuestMenu().isQuestMenu(inv)) {
            return;
        }
        
        // Cancel all clicks in our menu
        event.setCancelled(true);
        
        // Ignore clicks outside the inventory
        if (event.getRawSlot() < 0 || event.getRawSlot() >= inv.getSize()) {
            return;
        }
        
        // Handle the click
        plugin.getQuestMenu().handleClick(player, event.getRawSlot(), inv);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        // Check if it was our menu
        if (plugin.getQuestMenu().isQuestMenu(event.getInventory())) {
            plugin.getQuestMenu().handleClose(player);
        }
    }
}
