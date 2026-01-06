package com.wdp.start.shop;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import com.wdp.start.ui.NavbarManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple Shop Menu for WDP-Start Quest 3 and 4
 * Modeled after SkillCoins /shop command layout
 * 
 * Features:
 * - Main menu showing all categories
 * - Section menus with pagination (45 items per page)
 * - Quest-aware: disables categories based on current quest
 * - Token exchange for Quest 4
 * - Buy-only transactions (no sell)
 */
public class SimpleShopMenu {
    
    private final WDPStartPlugin plugin;
    private final ShopLoader shopLoader;
    private final NavbarManager navbarManager;
    
    // Menu tracking
    private final ConcurrentHashMap<UUID, MenuSession> openMenus = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> playerPages = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TransactionData> transactionData = new ConcurrentHashMap<>();
    
    // Constants
    private static final String MENU_ID = "§8§l"; // Hidden identifier
    private static final int ITEMS_PER_PAGE = 45; // 5 rows of 9
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public SimpleShopMenu(WDPStartPlugin plugin) {
        this.plugin = plugin;
        this.shopLoader = new ShopLoader(plugin);
        this.navbarManager = new NavbarManager(plugin);
    }
    
    /**
     * Initialize the shop - load all data
     */
    public void initialize() {
        shopLoader.load();
    }
    
    /**
     * Reload shop data
     */
    public void reload() {
        shopLoader.reload();
    }
    
    // ==================== MAIN SHOP MENU ====================
    
    /**
     * Open the main shop menu for a player
     * @param player The player
     * @param questNumber The player's current quest (3 or 4)
     */
    public void openMainMenu(Player player, int questNumber) {
        String title = hex(plugin.getMessageManager().get("shop.main.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Fill with gray glass pane (SkillCoins style)
        fillBackground(inv, Material.GRAY_STAINED_GLASS_PANE);
        
        // Player head (slot 0)
        addPlayerHead(inv, player);
        
        // Balance display (slot 8)
        addBalanceDisplay(inv, player);
        
        // Add shop sections
        addShopSections(inv, player, questNumber);
        
        // Close button (slot 53)
        addCloseButton(inv);
        
        // Open and track
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession("main", questNumber, null, 0));
        
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
    }
    
    /**
     * Add player head to slot 0
     */
    private void addPlayerHead(Inventory inv, Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.player-head.name",
                    "player", player.getName())));
            List<String> lore = plugin.getMessageManager().getList("shop.main.player-head.lore");
            skullMeta.setLore(lore.stream().map(this::hex).toList());
            head.setItemMeta(skullMeta);
        }
        
        inv.setItem(0, head);
    }
    
    /**
     * Add balance display to slot 8
     */
    private void addBalanceDisplay(Inventory inv, Player player) {
        double coins = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        ItemStack balance = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = balance.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.balance.name")));
            List<String> lore = plugin.getMessageManager().getList("shop.main.balance.lore",
                    "coins", MONEY_FORMAT.format(coins));
            meta.setLore(lore.stream().map(this::hex).toList());
            balance.setItemMeta(meta);
        }
        
        inv.setItem(8, balance);
    }
    
    /**
     * Add shop sections to the menu
     */
    private void addShopSections(Inventory inv, Player player, int questNumber) {
        List<ShopSection> allSections = shopLoader.getSections();
        
        for (ShopSection section : allSections) {
            int slot = section.getSlot();
            if (slot < 0 || slot >= 54) continue;
            
            // Skip if disabled in simplified shop (food, redstone, etc.) - don't show at all
            if (section.isDisabledInSimplifiedShop()) {
                continue;
            }
            
            boolean available = section.isAvailableForQuest(questNumber);
            
            ItemStack item = new ItemStack(section.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Format display name with color and icon
                String color = section.getDisplayColor();
                String icon = section.getIconChar();
                String cleanName = ChatColor.stripColor(hex(section.getDisplayName()));
                
                if (available) {
                    meta.setDisplayName(ChatColor.of(color) + icon + ChatColor.of("#FFFFFF") + cleanName);
                } else {
                    meta.setDisplayName(ChatColor.of("#555555") + "🔒 " + cleanName);
                }
                
                List<String> lore = new ArrayList<>();
                lore.add("");
                
                if (available) {
                    lore.add(hex(plugin.getMessageManager().get("shop.category.items-count",
                            "count", String.valueOf(section.getItemCount()))));
                    lore.add("");
                    lore.add(hex(plugin.getMessageManager().get("shop.category.click-to-open")));
                } else {
                    lore.add(hex(plugin.getMessageManager().get("shop.category.not-available")));
                }
                
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            
            inv.setItem(slot, item);
        }
    }
    
    // ==================== SECTION MENU WITH PAGINATION ====================
    
    /**
     * Open a shop section menu
     */
    public void openSectionMenu(Player player, String sectionId, int questNumber, int page) {
        ShopSection section = shopLoader.getSection(sectionId);
        if (section == null) {
            player.sendMessage(hex("&#FF5555Section not found!"));
            return;
        }
        
        List<ShopItem> items = section.getItems();
        if (items.isEmpty()) {
            player.sendMessage(hex("&#FFFF00This section has no items!"));
            openMainMenu(player, questNumber);
            return;
        }
        
        // Calculate pagination
        int maxPage = Math.max(0, (items.size() - 1) / ITEMS_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        
        // Create title with section styling
        String color = section.getDisplayColor();
        String icon = section.getIconChar();
        String cleanName = ChatColor.stripColor(hex(section.getDisplayName()));
        String title = MENU_ID + ChatColor.of(color) + icon + ChatColor.DARK_GRAY + cleanName + " Shop";
        
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        // Fill bottom row with black glass
        for (int i = 45; i < 54; i++) {
            ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = border.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                border.setItemMeta(meta);
            }
            inv.setItem(i, border);
        }
        
        // Add items for current page
        int startIndex = safePage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ShopItem shopItem = items.get(i);
            ItemStack display = createItemDisplay(shopItem);
            if (display != null) {
                inv.setItem(slot, display);
            }
            slot++;
        }
        
        // Add navigation bar
        addSectionNavbar(inv, safePage, maxPage, player);
        
        // Open and track
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession("section", questNumber, sectionId, safePage));
        playerPages.put(player.getUniqueId(), safePage);
        
        playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
    
    /**
     * Create item display with buy lore
     */
    private ItemStack createItemDisplay(ShopItem shopItem) {
        ItemStack display = shopItem.createItemStack(1);
        ItemMeta meta = display.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FFFFFF") + shopItem.getPrettyName());
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            
            if (shopItem.canBuy()) {
                lore.add(hex(plugin.getMessageManager().get("shop.item.buy",
                        "price", MONEY_FORMAT.format(shopItem.getBuyPrice()))));
                lore.add(hex(plugin.getMessageManager().get("shop.item.buy-action")));
            }
            
            // Hide enchant glint spam
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        
        return display;
    }
    
    /**
     * Add navigation bar to section menu using unified navbar system
     */
    private void addSectionNavbar(Inventory inv, int currentPage, int maxPage, Player player) {
        // Get balance for navbar
        double coins = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        // Create context for navbar
        Map<String, Object> context = new HashMap<>();
        context.put("page", currentPage + 1);
        context.put("total_pages", maxPage + 1);
        context.put("previous_menu", "main_shop"); // This enables the back button
        context.put("balance", (int) Math.round(coins));
        context.put("coins", MONEY_FORMAT.format(coins));
        
        // Apply unified navbar using local NavbarManager
        navbarManager.applyNavbar(inv, player, "shop_section", context);
    }
    
    // ==================== TRANSACTION MENU ====================
    
    /**
     * Open buy transaction menu for an item
     */
    public void openBuyTransaction(Player player, ShopItem item, String sectionId, int questNumber) {
        String title = hex(plugin.getMessageManager().get("shop.transaction.title-buy",
                "item", item.getPrettyName()));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Fill background
        fillBackground(inv, Material.BLACK_STAINED_GLASS_PANE);
        
        // Initialize transaction data (default quantity 1, max 3)
        int quantity = transactionData.containsKey(player.getUniqueId()) ?
                transactionData.get(player.getUniqueId()).quantity : 1;
        // Enforce 3 item limit
        quantity = Math.min(quantity, 3);
        transactionData.put(player.getUniqueId(), new TransactionData(item, sectionId, quantity));
        
        // Item display (slot 13)
        double totalCost = item.getBuyPrice() * quantity;
        double balance = plugin.getVaultIntegration() != null ?
                plugin.getVaultIntegration().getBalance(player) : 0;
        boolean canAfford = balance >= totalCost;
        
        ItemStack display = item.createItemStack(Math.min(quantity, 64));
        ItemMeta displayMeta = display.getItemMeta();
        if (displayMeta != null) {
            displayMeta.setDisplayName(ChatColor.of("#FFFFFF") + item.getPrettyName());
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#d0de34") + "Quantity: " + ChatColor.WHITE + quantity);
            lore.add(ChatColor.GRAY + "Price per item: " + ChatColor.of("#FFD700") + 
                    MONEY_FORMAT.format(item.getBuyPrice()) + " ⛃");
            lore.add("");
            lore.add(ChatColor.WHITE + "Total Cost: " + ChatColor.GOLD + 
                    MONEY_FORMAT.format(totalCost) + " ⛃");
            lore.add("");
            if (canAfford) {
                lore.add(ChatColor.of("#55FF55") + "✔ Sufficient Funds");
                lore.add(ChatColor.of("#808080") + "  Balance after: " + ChatColor.WHITE + 
                        MONEY_FORMAT.format(balance - totalCost) + " Coins");
            } else {
                lore.add(ChatColor.of("#FF5555") + "✖ Insufficient Funds");
                lore.add(ChatColor.of("#808080") + "  Need: " + ChatColor.of("#FF5555") + 
                        MONEY_FORMAT.format(totalCost - balance) + " more Coins");
            }
            displayMeta.setLore(lore);
            display.setItemMeta(displayMeta);
        }
        inv.setItem(13, display);
        
        // Quantity controls (row 3: slots 19-25) - Max 3 items
        // -10 button (slot 19) - REMOVED for 3 item limit
        
        // -1 button (slot 20)
        if (quantity > 1) {
            ItemStack minus = new ItemStack(Material.ORANGE_TERRACOTTA);
            ItemMeta minusMeta = minus.getItemMeta();
            if (minusMeta != null) {
                minusMeta.setDisplayName(ChatColor.RED + "▼ -1 Item");
                List<String> minusLore = new ArrayList<>();
                minusLore.add("");
                minusLore.add(ChatColor.GRAY + "Remove 1 from quantity");
                minusMeta.setLore(minusLore);
                minus.setItemMeta(minusMeta);
            }
            inv.setItem(20, minus);
        } else {
            // Show disabled state for -1 button
            ItemStack minusDisabled = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta disabledMeta = minusDisabled.getItemMeta();
            if (disabledMeta != null) {
                disabledMeta.setDisplayName(ChatColor.GRAY + "[Limit Reached]");
                minusDisabled.setItemMeta(disabledMeta);
            }
            inv.setItem(20, minusDisabled);
        }
        
        // Quantity display (slot 22 - center)
        // Use paper ONLY if item is unstackable (max stack size <= 16)
        int maxStackSize = item.getMaterial().getMaxStackSize();
        ItemStack qtyDisplay;
        if (maxStackSize <= 16) {
            // Unstackable or low-stack item - use paper with quantity
            qtyDisplay = new ItemStack(Material.PAPER, Math.min(quantity, 64));
            ItemMeta qtyMeta = qtyDisplay.getItemMeta();
            if (qtyMeta != null) {
                qtyMeta.setDisplayName(ChatColor.of("#FFFF00") + "Quantity: " + ChatColor.WHITE + quantity);
                List<String> qtyLore = new ArrayList<>();
                qtyLore.add("");
                qtyLore.add(ChatColor.of("#808080") + "Item: " + ChatColor.WHITE + item.getPrettyName());
                qtyLore.add(ChatColor.of("#808080") + "Max stack: " + ChatColor.WHITE + maxStackSize);
                qtyLore.add("");
                qtyLore.add(ChatColor.of("#808080") + "Use the buttons to adjust");
                qtyMeta.setLore(qtyLore);
                qtyDisplay.setItemMeta(qtyMeta);
            }
            inv.setItem(22, qtyDisplay);
        }
        // For stackable items (maxStackSize > 16), don't show the paper display
        // Slot 22 stays as border (black glass pane)
        
        // +1 button (slot 24) - Max 3 items
        if (quantity < 3) {
            ItemStack plus = new ItemStack(Material.LIME_TERRACOTTA);
            ItemMeta plusMeta = plus.getItemMeta();
            if (plusMeta != null) {
                plusMeta.setDisplayName(ChatColor.of("#55FF55") + "▲ +1 Item");
                List<String> plusLore = new ArrayList<>();
                plusLore.add("");
                plusLore.add(ChatColor.GRAY + "Add 1 to quantity");
                plusMeta.setLore(plusLore);
                plus.setItemMeta(plusMeta);
            }
            inv.setItem(24, plus);
        } else {
            // Show disabled state for +1 button at max (3 items)
            ItemStack maxed = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta maxedMeta = maxed.getItemMeta();
            if (maxedMeta != null) {
                maxedMeta.setDisplayName(ChatColor.GRAY + "[Limit Reached]");
                maxed.setItemMeta(maxedMeta);
            }
            inv.setItem(24, maxed);
        }
        
        // +10 button (slot 25) - REMOVED for 3 item limit
        
        // Quick select removed - no preset buttons
        
        // Add navbar with back to section menu
        Map<String, Object> context = new HashMap<>();
        context.put("coins", MONEY_FORMAT.format(balance));
        context.put("previous_menu", "section"); // Shows back button
        navbarManager.applyNavbar(inv, player, "shop_transaction", context);
        
        // Confirm button (slot 49 - center bottom)
        ItemStack confirm;
        if (canAfford) {
            confirm = new ItemStack(Material.EMERALD_BLOCK);
            ItemMeta confirmMeta = confirm.getItemMeta();
            if (confirmMeta != null) {
                confirmMeta.setDisplayName(ChatColor.of("#55FF55") + "✔ CONFIRM PURCHASE");
                List<String> confirmLore = new ArrayList<>();
                confirmLore.add("");
                confirmLore.add(ChatColor.of("#808080") + "Purchase " + ChatColor.of("#00FFFF") + quantity + 
                        ChatColor.of("#808080") + " x " + ChatColor.WHITE + item.getPrettyName());
                confirmLore.add(ChatColor.of("#808080") + "Cost: " + ChatColor.of("#FFD700") + 
                        MONEY_FORMAT.format(totalCost) + " Coins");
                confirmLore.add("");
                confirmLore.add(ChatColor.of("#55FF55") + "▸ Click to confirm!");
                confirmMeta.setLore(confirmLore);
                confirm.setItemMeta(confirmMeta);
            }
        } else {
            confirm = new ItemStack(Material.REDSTONE_BLOCK);
            ItemMeta confirmMeta = confirm.getItemMeta();
            if (confirmMeta != null) {
                confirmMeta.setDisplayName(ChatColor.of("#FF5555") + "✖ Cannot Purchase");
                List<String> confirmLore = new ArrayList<>();
                confirmLore.add("");
                confirmLore.add(ChatColor.of("#FF5555") + "Insufficient coins!");
                confirmMeta.setLore(confirmLore);
                confirm.setItemMeta(confirmMeta);
            }
        }
        inv.setItem(49, confirm);
        
        // Open and track
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession("transaction", questNumber, sectionId, 0));
        
        playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
    
    // Quick select menu removed - no preset buttons
    
    // ==================== TOKEN EXCHANGE MENU ====================
    
    /**
     * Open token exchange menu (Quest 4)
     */
    public void openTokenExchange(Player player, int questNumber) {
        String title = hex(plugin.getMessageManager().get("shop.token-exchange.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Fill background
        fillBackground(inv, Material.BLACK_STAINED_GLASS_PANE);
        
        int tokenCost = plugin.getConfigManager().getQuest4TokenCost();
        double balance = plugin.getVaultIntegration() != null ?
                plugin.getVaultIntegration().getBalance(player) : 0;
        boolean canAfford = balance >= tokenCost;
        
        // Token display (slot 13)
        ItemStack tokenDisplay = new ItemStack(Material.EMERALD);
        ItemMeta tokenMeta = tokenDisplay.getItemMeta();
        if (tokenMeta != null) {
            tokenMeta.setDisplayName(ChatColor.of("#00FFFF") + "⭐ Skill Token");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Quantity: " + ChatColor.WHITE + "1");
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + MONEY_FORMAT.format(tokenCost) + " Coins");
            lore.add("");
            lore.add(ChatColor.GRAY + "Total Cost: " + ChatColor.GOLD + MONEY_FORMAT.format(tokenCost) + " Coins");
            tokenMeta.setLore(lore);
            tokenDisplay.setItemMeta(tokenMeta);
        }
        inv.setItem(13, tokenDisplay);
        
        // -1 button (slot 20) - ORANGE_TERRACOTTA, disabled since quantity is fixed at 1
        ItemStack minus = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta minusMeta = minus.getItemMeta();
        if (minusMeta != null) {
            minusMeta.setDisplayName(ChatColor.GRAY + "[Limit Reached]");
            minus.setItemMeta(minusMeta);
        }
        inv.setItem(20, minus);
        
        // Quantity display (slot 22) - fixed at 1
        ItemStack qtyDisplay = new ItemStack(Material.PAPER);
        ItemMeta qtyMeta = qtyDisplay.getItemMeta();
        if (qtyMeta != null) {
            qtyMeta.setDisplayName(ChatColor.of("#FFFF00") + "Quantity: " + ChatColor.WHITE + "1");
            List<String> qtyLore = new ArrayList<>();
            qtyLore.add("");
            qtyLore.add(ChatColor.GRAY + "Item: " + ChatColor.WHITE + "Skill Token");
            qtyLore.add(ChatColor.GRAY + "Maximum: " + ChatColor.WHITE + "1 item");
            qtyLore.add("");
            qtyLore.add(ChatColor.GRAY + "Fixed quantity for tutorial");
            qtyMeta.setLore(qtyLore);
            qtyDisplay.setItemMeta(qtyMeta);
        }
        inv.setItem(22, qtyDisplay);
        
        // +1 button (slot 24) - LIME_TERRACOTTA, disabled since quantity is fixed at 1
        ItemStack plus = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta plusMeta = plus.getItemMeta();
        if (plusMeta != null) {
            plusMeta.setDisplayName(ChatColor.GRAY + "[Limit Reached]");
            plus.setItemMeta(plusMeta);
        }
        inv.setItem(24, plus);
        
        // Balance display (slot 45) - handled by navbar
        
        // Confirm button (slot 49)
        ItemStack confirm;
        if (canAfford) {
            confirm = new ItemStack(Material.EMERALD_BLOCK);
            ItemMeta confirmMeta = confirm.getItemMeta();
            if (confirmMeta != null) {
                confirmMeta.setDisplayName(ChatColor.of("#55FF55") + "✔ CONFIRM PURCHASE");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "Purchasing: " + ChatColor.of("#00FFFF") + "1 Token");
                lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + MONEY_FORMAT.format(tokenCost) + " Coins");
                lore.add(ChatColor.GRAY + "Balance after: " + ChatColor.WHITE + MONEY_FORMAT.format(balance - tokenCost));
                lore.add("");
                lore.add(ChatColor.GREEN + "▸ Click to purchase!");
                confirmMeta.setLore(lore);
                confirm.setItemMeta(confirmMeta);
            }
        } else {
            confirm = new ItemStack(Material.REDSTONE_BLOCK);
            ItemMeta confirmMeta = confirm.getItemMeta();
            if (confirmMeta != null) {
                confirmMeta.setDisplayName(ChatColor.of("#FF5555") + "✖ INSUFFICIENT COINS");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "Cost: " + ChatColor.RED + MONEY_FORMAT.format(tokenCost) + " Coins");
                lore.add(ChatColor.GRAY + "You have: " + ChatColor.WHITE + MONEY_FORMAT.format(balance));
                lore.add(ChatColor.GRAY + "Need: " + ChatColor.RED + MONEY_FORMAT.format(tokenCost - balance) + " more");
                confirmMeta.setLore(lore);
                confirm.setItemMeta(confirmMeta);
            }
        }
        inv.setItem(49, confirm);
        
        // Add navbar with back to main menu
        Map<String, Object> context = new HashMap<>();
        context.put("coins", MONEY_FORMAT.format(balance));
        context.put("previous_menu", "main_shop"); // Shows back button
        navbarManager.applyNavbar(inv, player, "shop_token_exchange", context);
        
        // Open and track
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession("token_exchange", questNumber, null, 0));
        
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.4f);
    }
    
    // ==================== CLICK HANDLING ====================
    
    /**
     * Handle inventory click
     * @return true if handled, false otherwise
     */
    public boolean handleClick(Player player, int slot, Inventory inv) {
        MenuSession session = openMenus.get(player.getUniqueId());
        if (session == null) return false;
        
        playSound(player, Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
        
        return switch (session.menuType) {
            case "main" -> handleMainMenuClick(player, slot, session);
            case "section" -> handleSectionClick(player, slot, session);
            case "transaction" -> handleTransactionClick(player, slot, session);
            case "token_exchange" -> handleTokenExchangeClick(player, slot, session);
            default -> false;
        };
    }
    
    /**
     * Handle main menu click
     */
    private boolean handleMainMenuClick(Player player, int slot, MenuSession session) {
        // Close button
        if (slot == 53) {
            player.closeInventory();
            return true;
        }
        
        // Find clicked section
        for (ShopSection section : shopLoader.getSections()) {
            if (section.getSlot() == slot) {
                if (!section.isAvailableForQuest(session.questNumber)) {
                    player.sendMessage(hex(plugin.getMessageManager().get("shop.category.not-available")));
                    playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                    return true;
                }
                
                // Token exchange special handling
                if (section.isTokenExchange()) {
                    openTokenExchange(player, session.questNumber);
                    return true;
                }
                
                // Open section
                openSectionMenu(player, section.getId(), session.questNumber, 0);
                return true;
            }
        }
        
        return true;
    }
    
    /**
     * Handle section menu click
     */
    private boolean handleSectionClick(Player player, int slot, MenuSession session) {
        // Previous page (slot 48)
        if (slot == 48 && session.page > 0) {
            openSectionMenu(player, session.sectionId, session.questNumber, session.page - 1);
            return true;
        }
        
        // Next page (slot 50)
        ShopSection section = shopLoader.getSection(session.sectionId);
        if (section != null && slot == 50) {
            int maxPage = (section.getItemCount() - 1) / ITEMS_PER_PAGE;
            if (session.page < maxPage) {
                openSectionMenu(player, session.sectionId, session.questNumber, session.page + 1);
                return true;
            }
        }
        
        // Back button (slot 53) - navbar back button
        if (slot == 53) {
            openMainMenu(player, session.questNumber);
            return true;
        }
        
        // Item click (slots 0-44)
        if (slot >= 0 && slot < ITEMS_PER_PAGE && section != null) {
            int itemIndex = session.page * ITEMS_PER_PAGE + slot;
            List<ShopItem> items = section.getItems();
            
            if (itemIndex < items.size()) {
                ShopItem item = items.get(itemIndex);
                transactionData.put(player.getUniqueId(), new TransactionData(item, session.sectionId, 1));
                openBuyTransaction(player, item, session.sectionId, session.questNumber);
                return true;
            }
        }
        
        return true;
    }
    
    /**
     * Handle transaction menu click
     */
    private boolean handleTransactionClick(Player player, int slot, MenuSession session) {
        TransactionData data = transactionData.get(player.getUniqueId());
        if (data == null) {
            openMainMenu(player, session.questNumber);
            return true;
        }
        
        // Back button (slot 53) - navbar back button
        if (slot == 53) {
            transactionData.remove(player.getUniqueId());
            openSectionMenu(player, data.sectionId, session.questNumber, session.page);
            return true;
        }
        
        // -10 button (slot 19) - REMOVED for 3 item limit
        
        // -1 button (slot 20)
        if (slot == 20 && data.quantity > 1) {
            data.quantity--;
            openBuyTransaction(player, data.item, data.sectionId, session.questNumber);
            return true;
        }
        
        // +1 button (slot 24) - Max 3 items
        if (slot == 24 && data.quantity < 3) {
            data.quantity++;
            openBuyTransaction(player, data.item, data.sectionId, session.questNumber);
            return true;
        }
        
        // +10 button (slot 25) - REMOVED for 3 item limit
        
        // Confirm button (slot 49)
        if (slot == 49) {
            processPurchase(player, data, session.questNumber);
            return true;
        }
        
        return true;
    }
    
    // Quick select click handler removed - no preset buttons
    
    /**
     * Handle token exchange click
     */
    private boolean handleTokenExchangeClick(Player player, int slot, MenuSession session) {
        // Back button (slot 53) - navbar back button
        if (slot == 53) {
            openMainMenu(player, session.questNumber);
            return true;
        }
        
        // Confirm button (slot 49)
        if (slot == 49) {
            processTokenPurchase(player, session.questNumber);
            return true;
        }
        
        return true;
    }
    
    // ==================== PURCHASE PROCESSING ====================
    
    /**
     * Process item purchase
     */
    private void processPurchase(Player player, TransactionData data, int questNumber) {
        double totalCost = data.item.getBuyPrice() * data.quantity;
        double balance = plugin.getVaultIntegration() != null ?
                plugin.getVaultIntegration().getBalance(player) : 0;
        
        if (balance < totalCost) {
            player.sendMessage(hex(plugin.getMessageManager().get("errors.not-enough-coins",
                    "amount", MONEY_FORMAT.format(totalCost))));
            playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }
        
        // Withdraw coins
        plugin.getVaultIntegration().withdraw(player, totalCost);
        
        // Give items
        ItemStack itemStack = data.item.createItemStack(data.quantity);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        
        // Handle full inventory
        if (!leftovers.isEmpty()) {
            player.sendMessage(hex(plugin.getMessageManager().get("errors.inventory-full")));
            // Drop items at player location
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        
        // Track spending for quest system
        com.wdp.start.api.WDPStartAPI.trackCoinSpending(player, (int) Math.round(totalCost));
        
        player.sendMessage(hex(plugin.getMessageManager().get("success.purchased-item",
                "quantity", String.valueOf(data.quantity),
                "item", data.item.getPrettyName(),
                "cost", MONEY_FORMAT.format(totalCost))));
        
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        
        // Notify quest listener for Quest 3
        plugin.getQuestListener().onShopItemPurchase(player);
        
        // Clear and return to main menu
        transactionData.remove(player.getUniqueId());
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            openMainMenu(player, questNumber);
        }, 10L);
    }
    
    /**
     * Process token purchase
     */
    private void processTokenPurchase(Player player, int questNumber) {
        int tokenCost = plugin.getConfigManager().getQuest4TokenCost();
        double balance = plugin.getVaultIntegration() != null ?
                plugin.getVaultIntegration().getBalance(player) : 0;
        
        if (balance < tokenCost) {
            player.sendMessage(hex(plugin.getMessageManager().get("errors.not-enough-coins",
                    "amount", String.valueOf(tokenCost))));
            playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }
        
        // Withdraw coins
        plugin.getVaultIntegration().withdraw(player, tokenCost);
        
        // Give skill tokens via AuraSkills integration
        if (plugin.getAuraSkillsIntegration() != null) {
            plugin.getAuraSkillsIntegration().giveSkillTokens(player, 1);
        }
        
        // Track spending
        com.wdp.start.api.WDPStartAPI.trackCoinSpending(player, tokenCost);
        com.wdp.start.api.WDPStartAPI.notifyTokenPurchase(player, 1);
        
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.8f);
        
        // Notify quest listener for Quest 4 - this will trigger completeQuest() which sends the completion message
        plugin.getQuestListener().onTokenPurchase(player, 1);
        // Note: Quest completion message is sent by completeQuest() - no extra message needed
        
        // Return to main menu
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PlayerData data = plugin.getPlayerDataManager().getData(player);
            if (data.isQuestCompleted(4)) {
                // Quest complete, close shop
                player.closeInventory();
            } else {
                openMainMenu(player, questNumber);
            }
        }, 10L);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Check if a player has this shop menu open
     */
    public boolean hasMenuOpen(Player player) {
        return openMenus.containsKey(player.getUniqueId());
    }
    
    /**
     * Handle menu close
     */
    public void handleClose(Player player) {
        openMenus.remove(player.getUniqueId());
        playerPages.remove(player.getUniqueId());
        // Keep transaction data in case they reopen
    }
    
    /**
     * Check if an inventory belongs to this shop
     */
    public boolean isShopInventory(String title) {
        return title != null && title.startsWith(MENU_ID);
    }
    
    /**
     * Fill inventory background with glass pane
     */
    private void fillBackground(Inventory inv, Material material) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }
    
    /**
     * Add close button to inventory (for main menu only)
     * For menus that need back buttons, use the navbar system instead
     */
    private void addCloseButton(Inventory inv) {
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FF5555") + "✗ Close");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Close shop");
            meta.setLore(lore);
            close.setItemMeta(meta);
        }
        inv.setItem(53, close);
    }
    
    /**
     * Play sound if enabled
     */
    private void playSound(Player player, Sound sound, float volume, float pitch) {
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
    
    /**
     * Translate hex color codes
     */
    private String hex(String message) {
        if (message == null) return "";
        
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hexCode).toString());
        }
        matcher.appendTail(buffer);
        
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Menu session tracking
     */
    private static class MenuSession {
        final String menuType;
        final int questNumber;
        final String sectionId;
        final int page;
        
        MenuSession(String menuType, int questNumber, String sectionId, int page) {
            this.menuType = menuType;
            this.questNumber = questNumber;
            this.sectionId = sectionId;
            this.page = page;
        }
    }
    
    /**
     * Transaction data tracking
     */
    private static class TransactionData {
        final ShopItem item;
        final String sectionId;
        int quantity;
        
        TransactionData(ShopItem item, String sectionId, int quantity) {
            this.item = item;
            this.sectionId = sectionId;
            this.quantity = quantity;
        }
    }
}
