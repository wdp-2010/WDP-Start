package com.wdp.start.ui.menu;

import com.wdp.start.WDPStartPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Global menu items - unified across all WDP plugins
 * Provides consistent close, back, and navigation functionality
 */
public class GlobalItems {

    private final WDPStartPlugin plugin;

    public GlobalItems(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a close button that closes the inventory
     */
    public ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get("items.close.name"));
        meta.setLore(plugin.getMessageManager().getList("items.close.lore"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a back button that returns to the previous menu
     * The previous menu name should be stored in the menu session
     */
    public ItemStack createBackItem(String menuName) {
        ItemStack item = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get("items.back.name"));
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(plugin.getMessageManager().get("items.back.lore", "menu", formatMenuName(menuName)));
        lore.add("");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a back button for returning to main menu
     */
    public ItemStack createBackToMainItem() {
        ItemStack item = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get("items.back-to-main.name"));
        meta.setLore(plugin.getMessageManager().getList("items.back-to-main.lore"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a previous page button
     */
    public ItemStack createPreviousPageItem(int currentPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get("items.previous-page.name"));
        meta.setLore(plugin.getMessageManager().getList("items.previous-page.lore", "page", String.valueOf(currentPage - 1)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a next page button
     */
    public ItemStack createNextPageItem(int currentPage, int totalPages) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get("items.next-page.name"));
        meta.setLore(plugin.getMessageManager().getList("items.next-page.lore", 
                "page", String.valueOf(currentPage + 1), 
                "total", String.valueOf(totalPages)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a page info item
     */
    public ItemStack createPageInfoItem(int currentPage, int totalPages) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get("items.page-info.name", 
                "current", String.valueOf(currentPage), 
                "total", String.valueOf(totalPages)));
        meta.setLore(plugin.getMessageManager().getList("items.page-info.lore", 
                "current", String.valueOf(currentPage), 
                "total", String.valueOf(totalPages)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates an info display item with custom text
     */
    public ItemStack createInfoItem(String title, List<String> loreLines) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        List<String> lore = new ArrayList<>();
        if (loreLines != null) {
            lore.addAll(loreLines);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a stats display item for WDP-Progress
     */
    public ItemStack createStatsItem(String playerName, double progress) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get("items.stats.name", "player", playerName));
        meta.setLore(plugin.getMessageManager().getList("items.stats.lore", "progress", String.format("%.1f", progress)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a quest info item for WDP-Quest/WDP-Start
     */
    public ItemStack createQuestInfoItem(String questName, int completed, int total) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get("items.quest-info.name"));
        meta.setLore(plugin.getMessageManager().getList("items.quest-info.lore", 
                "quest", questName, 
                "completed", String.valueOf(completed), 
                "total", String.valueOf(total)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a base info item for WDP-BaseDet
     */
    public ItemStack createBaseInfoItem(String baseName, int trustedCount) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get("items.base-info.name", "base", baseName));
        meta.setLore(plugin.getMessageManager().getList("items.base-info.lore", "trusted", String.valueOf(trustedCount)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a decorative glass pane
     */
    public ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a decorative gray glass pane
     */
    public ItemStack createGrayGlassPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a balance display item showing coins and tokens
     */
    public ItemStack createBalanceItem(double coins, double tokens) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get("items.balance-display.name"));
        meta.setLore(plugin.getMessageManager().getList("items.balance-display.lore", 
                "coins", String.format("%,.0f", coins), 
                "tokens", String.format("%,.0f", tokens)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Format menu name for display
     */
    private String formatMenuName(String menuName) {
        if (menuName == null) return "previous menu";
        
        // Replace underscores with spaces and capitalize
        String formatted = menuName.replace('_', ' ');
        StringBuilder result = new StringBuilder();
        boolean capitalize = true;
        
        for (char c : formatted.toCharArray()) {
            if (Character.isWhitespace(c)) {
                result.append(c);
                capitalize = true;
            } else if (capitalize) {
                result.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    /**
     * Fill an inventory with glass panes
     */
    public void fillInventory(org.bukkit.inventory.Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, createGlassPane());
            }
        }
    }

    /**
     * Fill a specific row with glass panes
     */
    public void fillRow(org.bukkit.inventory.Inventory inv, int row) {
        int start = row * 9;
        for (int i = start; i < start + 9; i++) {
            if (i < inv.getSize() && inv.getItem(i) == null) {
                inv.setItem(i, createGlassPane());
            }
        }
    }

    /**
     * Fill border of inventory (first and last row, first and last column)
     */
    public void fillBorder(org.bukkit.inventory.Inventory inv) {
        int size = inv.getSize();
        int rows = size / 9;
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                    int slot = row * 9 + col;
                    if (slot < size && inv.getItem(slot) == null) {
                        inv.setItem(slot, createGlassPane());
                    }
                }
            }
        }
    }
}