package com.wdp.start.ui.shop;

import org.bukkit.Material;

/**
 * Represents a shop item with buy/sell prices.
 * Immutable data class for shop item information.
 */
public record ShopItemData(
    Material material,
    String name,
    double buyPrice,
    double sellPrice,
    boolean canSell
) {
    
    /**
     * Create a buy-only item
     */
    public static ShopItemData buyOnly(Material material, String name, double buyPrice) {
        return new ShopItemData(material, name, buyPrice, 0, false);
    }
    
    /**
     * Create item that can be bought and sold
     */
    public static ShopItemData buyAndSell(Material material, String name, double buyPrice, double sellPrice) {
        return new ShopItemData(material, name, buyPrice, sellPrice, true);
    }
    
    /**
     * Get formatted buy price string
     */
    public String getFormattedBuyPrice() {
        return String.format("%.0f", buyPrice);
    }
    
    /**
     * Get formatted sell price string
     */
    public String getFormattedSellPrice() {
        return String.format("%.0f", sellPrice);
    }
    
    /**
     * Check if player can afford this item at a given quantity
     */
    public boolean canAfford(double balance, int quantity) {
        return balance >= (buyPrice * quantity);
    }
    
    /**
     * Get total cost for a quantity
     */
    public double getTotalCost(int quantity) {
        return buyPrice * quantity;
    }
    
    /**
     * Get total sell value for a quantity
     */
    public double getTotalSellValue(int quantity) {
        return sellPrice * quantity;
    }
}
