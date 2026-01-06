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
 * Handles simple shop menu interactions only.
 */
public class ShopMenuListener implements Listener {

    private final WDPStartPlugin plugin;

    public ShopMenuListener(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();

        if (plugin.getSimpleShopMenu() == null || !plugin.getSimpleShopMenu().isShopInventory(title)) {
            return;
        }

        event.setCancelled(true);

        Inventory inv = event.getInventory();
        if (event.getRawSlot() < 0 || event.getRawSlot() >= inv.getSize()) {
            return;
        }

        plugin.getSimpleShopMenu().handleClick(player, event.getRawSlot(), inv);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();

        if (plugin.getSimpleShopMenu() != null && plugin.getSimpleShopMenu().isShopInventory(title)) {
            plugin.getSimpleShopMenu().handleClose(player);
        }
    }
}