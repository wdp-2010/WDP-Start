package com.wdp.start.ui.menu;

import com.wdp.start.WDPStartPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.wdp.start.ui.menu.MenuUtils.*;

/**
 * Handles the universal navbar that appears at the bottom of all menus.
 * Loaded from navbar.yml configuration file.
 * 
 * Navbar occupies row 5 (slots 45-53) in all 54-slot inventories.
 */
public class NavbarRenderer {
    
    private final WDPStartPlugin plugin;
    private FileConfiguration navbarConfig;
    
    public NavbarRenderer(WDPStartPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Load navbar configuration from navbar.yml
     */
    public void loadConfig() {
        File navbarFile = new File(plugin.getDataFolder(), "navbar.yml");
        if (navbarFile.exists()) {
            navbarConfig = YamlConfiguration.loadConfiguration(navbarFile);
        } else {
            navbarConfig = null;
            plugin.debug("navbar.yml not found - navbar will not be rendered");
        }
    }
    
    /**
     * Reload navbar configuration
     */
    public void reload() {
        loadConfig();
    }
    
    /**
     * Apply the universal navbar to an inventory
     * 
     * @param inv The inventory to apply navbar to
     * @param player The player viewing the menu
     * @param menuType The type of menu being displayed
     * @param context Context data for placeholder replacement
     */
    public void apply(Inventory inv, Player player, String menuType, Map<String, Object> context) {
        if (navbarConfig == null || !navbarConfig.contains("navbar")) {
            // No config - fill with glass panes as fallback
            for (int i = 45; i <= 53; i++) {
                inv.setItem(i, createGlassPane());
            }
            return;
        }
        
        // Process each navbar item from config
        for (String itemName : navbarConfig.getConfigurationSection("navbar").getKeys(false)) {
            Map<String, Object> itemConfig = navbarConfig.getConfigurationSection("navbar." + itemName).getValues(false);
            
            // Handle glass_fill with multiple slots
            if ("glass_fill".equals(itemName)) {
                applyGlassFill(inv, itemConfig, player, context);
                continue;
            }
            
            // Get slot
            Integer slot = (Integer) itemConfig.get("slot");
            if (slot == null || slot < 45 || slot > 53) continue;
            
            // Handle conditional items
            if (!shouldShowItem(itemName, context)) {
                inv.setItem(slot, createGlassPane());
                continue;
            }
            
            // Create and place item
            ItemStack item = createNavbarItem(itemName, itemConfig, player, context);
            if (item != null) {
                inv.setItem(slot, item);
            }
        }
    }
    
    /**
     * Apply glass fill items to multiple slots
     */
    private void applyGlassFill(Inventory inv, Map<String, Object> config, Player player, Map<String, Object> context) {
        @SuppressWarnings("unchecked")
        List<Integer> slots = (List<Integer>) config.get("slots");
        if (slots == null) return;
        
        ItemStack glassItem = createNavbarItem("glass_fill", config, player, context);
        if (glassItem != null) {
            for (Integer slot : slots) {
                if (slot >= 45 && slot <= 53) {
                    inv.setItem(slot, glassItem);
                }
            }
        }
    }
    
    /**
     * Check if an item should be shown based on context
     */
    private boolean shouldShowItem(String itemName, Map<String, Object> context) {
        if (context == null) return true;
        
        switch (itemName) {
            case "back":
                // Only show back button if there's a previous menu
                return context.containsKey("previous_menu");
                
            case "close":
                // Only show close button if there's NO previous menu
                return !context.containsKey("previous_menu");
                
            case "previous_page":
                if (!context.containsKey("page")) return false;
                int page = (Integer) context.get("page");
                return page > 1;
                
            case "next_page":
                if (!context.containsKey("page") || !context.containsKey("total_pages")) return false;
                int currentPage = (Integer) context.get("page");
                int totalPages = (Integer) context.get("total_pages");
                return currentPage < totalPages;
                
            case "page_info":
                return context.containsKey("page") && context.containsKey("total_pages");
                
            case "quest_progress":
                return context.containsKey("completed_quests") && context.containsKey("total_quests");
                
            default:
                return true;
        }
    }
    
    /**
     * Create a navbar item from configuration
     */
    private ItemStack createNavbarItem(String itemName, Map<String, Object> config, Player player, Map<String, Object> context) {
        String materialStr = (String) config.get("material");
        String displayName = (String) config.get("display_name");
        
        @SuppressWarnings("unchecked")
        List<String> lore = (List<String>) config.get("lore");
        
        if (materialStr == null) return null;
        
        Material material = Material.getMaterial(materialStr.toUpperCase());
        if (material == null) return null;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        if (displayName != null) {
            meta.setDisplayName(hex(replacePlaceholders(displayName, context)));
        }
        
        if (lore != null) {
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(hex(replacePlaceholders(line, context)));
            }
            meta.setLore(processedLore);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Replace placeholders in text with context values
     */
    private String replacePlaceholders(String text, Map<String, Object> context) {
        if (text == null || context == null) return text;
        
        String result = text;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
    
    /**
     * Check if a slot is part of the navbar
     */
    public static boolean isNavbarSlot(int slot) {
        return slot >= 45 && slot <= 53;
    }
    
    /**
     * Get the back button slot (usually 45)
     */
    public int getBackButtonSlot() {
        if (navbarConfig == null || !navbarConfig.contains("navbar.back.slot")) {
            return 45; // Default
        }
        return navbarConfig.getInt("navbar.back.slot", 45);
    }
    
    /**
     * Get the close button slot (usually 53)
     */
    public int getCloseButtonSlot() {
        if (navbarConfig == null || !navbarConfig.contains("navbar.close.slot")) {
            return 53; // Default
        }
        return navbarConfig.getInt("navbar.close.slot", 53);
    }
}
