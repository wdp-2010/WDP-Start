package com.wdp.start.ui.menu;

import com.wdp.start.WDPStartPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utility methods for all menu classes.
 * Provides item creation, hex color parsing, progress bars, and common UI elements.
 */
public final class MenuUtils {
    
    /** Common hex color pattern for &#RRGGBB format */
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    /** Menu identifier prefix for all WDP-Start menus */
    public static final String MENU_ID = "§8§l";
    
    private MenuUtils() {
        // Utility class - no instantiation
    }
    
    // ==================== ITEM CREATION ====================
    
    /**
     * Create an item with name and lore
     */
    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.setDisplayName(name);
        
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            if (line != null && !line.isEmpty()) {
                loreList.add(line);
            }
        }
        meta.setLore(loreList);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create an item with name and list lore
     */
    public static ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * Create a player head item
     */
    public static ItemStack createPlayerHead(Player player, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(displayName);
            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
        }
        
        return head;
    }
    
    /**
     * Create a glass pane filler item
     */
    public static ItemStack createGlassPane() {
        return createGlassPane(Material.BLACK_STAINED_GLASS_PANE);
    }
    
    /**
     * Create a glass pane filler item with specific material
     */
    public static ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }
    
    /**
     * Add enchantment glow to an item
     */
    public static void addGlow(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        
        item.addUnsafeEnchantment(Enchantment.MENDING, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
    }
    
    /**
     * Remove enchantment glow from an item
     */
    public static void removeGlow(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        item.removeEnchantment(Enchantment.MENDING);
    }
    
    // ==================== TEXT FORMATTING ====================
    
    /**
     * Convert &#RRGGBB hex codes to Bukkit ChatColor
     */
    public static String hex(String message) {
        if (message == null) return "";
        
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        
        while (matcher.find()) {
            String color = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + color).toString());
        }
        matcher.appendTail(buffer);
        
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    /**
     * Strip all color codes from a string
     */
    public static String stripColor(String input) {
        return ChatColor.stripColor(hex(input));
    }
    
    // ==================== PROGRESS BARS ====================
    
    /**
     * Create a simple 10-segment progress bar
     */
    public static String createProgressBar(int current, int total) {
        StringBuilder bar = new StringBuilder();
        bar.append(hex("&#55FF55"));
        
        int filled = total > 0 ? (int) ((double) current / total * 10) : 0;
        
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append(hex("&#555555")).append("█");
            }
        }
        
        return bar.toString();
    }
    
    /**
     * Create a percentage-based progress bar
     */
    public static String createProgressBar(double percentage) {
        return createProgressBar((int) percentage, 100);
    }
    
    // ==================== INVENTORY HELPERS ====================
    
    /**
     * Fill an inventory with glass panes
     */
    public static void fillInventory(Inventory inv) {
        fillInventory(inv, Material.BLACK_STAINED_GLASS_PANE);
    }
    
    /**
     * Fill an inventory with specified material
     */
    public static void fillInventory(Inventory inv, Material material) {
        ItemStack filler = createGlassPane(material);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }
    
    /**
     * Fill a specific row with glass panes
     */
    public static void fillRow(Inventory inv, int row) {
        fillRow(inv, row, Material.GRAY_STAINED_GLASS_PANE);
    }
    
    /**
     * Fill a specific row with material
     */
    public static void fillRow(Inventory inv, int row, Material material) {
        int startSlot = row * 9;
        ItemStack filler = createGlassPane(material);
        for (int i = 0; i < 9; i++) {
            inv.setItem(startSlot + i, filler);
        }
    }
    
    /**
     * Create an inventory with title prefix
     */
    public static Inventory createMenu(int size, String title) {
        return Bukkit.createInventory(null, size, MENU_ID + hex(title));
    }
    
    /**
     * Check if an inventory is a WDP-Start menu
     */
    public static boolean isWDPMenu(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        String title = inv.getViewers().get(0).getOpenInventory().getTitle();
        return title != null && title.startsWith(MENU_ID);
    }
    
    // ==================== MATERIAL HELPERS ====================
    
    /**
     * Convert material name to pretty display name
     */
    public static String prettifyMaterialName(Material material) {
        if (material == null) return "Unknown";
        
        String name = material.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Safely get material from string, with fallback
     */
    public static Material getMaterial(String name, Material fallback) {
        if (name == null || name.isEmpty()) return fallback;
        
        Material mat = Material.matchMaterial(name.toUpperCase());
        return mat != null ? mat : fallback;
    }
}
