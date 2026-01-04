package com.wdp.start.ui;

import com.wdp.start.WDPStartPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for menu item creation and manipulation
 * Extracted from QuestMenu for reusability
 */
public class MenuHelper {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    private final WDPStartPlugin plugin;
    
    public MenuHelper(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Create an ItemStack with name and lore
     */
    public ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
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
     * Add enchantment glow to an item
     */
    public void addGlow(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.MENDING, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }
    
    /**
     * Create a progress bar string
     */
    public String createProgressBar(int current, int total) {
        StringBuilder bar = new StringBuilder();
        bar.append(hex("&#55FF55"));
        
        int filled = (int) ((double) current / total * 10);
        
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append(hex("&#555555") + "█");
            }
        }
        
        return bar.toString();
    }
    
    /**
     * Create a glass pane border item
     */
    public ItemStack createBorder() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        return border;
    }
    
    /**
     * Fill inventory with border
     */
    public void fillWithBorder(Inventory inv) {
        ItemStack border = createBorder();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, border);
        }
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
     * Create WDP-Quest style progress segment with Custom Model Data
     */
    public ItemStack createProgressSegment(int segmentIndex, double completion, boolean isHard) {
        final int SEGMENTS = 8;
        final int FILLS_PER_SEGMENT = 5;
        final int TOTAL_UNITS = SEGMENTS * FILLS_PER_SEGMENT;
        final int CMD_NORMAL = 1000;
        final int CMD_HARD = 1010;
        
        int totalFilledUnits = (int) Math.round(completion / 100.0 * TOTAL_UNITS);
        int unitsBeforeThis = segmentIndex * FILLS_PER_SEGMENT;
        int unitsInThisSegment = Math.max(0, Math.min(FILLS_PER_SEGMENT, totalFilledUnits - unitsBeforeThis));
        
        int cmdBase = isHard ? CMD_HARD : CMD_NORMAL;
        
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setCustomModelData(cmdBase + unitsInThisSegment);
            
            String color = isHard ? "§c" : "§a";
            String segmentBar = createSegmentVisual(unitsInThisSegment, isHard);
            
            meta.setDisplayName(segmentBar);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Segment " + (segmentIndex + 1) + "/8");
            lore.add("§7Fill: " + color + unitsInThisSegment + "/5");
            lore.add("");
            lore.add("§7Overall: §f" + String.format("%.0f", completion) + "%");
            
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a visual representation of a single segment
     */
    public String createSegmentVisual(int fillLevel, boolean isHard) {
        String filled = isHard ? "§c█" : "§a█";
        String empty = "§7░";
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            bar.append(i < fillLevel ? filled : empty);
        }
        return bar.toString();
    }
    
    /**
     * Convert hex color codes to Bukkit format
     */
    public String hex(String message) {
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
    
    public WDPStartPlugin getPlugin() {
        return plugin;
    }
}
