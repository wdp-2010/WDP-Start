package com.wdp.start.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single purchasable item in the shop
 */
public class ShopItem {
    
    public enum ItemType {
        REGULAR,        // Regular item purchase
        TOKEN_EXCHANGE  // Exchange coins for tokens
    }
    
    private final Material material;
    private final String displayName;
    private final double buyPrice;
    private final Map<Enchantment, Integer> enchantments;
    private final ItemType type;
    private final int tokenAmount;  // For TOKEN_EXCHANGE type
    
    /**
     * Create a regular shop item
     */
    public ShopItem(Material material, String displayName, double buyPrice) {
        this(material, displayName, buyPrice, new HashMap<>(), ItemType.REGULAR, 0);
    }
    
    /**
     * Create a shop item with enchantments
     */
    public ShopItem(Material material, String displayName, double buyPrice, Map<Enchantment, Integer> enchantments) {
        this(material, displayName, buyPrice, enchantments, ItemType.REGULAR, 0);
    }
    
    /**
     * Create a token exchange item
     */
    public static ShopItem tokenExchange(double coinCost, int tokenAmount) {
        return new ShopItem(Material.EMERALD, "Skill Token", coinCost, new HashMap<>(), ItemType.TOKEN_EXCHANGE, tokenAmount);
    }
    
    /**
     * Full constructor
     */
    public ShopItem(Material material, String displayName, double buyPrice, 
                   Map<Enchantment, Integer> enchantments, ItemType type, int tokenAmount) {
        this.material = material;
        this.displayName = displayName;
        this.buyPrice = buyPrice;
        this.enchantments = new HashMap<>(enchantments);
        this.type = type;
        this.tokenAmount = tokenAmount;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getBuyPrice() {
        return buyPrice;
    }
    
    public Map<Enchantment, Integer> getEnchantments() {
        return new HashMap<>(enchantments);
    }
    
    public boolean hasEnchantments() {
        return !enchantments.isEmpty();
    }
    
    public ItemType getType() {
        return type;
    }
    
    public int getTokenAmount() {
        return tokenAmount;
    }
    
    /**
     * Check if buying is enabled
     */
    public boolean canBuy() {
        return buyPrice >= 0;
    }
    
    /**
     * Create an ItemStack for display or purchase
     */
    public ItemStack createItemStack(int amount) {
        ItemStack item = new ItemStack(material, amount);
        
        if (!enchantments.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
                item.setItemMeta(meta);
            }
        }
        
        return item;
    }
    
    /**
     * Get a pretty name for display (title case)
     */
    public String getPrettyName() {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        
        // Convert material name to pretty format
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}
