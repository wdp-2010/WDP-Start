package com.wdp.start.ui.shop;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.ui.menu.MenuUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

import static com.wdp.start.ui.menu.MenuUtils.*;

/**
 * Builds shop menus for Quest 3 (item purchase) and Quest 4 (token exchange).
 * Mirrors the SkillCoins shop style exactly.
 */
public class ShopMenuBuilder {
    
    private final WDPStartPlugin plugin;
    private final ShopDataLoader dataLoader;
    
    // Categories disabled in simplified shop (Quest 3)
    private static final Set<String> DISABLED_CATEGORIES = Set.of(
        "food", "redstone", "potion", "potions", "decoration", "decorations",
        "blocks", "block", "workshop", "workshops", "workstation", "workstations",
        "music", "musicdiscs", "music_discs"
    );
    
    public ShopMenuBuilder(WDPStartPlugin plugin, ShopDataLoader dataLoader) {
        this.plugin = plugin;
        this.dataLoader = dataLoader;
    }
    
    /**
     * Build the main shop menu
     * @param player The player
     * @param isTokenShop If true, build token shop (Quest 4), else item shop (Quest 3)
     */
    public Inventory buildMainShopMenu(Player player, boolean isTokenShop) {
        return isTokenShop ? buildTokenShop(player) : buildMainShop(player);
    }
    
    /**
     * Build a shop section menu
     */
    public Inventory buildShopSection(Player player, String category, Material icon) {
        return buildSectionMenu(player, category);
    }
    
    /**
     * Build transaction menu with quantity
     */
    public Inventory buildTransactionMenu(Player player, ShopItemData item, int quantity, boolean isBuying) {
        return buildTransactionMenu(player, item, quantity);
    }
    
    /**
     * Build the main shop menu (Quest 3 - buy items)
     */
    public Inventory buildMainShop(Player player) {
        String title = hex(plugin.getMessageManager().get("shop.main.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Fill with border
        fillInventory(inv);
        
        // Get player balance
        double coins = getPlayerCoins(player);
        
        // Player head (slot 0)
        inv.setItem(0, createPlayerHeadItem(player));
        
        // Balance display (slot 8)
        inv.setItem(8, createBalanceItem(coins));
        
        // Load sections from config
        for (ShopDataLoader.ShopSection section : dataLoader.getSections()) {
            if (!section.enabled()) continue;
            if (section.slot() < 0 || section.slot() >= 54) continue;
            
            String sectionId = section.id().toLowerCase();
            
            // Skip special sections
            if (sectionId.contains("skilllevels") || sectionId.contains("tokens")) continue;
            
            // Skip disabled categories for simplified shop
            boolean isDisabled = DISABLED_CATEGORIES.stream()
                .anyMatch(sectionId::contains);
            if (isDisabled) continue;
            
            int itemCount = dataLoader.getItemCount(section.displayName());
            if (itemCount == 0) {
                itemCount = dataLoader.getItems(section.displayName()).size();
            }
            
            inv.setItem(section.slot(), createCategoryItem(
                section.icon(),
                hex(section.displayName()),
                "Open " + stripColor(section.displayName()) + " shop",
                itemCount,
                true
            ));
        }
        
        // Close button (slot 53)
        inv.setItem(53, createCloseButton());
        
        return inv;
    }
    
    /**
     * Build the token exchange shop menu (Quest 4)
     */
    public Inventory buildTokenShop(Player player) {
        String title = hex(plugin.getMessageManager().get("shop.main.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Fill with border
        fillInventory(inv);
        
        // Get player balance
        double coins = getPlayerCoins(player);
        
        // Player head (slot 0)
        inv.setItem(0, createPlayerHeadItem(player));
        
        // Balance display (slot 8)
        inv.setItem(8, createBalanceItem(coins));
        
        // Load sections - all greyed out except token exchange
        for (ShopDataLoader.ShopSection section : dataLoader.getSections()) {
            if (!section.enabled()) continue;
            if (section.slot() < 0 || section.slot() >= 54) continue;
            
            // Skip special sections
            String sectionId = section.id().toLowerCase();
            if (sectionId.contains("skilllevels") || sectionId.contains("tokens")) continue;
            
            // Token exchange is clickable
            if (section.isTokenExchange()) {
                ItemStack tokenItem = createItem(Material.EMERALD,
                    hex(plugin.getMessageManager().get("shop.main.token-exchange.name")),
                    plugin.getMessageManager().getList("shop.main.token-exchange.lore")
                        .stream().map(MenuUtils::hex).toArray(String[]::new)
                );
                addGlow(tokenItem);
                inv.setItem(section.slot(), tokenItem);
                continue;
            }
            
            // Everything else is greyed out
            int itemCount = dataLoader.getItemCount(section.displayName());
            inv.setItem(section.slot(), createCategoryItem(
                section.icon(),
                ChatColor.of("#777777") + stripColor(section.displayName()),
                plugin.getMessageManager().get("shop.category.not-available"),
                Math.max(1, itemCount),
                false
            ));
        }
        
        // Close button
        inv.setItem(53, createCloseButton());
        
        return inv;
    }
    
    /**
     * Build a shop section menu (category items)
     */
    public Inventory buildSectionMenu(Player player, String category) {
        String title = ChatColor.of("#00FFFF") + category + " Shop";
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Get items for this category
        List<ShopItemData> items = dataLoader.getItems(category);
        
        int slot = 0;
        for (ShopItemData item : items) {
            if (slot >= 45) break; // Leave navbar row
            inv.setItem(slot, createShopItemDisplay(item));
            slot++;
        }
        
        // Fill remaining slots with glass
        for (int i = slot; i < 45; i++) {
            inv.setItem(i, createGlassPane());
        }
        
        return inv;
    }
    
    /**
     * Build the token exchange transaction menu
     */
    public Inventory buildTokenExchangeMenu(Player player) {
        String title = hex(plugin.getMessageManager().get("shop.token-exchange.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Fill with border
        fillInventory(inv);
        
        int tokenCost = plugin.getConfigManager().getQuest4TokenCost();
        int quantity = 1; // Only 1 token in tutorial
        int totalCost = tokenCost * quantity;
        
        double coinBalance = getPlayerCoins(player);
        boolean canAfford = coinBalance >= totalCost;
        
        // Token display (slot 13)
        ItemStack tokenDisplay = new ItemStack(Material.PAPER, 1);
        ItemMeta tokenMeta = tokenDisplay.getItemMeta();
        if (tokenMeta != null) {
            tokenMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.token-exchange.token-display.name")));
            List<String> lore = plugin.getMessageManager().getList("shop.token-exchange.token-display.lore",
                "quantity", String.valueOf(quantity),
                "cost", String.valueOf(tokenCost),
                "total", String.valueOf(totalCost));
            List<String> processedLore = new ArrayList<>(lore.stream().map(MenuUtils::hex).toList());
            processedLore.add(canAfford 
                ? hex(plugin.getMessageManager().get("shop.transaction.sufficient-funds"))
                : hex(plugin.getMessageManager().get("shop.transaction.insufficient-funds")));
            tokenMeta.setLore(processedLore);
            tokenDisplay.setItemMeta(tokenMeta);
        }
        inv.setItem(13, tokenDisplay);
        
        // Locked +1 button (slot 24)
        ItemStack plus1 = createItem(Material.GRAY_TERRACOTTA,
            hex("&#777777🔒 +1"),
            "", hex("&#555555Locked during tutorial"), hex("&#555555Max: 1 token")
        );
        inv.setItem(24, plus1);
        
        // Quantity display (slot 22)
        ItemStack qtyDisplay = createItem(Material.PAPER,
            hex(plugin.getMessageManager().get("shop.token-exchange.quantity-display.name", "quantity", String.valueOf(quantity))),
            plugin.getMessageManager().getList("shop.token-exchange.quantity-display.lore")
                .stream().map(MenuUtils::hex).toArray(String[]::new)
        );
        inv.setItem(22, qtyDisplay);
        
        // Confirm button (slot 49)
        ItemStack confirm = new ItemStack(canAfford ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            if (canAfford) {
                confirmMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.token-exchange.confirm.can-buy.name")));
                confirmMeta.setLore(plugin.getMessageManager().getList("shop.token-exchange.confirm.can-buy.lore",
                    "quantity", String.valueOf(quantity),
                    "total", String.valueOf(totalCost)).stream().map(MenuUtils::hex).toList());
            } else {
                confirmMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.token-exchange.confirm.cannot-buy.name")));
                confirmMeta.setLore(plugin.getMessageManager().getList("shop.token-exchange.confirm.cannot-buy.lore")
                    .stream().map(MenuUtils::hex).toList());
            }
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(49, confirm);
        
        // Balance display (slot 45)
        ItemStack balanceItem = createItem(Material.GOLD_INGOT,
            hex(plugin.getMessageManager().get("shop.token-exchange.balance.name")),
            plugin.getMessageManager().getList("shop.token-exchange.balance.lore",
                "coins", String.format("%.0f", coinBalance),
                "required", String.valueOf(totalCost)).stream().map(MenuUtils::hex).toArray(String[]::new)
        );
        inv.setItem(45, balanceItem);
        
        // Back button (slot 53)
        ItemStack back = createItem(Material.SPYGLASS,
            hex(plugin.getMessageManager().get("shop.token-exchange.back.name")),
            plugin.getMessageManager().getList("shop.token-exchange.back.lore")
                .stream().map(MenuUtils::hex).toArray(String[]::new)
        );
        inv.setItem(53, back);
        
        return inv;
    }
    
    /**
     * Build item transaction menu
     */
    public Inventory buildTransactionMenu(Player player, ShopItemData item, int quantity) {
        String title = hex(plugin.getMessageManager().get("shop.transaction.title-buy", "item", item.name()));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Fill with border
        fillInventory(inv);
        
        double coins = getPlayerCoins(player);
        double totalPrice = item.getTotalCost(quantity);
        boolean canAfford = coins >= totalPrice;
        
        // Item display (slot 13)
        ItemStack display = new ItemStack(item.material(), Math.min(quantity, 64));
        ItemMeta displayMeta = display.getItemMeta();
        if (displayMeta != null) {
            displayMeta.setDisplayName(ChatColor.of("#FFFFFF") + item.name());
            List<String> displayLore = new ArrayList<>();
            displayLore.add("");
            displayLore.add(hex(plugin.getMessageManager().get("shop.transaction.quantity", "quantity", String.valueOf(quantity))));
            displayLore.add(hex(plugin.getMessageManager().get("shop.transaction.price-per", "price", item.getFormattedBuyPrice())));
            displayLore.add("");
            displayLore.add(hex(plugin.getMessageManager().get("shop.transaction.total-cost", "total", String.format("%.0f", totalPrice))));
            displayLore.add("");
            displayLore.add(canAfford 
                ? hex(plugin.getMessageManager().get("shop.transaction.sufficient-funds"))
                : hex(plugin.getMessageManager().get("shop.transaction.insufficient-funds")));
            displayMeta.setLore(displayLore);
            display.setItemMeta(displayMeta);
        }
        inv.setItem(13, display);
        
        // +1 button (slot 24) - only if under max
        if (quantity < 3) {
            inv.setItem(24, createItem(Material.LIME_TERRACOTTA,
                hex(plugin.getMessageManager().get("shop.transaction.plus-one.name")),
                plugin.getMessageManager().getList("shop.transaction.plus-one.lore")
                    .stream().map(MenuUtils::hex).toArray(String[]::new)
            ));
        }
        
        // -1 button (slot 20) - only if above 1
        if (quantity > 1) {
            inv.setItem(20, createItem(Material.RED_TERRACOTTA,
                hex(plugin.getMessageManager().get("shop.transaction.minus-one.name")),
                plugin.getMessageManager().getList("shop.transaction.minus-one.lore")
                    .stream().map(MenuUtils::hex).toArray(String[]::new)
            ));
        }
        
        // Confirm button (slot 49)
        ItemStack confirm = new ItemStack(canAfford ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            if (canAfford) {
                confirmMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.transaction.confirm.can-buy.name")));
                confirmMeta.setLore(plugin.getMessageManager().getList("shop.transaction.confirm.can-buy.lore",
                    "quantity", String.valueOf(quantity),
                    "item", item.name(),
                    "total", String.format("%.0f", totalPrice)).stream().map(MenuUtils::hex).toList());
            } else {
                confirmMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.transaction.confirm.cannot-buy.name")));
                confirmMeta.setLore(plugin.getMessageManager().getList("shop.transaction.confirm.cannot-buy.lore")
                    .stream().map(MenuUtils::hex).toList());
            }
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(49, confirm);
        
        // Back button (slot 53)
        inv.setItem(53, createItem(Material.SPYGLASS,
            hex(plugin.getMessageManager().get("shop.transaction.back.name"))
        ));
        
        return inv;
    }
    
    // ==================== HELPER METHODS ====================
    
    private double getPlayerCoins(Player player) {
        if (plugin.getVaultIntegration() != null) {
            return plugin.getVaultIntegration().getBalance(player);
        }
        return 0;
    }
    
    private ItemStack createPlayerHeadItem(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.player-head.name",
                "player", player.getName())));
            skullMeta.setLore(plugin.getMessageManager().getList("shop.main.player-head.lore")
                .stream().map(MenuUtils::hex).toList());
            head.setItemMeta(skullMeta);
        }
        return head;
    }
    
    private ItemStack createBalanceItem(double coins) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.balance.name")));
            meta.setLore(plugin.getMessageManager().getList("shop.main.balance.lore",
                "coins", String.format("%.0f", coins)).stream().map(MenuUtils::hex).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCategoryItem(Material icon, String name, String description, int itemCount, boolean clickable) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(hex(plugin.getMessageManager().get("shop.category.description", "description", description)));
            lore.add("");
            lore.add(hex(plugin.getMessageManager().get("shop.category.items-count", "count", String.valueOf(itemCount))));
            lore.add("");
            lore.add(clickable 
                ? hex(plugin.getMessageManager().get("shop.category.click-to-open"))
                : hex(plugin.getMessageManager().get("shop.category.not-available")));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createShopItemDisplay(ShopItemData item) {
        ItemStack display = new ItemStack(item.material());
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FFFFFF") + item.name());
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(hex(plugin.getMessageManager().get("shop.item.buy", "price", item.getFormattedBuyPrice())));
            lore.add(hex(plugin.getMessageManager().get("shop.item.buy-action")));
            lore.add("");
            if (item.canSell()) {
                lore.add(hex(plugin.getMessageManager().get("shop.item.sell", "price", item.getFormattedSellPrice())));
                lore.add(hex(plugin.getMessageManager().get("shop.item.sell-action")));
            }
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }
    
    private ItemStack createCloseButton() {
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.close.name")));
            meta.setLore(plugin.getMessageManager().getList("shop.main.close.lore")
                .stream().map(MenuUtils::hex).toList());
            close.setItemMeta(meta);
        }
        return close;
    }
}
