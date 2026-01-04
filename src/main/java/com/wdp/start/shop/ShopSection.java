package com.wdp.start.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a shop section/category (Food, Tools, Resources, etc.)
 */
public class ShopSection {
    
    private final String id;
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final boolean enabled;
    private final List<ShopItem> items;
    
    // Section type flags
    private final boolean tokenExchange;
    private final boolean skillLevels;
    
    public ShopSection(String id, String displayName, Material icon, int slot, boolean enabled) {
        this(id, displayName, icon, slot, enabled, false, false);
    }
    
    public ShopSection(String id, String displayName, Material icon, int slot, boolean enabled,
                       boolean tokenExchange, boolean skillLevels) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.slot = slot;
        this.enabled = enabled;
        this.tokenExchange = tokenExchange;
        this.skillLevels = skillLevels;
        this.items = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public int getSlot() {
        return slot;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isTokenExchange() {
        return tokenExchange;
    }
    
    public boolean isSkillLevels() {
        return skillLevels;
    }
    
    public List<ShopItem> getItems() {
        return new ArrayList<>(items);
    }
    
    public void addItem(ShopItem item) {
        items.add(item);
    }
    
    public void setItems(List<ShopItem> newItems) {
        items.clear();
        items.addAll(newItems);
    }
    
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * Check if this section should be available during a specific quest
     * @param currentQuest The player's current quest number
     * @return true if this section is accessible
     */
    public boolean isAvailableForQuest(int currentQuest) {
        // Quest 3: All regular sections available, token exchange disabled
        // Quest 4: Only token exchange available
        
        if (currentQuest == 3) {
            // Regular items only, no token exchange or skill levels
            return !tokenExchange && !skillLevels;
        } else if (currentQuest == 4) {
            // Only token exchange available
            return tokenExchange;
        }
        
        // For completed players, everything is available via normal shop
        return true;
    }
    
    /**
     * Get the display color based on section type
     */
    public String getDisplayColor() {
        String lower = id.toLowerCase();
        
        if (lower.contains("combat")) return "#FF5555";
        if (lower.contains("enchant")) return "#FF55FF";
        if (lower.contains("resource")) return "#55FF55";
        if (lower.contains("tool")) return "#5555FF";
        if (lower.contains("food")) return "#FFFF00";
        if (lower.contains("block")) return "#FFD700";
        if (lower.contains("farm")) return "#55FF55";
        if (lower.contains("potion")) return "#FF55FF";
        if (lower.contains("redstone")) return "#FF5555";
        if (lower.contains("skill") || lower.contains("level")) return "#FFD700";
        if (lower.contains("token") || lower.contains("exchange")) return "#00FFFF";
        if (lower.contains("misc")) return "#808080";
        if (lower.contains("decoration")) return "#FF69B4";
        if (lower.contains("dye")) return "#DA70D6";
        if (lower.contains("music")) return "#9370DB";
        if (lower.contains("ore")) return "#CD853F";
        if (lower.contains("spawn")) return "#90EE90";
        if (lower.contains("workstation")) return "#DEB887";
        
        return "#00FFFF"; // Default cyan
    }
    
    /**
     * Get an icon character for the section
     */
    public String getIconChar() {
        String lower = id.toLowerCase();
        
        if (lower.contains("combat")) return "⚔ ";
        if (lower.contains("enchant")) return "✦ ";
        if (lower.contains("resource")) return "❖ ";
        if (lower.contains("tool")) return "⚒ ";
        if (lower.contains("food")) return "🍖 ";
        if (lower.contains("block")) return "⬛ ";
        if (lower.contains("farm")) return "🌾 ";
        if (lower.contains("potion")) return "⚗ ";
        if (lower.contains("redstone")) return "🔴 ";
        if (lower.contains("skill") || lower.contains("level")) return "★ ";
        if (lower.contains("token") || lower.contains("exchange")) return "🎟 ";
        if (lower.contains("misc")) return "⋯ ";
        if (lower.contains("decoration")) return "✿ ";
        if (lower.contains("dye")) return "◉ ";
        if (lower.contains("music")) return "♪ ";
        if (lower.contains("ore")) return "◆ ";
        if (lower.contains("spawn")) return "⊛ ";
        if (lower.contains("workstation")) return "⌂ ";
        
        return "● ";
    }
}
