package com.wdp.start.ui;

import com.wdp.start.WDPStartPlugin;
import net.md_5.bungee.api.ChatColor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles universal navbar creation and management
 * Extracted from QuestMenu for cleaner separation of concerns
 */
public class NavbarManager {
    
    private final WDPStartPlugin plugin;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public NavbarManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Apply universal navbar from navbar.yml configuration
     */
    public void applyNavbar(Inventory inv, Player player, String menuType, Map<String, Object> context) {
        File navbarFile = new File(plugin.getDataFolder(), "navbar.yml");
        FileConfiguration config;
        if (navbarFile.exists()) {
            config = YamlConfiguration.loadConfiguration(navbarFile);
        } else {
            return;
        }
        
        if (!config.contains("navbar")) {
            return;
        }

        for (String itemName : config.getConfigurationSection("navbar").getKeys(false)) {
            // Skip quest_progress (slot 49) for shop menus - confirm button needs that slot
            if ("quest_progress".equals(itemName) && menuType != null && menuType.startsWith("shop")) {
                continue;
            }
            Map<String, Object> itemConfig = config.getConfigurationSection("navbar." + itemName).getValues(false);
            
            // Handle glass_fill with multiple slots
            if ("glass_fill".equals(itemName)) {
                @SuppressWarnings("unchecked")
                List<Integer> slots = (List<Integer>) itemConfig.get("slots");
                if (slots != null) {
                    ItemStack glassItem = createNavbarItem(itemName, itemConfig, player, context);
                    if (glassItem != null) {
                        for (Integer slot : slots) {
                            if (slot >= 45 && slot <= 53) {
                                inv.setItem(slot, glassItem);
                            }
                        }
                    }
                }
                continue;
            }
            
            Integer slot = (Integer) itemConfig.get("slot");
            if (slot == null || slot < 45 || slot > 53) continue;
            
            // Handle special cases
            if ("back".equals(itemName)) {
                if (context == null || !context.containsKey("previous_menu")) {
                    continue;
                }
            }
            
            if ("close".equals(itemName)) {
                if (context != null && context.containsKey("previous_menu")) {
                    continue;
                }
            }
            
            if ("previous_page".equals(itemName) || "next_page".equals(itemName)) {
                if (context == null || !context.containsKey("page") || !context.containsKey("total_pages")) {
                    inv.setItem(slot, createGlassPane());
                    continue;
                }
                
                int page = (Integer) context.get("page");
                int totalPages = (Integer) context.get("total_pages");
                
                if ("previous_page".equals(itemName) && page <= 1) {
                    inv.setItem(slot, createGlassPane());
                    continue;
                }
                
                if ("next_page".equals(itemName) && page >= totalPages) {
                    inv.setItem(slot, createGlassPane());
                    continue;
                }
            }
            
            if ("page_info".equals(itemName)) {
                if (context == null || !context.containsKey("page") || !context.containsKey("total_pages")) {
                    inv.setItem(slot, createGlassPane());
                    continue;
                }
            }
            
            if ("quest_progress".equals(itemName)) {
                if (context == null || !context.containsKey("completed_quests") || !context.containsKey("total_quests")) {
                    inv.setItem(slot, createGlassPane());
                    continue;
                }
            }
            
            // Handle balance display with tokens from AuraSkills
            if ("balance".equals(itemName)) {
                ItemStack balanceItem = createBalanceItem(player, context);
                if (balanceItem != null) {
                    inv.setItem(slot, balanceItem);
                }
                continue;
            }
            
            ItemStack item = createNavbarItem(itemName, itemConfig, player, context);
            if (item != null) {
                inv.setItem(slot, item);
            }
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
     * Replace placeholders in text
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
     * Create a glass pane item
     */
    public ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Fill a row with material
     */
    public void fillRow(Inventory inv, int row, Material material) {
        int startSlot = row * 9;
        for (int i = 0; i < 9; i++) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                item.setItemMeta(meta);
            }
            inv.setItem(startSlot + i, item);
        }
    }
    
    /**
     * Create the balance display item with coins and tokens
     */
    private ItemStack createBalanceItem(Player player, Map<String, Object> context) {
        // Get coins balance
        double coins = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        // Get tokens from AuraSkills
        double tokens = 0;
        if (plugin.getAuraSkillsIntegration() != null && plugin.getAuraSkillsIntegration().isEnabled()) {
            try {
                org.bukkit.plugin.Plugin auraSkillsPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("AuraSkills");
                if (auraSkillsPlugin != null) {
                    // Use public API method
                    try {
                        java.lang.reflect.Method getTokensMethod = auraSkillsPlugin.getClass().getMethod("getPlayerTokens", java.util.UUID.class);
                        Object balance = getTokensMethod.invoke(auraSkillsPlugin, player.getUniqueId());
                        tokens = ((Number) balance).doubleValue();
                    } catch (Exception reflectionError) {
                        plugin.debug("Could not access AuraSkills tokens: " + reflectionError.getMessage());
                    }
                }
            } catch (Exception e) {
                plugin.debug("Failed to get token balance: " + e.getMessage());
            }
        }
        
        // Create the item
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(hex("§6Balance:"));
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(hex("§eSkillCoins: §6" + String.format("%,.0f", coins) + " ⛃"));
            lore.add(hex("§aTokens: §2" + String.format("%,.0f", tokens) + " 🎟"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private String hex(String message) {
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
}
