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
 * Handles quest menu interactions only.
 */
public class QuestMenuListener implements Listener {

    private final WDPStartPlugin plugin;

    public QuestMenuListener(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory inv = event.getInventory();

        // Only handle quest menus
        if (!plugin.getQuestMenuCoordinator().isQuestMenu(inv)) {
            return;
        }

        event.setCancelled(true);

        // Ignore clicks outside the inventory
        if (event.getRawSlot() < 0 || event.getRawSlot() >= inv.getSize()) {
            return;
        }

        plugin.getQuestMenuCoordinator().handleClick(player, event.getRawSlot(), inv);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Only handle quest menus
        if (plugin.getQuestMenuCoordinator().isQuestMenu(event.getInventory())) {
            plugin.getQuestMenuCoordinator().handleClose(player);
        }
    }
}