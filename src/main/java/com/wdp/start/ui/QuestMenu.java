package com.wdp.start.ui;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import com.wdp.start.quest.QuestManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quest Menu GUI - SkillCoins-style navigation
 */
public class QuestMenu {
    
    private final WDPStartPlugin plugin;
    
    // Track open menus
    private final ConcurrentHashMap<UUID, MenuSession> openMenus = new ConcurrentHashMap<>();
    
    // Menu identifier
    private static final String MENU_ID = "§8§l";
    
    // Hex pattern
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public QuestMenu(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Open the main quest menu
     * Data is loaded from database to ensure progress is accurately reflected
     */
    public void openMainMenu(Player player) {
        // Always get fresh data from database
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Debug log to console
        plugin.debug("[QuestMenu] Opening menu for " + player.getName() + 
            " | Started: " + data.isStarted() + 
            " | CurrentQuest: " + data.getCurrentQuest() + 
            " | Completed: " + data.isCompleted());
        
        String title = hex("&#FFD700✦ &#FFFFFFGet Started Quests &#FFD700✦");
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Fill background
        fillBackground(inv, Material.GRAY_STAINED_GLASS_PANE);
        
        // Determine menu type based on quest progress
        if (!data.isStarted()) {
            // Not started - show welcome/start menu
            plugin.debug("[QuestMenu] Showing welcome menu (not started)");
            buildWelcomeMenu(inv, player);
        } else {
            // Normal menu - show all quests with current progress
            plugin.debug("[QuestMenu] Showing normal menu with progress");
            buildNormalMenu(inv, player, data);
        }
        
        // Add navbar (always present)
        addNavbar(inv, data);
        
        // Open and track
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "main", data.getCurrentQuest()));
        
        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }
    
    /**
     * Build welcome menu for players who haven't started
     */
    private void buildWelcomeMenu(Inventory inv, Player player) {
        // Center content area
        
        // Welcome title item (slot 13)
        ItemStack welcome = createItem(Material.NETHER_STAR,
            hex("&#FFD700&l✦ &#FFFFFF&lWelcome to WDP! &#FFD700&l✦"),
            "",
            hex("&#AAAAAATake your first steps on the server"),
            hex("&#AAAAAAby completing starter quests!"),
            "",
            hex("&#55FF55You'll learn about:"),
            hex("&#FFFFFF• Exploration & Teleportation"),
            hex("&#FFFFFF• Skills & Leveling"),
            hex("&#FFFFFF• Progress Tracking"),
            hex("&#FFFFFF• Economy & Shopping"),
            "",
            hex("&#FFD700&lRewards:"),
            hex("&#FFD700✦ 300 SkillCoins"),
            hex("&#FF55FF✦ 10 SkillTokens"),
            hex("&#55FFFF✦ Starter Items"),
            ""
        );
        inv.setItem(13, welcome);
        
        // Start button (slot 31)
        ItemStack start = createItem(Material.LIME_CONCRETE,
            hex("&#55FF55&l▶ START QUESTS"),
            "",
            hex("&#AAAAAAClick to begin your adventure!"),
            "",
            hex("&#FFFF55Tip: You can cancel anytime"),
            hex("&#FFFFFFwith /quests cancel"),
            ""
        );
        addGlow(start);
        inv.setItem(31, start);
    }
    
    /**
     * Build simplified menu for Quest 5
     */

    
    /**
     * Build normal menu with all quests
     */
    private void buildNormalMenu(Inventory inv, Player player, PlayerData data) {
        // Row 1: Quests 1, 2, 3 (slots 11, 13, 15) - horizontal
        inv.setItem(11, createQuestItem(1, data));
        inv.setItem(13, createQuestItem(2, data, player)); // Quest 2 has level progress
        inv.setItem(15, createQuestItem(3, data));
        
        // Row 3: Quests 4, 5, 6 (slots 29, 31, 33) - horizontal
        inv.setItem(29, createQuestItem(4, data));
        inv.setItem(31, createQuestItem(5, data));
        inv.setItem(33, createQuestItem(6, data));
        
        // Stats row on row 5 (after one row gap)
        
        // Progress overview (slot 49 - center of row 5)
        int completed = data.getCompletedQuestCount();
        ItemStack progress = createItem(Material.CLOCK,
            hex("&#FFD700&l✦ Progress Overview"),
            "",
            hex("&#AAAAAACompleted: &#55FF55" + completed + "&#AAAAAA/&#55FF556"),
            createProgressBar(completed, 6),
            "",
            data.isCompleted() 
                ? hex("&#55FF55&l✓ All quests complete!") 
                : hex("&#FFFFFFCurrent: &#55FFFF" + plugin.getQuestManager().getQuestName(data.getCurrentQuest())),
            ""
        );
        inv.setItem(49, progress);
        
        // Cancel button (slot 47) - only if started and not completed
        if (data.isStarted() && !data.isCompleted()) {
            ItemStack cancel = createItem(Material.RED_CONCRETE,
                hex("&#FF5555✗ Cancel Quests"),
                "",
                hex("&#AAAAAAClick to cancel the quest chain."),
                hex("&#FFFF55Unspent coins will be refunded."),
                ""
            );
            inv.setItem(47, cancel);
        }
        
        // Help (slot 51)
        ItemStack help = createItem(Material.WRITABLE_BOOK,
            hex("&#55FFFF? Help"),
            "",
            hex("&#AAAAAACommands:"),
            hex("&#FFFFFF/quests &#AAAAAA- Open this menu"),
            hex("&#FFFFFF/quests cancel &#AAAAAA- Cancel quests"),
            "",
            hex("&#AAAAAANeed more help?"),
            hex("&#55FFFF" + plugin.getConfigManager().getDiscordLink()),
            ""
        );
        inv.setItem(51, help);
    }
    
    /**
     * Create a quest item based on state
     */
    private ItemStack createQuestItem(int quest, PlayerData data) {
        return createQuestItem(quest, data, null);
    }
    
    /**
     * Create a quest item based on state - with player for level progress
     */
    private ItemStack createQuestItem(int quest, PlayerData data, Player player) {
        QuestManager qm = plugin.getQuestManager();
        PlayerData.QuestProgress progress = data.getQuestProgress(quest);
        
        Material icon = qm.getQuestIcon(quest);
        String name = qm.getQuestName(quest);
        String desc = qm.getQuestDescription(quest);
        int reward = qm.getQuestRewardCoins(quest);
        int totalSteps = qm.getQuestSteps(quest);
        int currentStep = progress.getStep();
        
        List<String> lore = new ArrayList<>();
        lore.add(hex("&#555555━━━━━━━━━━━━━━━━━"));
        
        // Status line
        if (progress.isCompleted()) {
            lore.add(hex("&#AAAAAAStatus: &#55FF55✓ Completed"));
        } else if (data.getCurrentQuest() == quest) {
            lore.add(hex("&#AAAAAAStatus: &#FFFF55⚡ In Progress"));
        } else if (data.getCurrentQuest() > quest) {
            lore.add(hex("&#AAAAAAStatus: &#55FF55✓ Completed"));
        } else {
            lore.add(hex("&#AAAAAAStatus: &#777777🔒 Locked"));
        }
        
        lore.add(hex("&#555555━━━━━━━━━━━━━━━━━"));
        lore.add("");
        
        // Objective
        lore.add(hex("&#FFFFFFObjective:"));
        lore.add(hex("&#AAAAAA" + desc));
        lore.add("");
        
        // Progress (if in progress)
        if (data.getCurrentQuest() == quest && !progress.isCompleted()) {
            // Quest 2: Show level percentage instead of step progress
            if (quest == 2 && player != null) {
                int targetLevel = plugin.getConfigManager().getQuest2TargetLevel();
                int levelPercent = getLevelProgressPercent(player, data);
                lore.add(hex("&#FFFFFFForaging Level: &#55FFFF0&#FFFFFF → &#55FFFF" + targetLevel));
                lore.add(createProgressBar(levelPercent, 100));
            } else {
                lore.add(hex("&#FFFFFFProgress: &#55FFFF" + currentStep + "&#FFFFFF/&#55FFFF" + totalSteps));
                lore.add(createProgressBar(currentStep, totalSteps));
            }
            lore.add("");
        }
        
        // Reward
        lore.add(hex("&#FFFFFFReward:"));
        lore.add(hex("&#FFD700✦ " + reward + " SkillCoins"));
        
        // Extra rewards for specific quests
        if (quest == 1) {
            lore.add(hex("&#55FFFF✦ 3x Apples"));
        } else if (quest == 6) {
            lore.add(hex("&#FF55FF✦ 10 SkillTokens"));
            lore.add(hex("&#55FFFF✦ 1x Diamond + more"));
        }
        
        lore.add("");
        lore.add(hex("&#555555━━━━━━━━━━━━━━━━━"));
        
        // Action hint
        if (progress.isCompleted()) {
            lore.add(hex("&#55FF55✓ Quest completed!"));
        } else if (data.getCurrentQuest() == quest) {
            lore.add(hex("&#FFFF55▶ Click for details"));
        } else if (data.getCurrentQuest() > quest) {
            lore.add(hex("&#55FF55✓ Quest completed!"));
        } else {
            lore.add(hex("&#777777Complete previous quests first"));
        }
        
        lore.add(hex("&#555555━━━━━━━━━━━━━━━━━"));
        
        // Build name with color based on state
        String displayName;
        if (progress.isCompleted() || data.getCurrentQuest() > quest) {
            displayName = hex("&#55FF55" + name);
        } else if (data.getCurrentQuest() == quest) {
            displayName = hex("&#FFFF55" + name);
        } else {
            displayName = hex("&#777777" + name);
        }
        
        ItemStack item = createItem(icon, displayName, lore.toArray(new String[0]));
        
        // Add glow if in progress or completed
        if (data.getCurrentQuest() == quest || progress.isCompleted()) {
            addGlow(item);
        }
        
        // Special: Quest 6 gets double glow for blink effect when it's the current quest
        if (quest == 6 && data.getCurrentQuest() == 6 && !progress.isCompleted()) {
            // Add glow twice for enhanced visibility (blink effect)
            addGlow(item);
        }
        
        return item;
    }
    
    /**
     * Add the SkillCoins-style navbar
     */
    private void addNavbar(Inventory inv, PlayerData data) {
        // Home (slot 45)
        ItemStack home = createItem(Material.NETHER_STAR,
            hex("&#55FFFF⌂ Home"),
            "",
            hex("&#AAAAAAMain quest menu"),
            ""
        );
        inv.setItem(45, home);
        
        // Decorations (slots 46-48, 50-52)
        ItemStack deco = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta decoMeta = deco.getItemMeta();
        decoMeta.setDisplayName(" ");
        deco.setItemMeta(decoMeta);
        
        inv.setItem(46, deco);
        inv.setItem(47, deco);
        inv.setItem(48, deco);
        inv.setItem(50, deco);
        inv.setItem(51, deco);
        inv.setItem(52, deco);
        
        // Info (slot 49)
        ItemStack info = createItem(Material.PAPER,
            hex("&#FFD700WDP-Start"),
            "",
            hex("&#AAAAAANew player quest system"),
            hex("&#AAAAAAv1.0.0"),
            ""
        );
        inv.setItem(49, info);
        
        // Close (slot 53)
        ItemStack close = createItem(Material.BARRIER,
            hex("&#FF5555✗ Close"),
            "",
            hex("&#AAAAAAClose this menu"),
            ""
        );
        inv.setItem(53, close);
    }
    
    /**
     * Handle menu click
     */
    public void handleClick(Player player, int slot, Inventory inv) {
        MenuSession session = openMenus.get(player.getUniqueId());
        if (session == null) return;
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Play click sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        
        String menuType = session.getMenuType();
        
        // Close button (slot 53 for all menus)
        if (slot == 53) {
            // Check if this is a sub-menu that should go back
            if (menuType.startsWith("skillcoins_shop_section") || 
                menuType.equals("token_exchange") ||
                menuType.startsWith("skillcoins_transaction")) {
                // Go back to main shop menu
                if (session.getCurrentQuest() == 3) {
                    openSimplifiedShopItems(player);
                } else {
                    openSimplifiedShop(player);
                }
                return;
            }
            player.closeInventory();
            return;
        }
        
        // ========== SKILLCOINS SHOP MAIN MENU (Quest 3) ==========
        if (menuType.equals("skillcoins_shop_main")) {
            // Category clicks
            if (slot == 19) { // Food
                openShopSection(player, "Food", Material.COOKED_BEEF);
                return;
            } else if (slot == 21) { // Tools
                openShopSection(player, "Tools", Material.IRON_PICKAXE);
                return;
            } else if (slot == 23) { // Resources
                openShopSection(player, "Resources", Material.OAK_LOG);
                return;
            } else if (slot == 25) { // Blocks
                openShopSection(player, "Blocks", Material.STONE);
                return;
            }
            return;
        }
        
        // ========== SHOP SECTION MENU (Quest 3 sub-menu) ==========
        if (menuType.startsWith("skillcoins_shop_section")) {
            String category = menuType.replace("skillcoins_shop_section_", "");
            List<ShopItemData> items = getShopItemsForCategory(category);
            
            // Check if clicked slot has an item
            if (slot >= 0 && slot < items.size()) {
                ShopItemData item = items.get(slot);
                // Open transaction menu - left click = buy
                openItemTransaction(player, item, true);
                return;
            }
            return;
        }
        
        // ========== TRANSACTION MENU (Quest 3 buy/sell) ==========
        if (menuType.startsWith("skillcoins_transaction")) {
            ShopItemData item = transactionItems.get(player.getUniqueId());
            Integer quantity = transactionQuantities.get(player.getUniqueId());
            if (item == null || quantity == null) return;
            
            // +1 button (slot 24)
            if (slot == 24 && quantity < 5) {
                transactionQuantities.put(player.getUniqueId(), quantity + 1);
                openItemTransaction(player, item, true); // Refresh
                return;
            }
            
            // -1 button (slot 20)
            if (slot == 20 && quantity > 1) {
                transactionQuantities.put(player.getUniqueId(), quantity - 1);
                openItemTransaction(player, item, true); // Refresh
                return;
            }
            
            // Confirm button (slot 49)
            if (slot == 49) {
                double cost = item.buyPrice * quantity;
                double balance = 0;
                if (plugin.getVaultIntegration() != null) {
                    balance = plugin.getVaultIntegration().getBalance(player);
                }
                
                if (balance >= cost) {
                    plugin.getVaultIntegration().withdraw(player, cost);
                    player.getInventory().addItem(new ItemStack(item.material, quantity));
                    player.sendMessage(ChatColor.of("#55FF55") + "✓ Purchased " + quantity + "x " + item.name + 
                            " for " + ChatColor.of("#FFD700") + String.format("%.0f", cost) + " SkillCoins" + 
                            ChatColor.of("#55FF55") + "!");
                    
                    // Notify quest listener for Quest 3
                    plugin.getQuestListener().onShopItemPurchase(player);
                    
                    // Clear transaction data
                    transactionItems.remove(player.getUniqueId());
                    transactionQuantities.remove(player.getUniqueId());
                    
                    player.closeInventory();
                } else {
                    player.sendMessage(ChatColor.of("#FF5555") + "✗ Not enough SkillCoins! You need " + 
                            ChatColor.of("#FFD700") + String.format("%.0f", cost));
                }
                return;
            }
            return;
        }
        
        // ========== SKILLCOINS SHOP FOR TOKENS (Quest 4 main) ==========
        if (menuType.equals("skillcoins_shop_tokens")) {
            // Token exchange button (slot 31)
            if (slot == 31) {
                openTokenExchange(player);
                return;
            }
            // Other categories are NOT clickable (greyed out)
            if (slot == 19 || slot == 21 || slot == 23 || slot == 25) {
                player.sendMessage(ChatColor.of("#777777") + "✖ This section is not available during the tutorial.");
                return;
            }
            return;
        }
        
        // ========== TOKEN EXCHANGE MENU (Quest 4 sub-menu) ==========
        if (menuType.equals("token_exchange")) {
            // Confirm button (slot 49)
            if (slot == 49) {
                int tokenCost = plugin.getConfigManager().getQuest4TokenCost();
                double balance = 0;
                if (plugin.getVaultIntegration() != null) {
                    balance = plugin.getVaultIntegration().getBalance(player);
                }
                
                if (balance >= tokenCost) {
                    plugin.getVaultIntegration().withdraw(player, tokenCost);
                    player.sendMessage(ChatColor.of("#55FF55") + "✓ Purchased " + ChatColor.of("#00FFFF") + 
                            "1 SkillToken" + ChatColor.of("#55FF55") + " for " + ChatColor.of("#FFD700") + 
                            tokenCost + " SkillCoins" + ChatColor.of("#55FF55") + "!");
                    
                    // Notify quest listener for Quest 4
                    plugin.getQuestListener().onTokenPurchase(player, 1);
                    player.closeInventory();
                } else {
                    player.sendMessage(ChatColor.of("#FF5555") + "✗ Not enough SkillCoins! You need " + 
                            ChatColor.of("#FFD700") + tokenCost);
                }
                return;
            }
            // +1 button does nothing (already at max 1 for tutorial)
            if (slot == 24) {
                player.sendMessage(ChatColor.of("#777777") + "Tutorial limit: 1 token per purchase");
                return;
            }
            return;
        }
        
        // ========== SIMPLIFIED QUEST MENU (Quest 5) ==========
        if (menuType.equals("simplified_quest")) {
            // Quest icon (slot 0) or Complete button (slot 49)
            if (slot == 0 || slot == 49) {
                plugin.getQuestManager().completeQuest(player, 5);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    openMainMenu(player);
                }, 5L);
                return;
            }
            
            // Close button (slot 53)
            // Already handled at top
            return;
        }
        
        // ========== OLD MENU HANDLERS (for backward compatibility) ==========
        
        // Navbar handling
        if (slot == 45) {
            openMainMenu(player);
            return;
        }
        
        // Simplified shop items menu (Quest 3) - legacy
        if (menuType.equals("simplified_shop_items")) {
            int cost = 0;
            Material itemMat = null;
            int itemAmount = 1;
            String itemName = "";
            
            if (slot == 20) {
                cost = 10;
                itemMat = Material.APPLE;
                itemAmount = 1;
                itemName = "Apple";
            } else if (slot == 22) {
                cost = 15;
                itemMat = Material.BREAD;
                itemAmount = 1;
                itemName = "Bread";
            } else if (slot == 24) {
                cost = 20;
                itemMat = Material.TORCH;
                itemAmount = 4;
                itemName = "Torch x4";
            }
            
            if (itemMat != null && plugin.getVaultIntegration() != null) {
                double balance = plugin.getVaultIntegration().getBalance(player);
                if (balance >= cost) {
                    plugin.getVaultIntegration().withdraw(player, cost);
                    player.getInventory().addItem(new ItemStack(itemMat, itemAmount));
                    player.sendMessage(hex("&#55FF55✓ You purchased " + itemName + " for &#FFD700" + cost + " SkillCoins&#55FF55!"));
                    plugin.getQuestListener().onShopItemPurchase(player);
                    player.closeInventory();
                } else {
                    player.sendMessage(hex("&#FF5555✗ Not enough SkillCoins! You need &#FFD700" + cost + "&#FF5555."));
                }
            }
            return;
        }
        
        // Simplified token exchange menu (Quest 4) - legacy
        if (menuType.equals("simplified_shop") && slot == 31) {
            plugin.getQuestListener().onTokenPurchase(player, 1);
            player.sendMessage(hex("&#55FF55✓ You purchased 1 SkillToken!"));
            player.closeInventory();
            return;
        }
        
        // Welcome menu - start button
        if (!data.isStarted() && slot == 31) {
            plugin.getQuestManager().startQuests(player);
            return;
        }
        
        // Quest 5 WDP-Quest menu handlers
        if (data.getCurrentQuest() == 5 && !data.isQuestCompleted(5)) {
            // Back button (slot 48)
            if (slot == 48) {
                openMainMenu(player);
                return;
            }
            
            // Complete button (slot 49)
            if (slot == 49) {
                // Check stone mining progress
                PlayerData.QuestProgress progress = data.getQuestProgress(5);
                int stoneMined = progress.getCounter("stone_mined", 0);
                
                if (stoneMined >= 5) {
                    plugin.getQuestManager().completeQuest(player, 5);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        openMainMenu(player);
                    }, 5L);
                } else {
                    player.sendMessage(ChatColor.of("#FF5555") + "✖ You must mine 5 stone first! (" + stoneMined + "/5)");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                return;
            }
            
            // Close button (slot 53)
            if (slot == 53) {
                player.closeInventory();
                return;
            }
        }
        
        // Cancel button (slot 47)
        if (slot == 47 && data.isStarted() && !data.isCompleted()) {
            player.closeInventory();
            plugin.getMessageManager().send(player, "cancel.confirm");
            return;
        }
        
        // Quest clicks (slots 11, 13, 15, 29, 31, 33)
        int quest = getQuestFromSlot(slot);
        if (quest > 0 && quest == data.getCurrentQuest()) {
            showQuestDetails(player, quest);
        }
    }
    
    /**
     * Show detailed quest view
     */
    private void showQuestDetails(Player player, int quest) {
        player.closeInventory();
        
        // Quest 6: Simply complete it
        if (quest == 6) {
            plugin.getQuestManager().completeQuest(player, 6);
            player.sendMessage(hex("&#55FF55&l✓ Quest completed! Welcome to the server!"));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            return;
        }
        
        String hint = switch (quest) {
            case 1 -> "Walk towards the spawn exit to begin!";
            case 2 -> "Chop trees to reach Foraging level 1!";
            case 3 -> "Type /shop to open the shop and buy an item!";
            case 4 -> "Type /shop to open the token exchange!";
            case 5 -> "Type /quests to see the quest menu!";
            default -> "Keep going!";
        };
        
        player.sendMessage(hex("&#FFD700&l➤ &#FFFFFF" + hint));
    }
    
    /**
     * Get quest number from slot
     */
    private int getQuestFromSlot(int slot) {
        return switch (slot) {
            case 11 -> 1;  // Row 1, left
            case 13 -> 2;  // Row 1, center
            case 15 -> 3;  // Row 1, right
            case 29 -> 4;  // Row 3, left
            case 31 -> 5;  // Row 3, center
            case 33 -> 6;  // Row 3, right
            default -> 0;
        };
    }
    
    /**
     * Check if an inventory is our menu
     */
    public boolean isQuestMenu(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        String title = inv.getViewers().get(0).getOpenInventory().getTitle();
        return title != null && title.startsWith(MENU_ID);
    }
    
    /**
     * Handle menu close
     */
    public void handleClose(Player player) {
        openMenus.remove(player.getUniqueId());
    }
    
    // ==================== UTILITY METHODS ====================
    
    private void fillBackground(Inventory inv, Material material) {
        ItemStack bg = new ItemStack(material);
        ItemMeta meta = bg.getItemMeta();
        meta.setDisplayName(" ");
        bg.setItemMeta(meta);
        
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
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
    
    private void addGlow(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.MENDING, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }
    
    private String createProgressBar(int current, int total) {
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
     * Get level progress percentage for Quest 2
     */
    private int getLevelProgressPercent(Player player, PlayerData data) {
        // Check if we have stored level progress
        PlayerData.QuestProgress progress = data.getQuestProgress(2);
        if (progress.hasData("level_progress")) {
            Object val = progress.getData("level_progress");
            if (val instanceof Number) {
                return Math.min(100, ((Number) val).intValue());
            }
        }
        
        // Try to get from AuraSkills
        if (plugin.getAuraSkillsIntegration() != null) {
            try {
                String skill = plugin.getConfigManager().getQuest2Skill();
                int targetLevel = plugin.getConfigManager().getQuest2TargetLevel();
                int currentLevel = plugin.getAuraSkillsIntegration().getSkillLevel(player, skill);
                
                if (currentLevel >= targetLevel) {
                    return 100;
                }
                
                return (int) ((double) currentLevel / targetLevel * 100);
            } catch (Exception e) {
                // Fallback
            }
        }
        
        return 0;
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
    
    // ==================== SIMPLIFIED MENUS (SkillCoins-Style) ====================
    
    /**
     * Open simplified shop menu for Quest 3 - EXACT SkillCoins ShopMainMenu style
     * Shows item categories (NOT token exchange) - player must buy an item
     */
    public void openSimplifiedShopItems(Player player) {
        String title = ChatColor.of("#00FFFF") + "❖ " + ChatColor.of("#FFFFFF") + "SkillCoins Shop";
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Fill with black glass pane border (EXACT SkillCoins style)
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }
        
        // Get player balance
        double coins = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        // Player head (slot 0) - EXACT SkillCoins style
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = playerHead.getItemMeta();
        if (headMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) headMeta;
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ChatColor.of("#00FFFF") + "SkillCoins Shop " + 
                    ChatColor.of("#FFD700") + "♦ " + player.getName());
            List<String> headLore = new ArrayList<>();
            headLore.add("");
            headLore.add(ChatColor.of("#808080") + "Welcome to the SkillCoins shop!");
            headLore.add(ChatColor.of("#808080") + "Browse categories below to buy");
            headLore.add(ChatColor.of("#808080") + "items using your earnings.");
            headLore.add("");
            headLore.add(ChatColor.of("#00FFFF") + "▸ Your balance is on the right →");
            skullMeta.setLore(headLore);
            playerHead.setItemMeta(skullMeta);
        }
        inv.setItem(0, playerHead);
        
        // Balance display (slot 8) - EXACT SkillCoins style
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        if (balanceMeta != null) {
            balanceMeta.setDisplayName(ChatColor.of("#FFD700") + "⬥ Your Balance");
            List<String> balanceLore = new ArrayList<>();
            balanceLore.add("");
            balanceLore.add(ChatColor.of("#FFFF00") + "SkillCoins: " + ChatColor.of("#FFFFFF") + String.format("%.0f", coins));
            balanceLore.add("");
            balanceLore.add(ChatColor.of("#808080") + "Earn more by leveling skills");
            balanceLore.add(ChatColor.of("#808080") + "and completing objectives!");
            balanceMeta.setLore(balanceLore);
            balanceItem.setItemMeta(balanceMeta);
        }
        inv.setItem(8, balanceItem);
        
        // Shop categories - EXACT positions from SkillCoins (slots 10-35 area)
        // Food category (slot 19)
        inv.setItem(19, createShopCategory(Material.COOKED_BEEF, 
            ChatColor.of("#FFAA00") + "✦ Food Shop",
            "Buy food items to keep your hunger full!",
            8, true));
        
        // Tools category (slot 21)
        inv.setItem(21, createShopCategory(Material.IRON_PICKAXE,
            ChatColor.of("#5555FF") + "⚒ Tools Shop", 
            "Purchase tools for mining and building!",
            12, true));
        
        // Resources category (slot 23)
        inv.setItem(23, createShopCategory(Material.OAK_LOG,
            ChatColor.of("#55FF55") + "❖ Resources Shop",
            "Stock up on building materials!",
            15, true));
        
        // Blocks category (slot 25)
        inv.setItem(25, createShopCategory(Material.STONE,
            ChatColor.of("#AAAAAA") + "▣ Blocks Shop",
            "Various blocks for construction!",
            20, true));
        
        // Close button (slot 53)
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(ChatColor.of("#FF5555") + "✖ Close");
            List<String> closeLore = new ArrayList<>();
            closeLore.add("");
            closeLore.add(ChatColor.of("#808080") + "Close this menu");
            closeMeta.setLore(closeLore);
            close.setItemMeta(closeMeta);
        }
        inv.setItem(53, close);
        
        // Open and track
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "skillcoins_shop_main", 3));
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }
    
    /**
     * Open shop section menu for Quest 3 - Shows items with +1/-1 buttons only
     */
    public void openShopSection(Player player, String category, Material icon) {
        String title = ChatColor.of("#00FFFF") + category + " Shop";
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Fill bottom row with border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, border);
        }
        
        // Add shop items based on category
        List<ShopItemData> items = getShopItemsForCategory(category);
        int slot = 0;
        for (ShopItemData item : items) {
            if (slot >= 45) break;
            inv.setItem(slot, createShopItemDisplay(item));
            slot++;
        }
        
        // Get balance
        double coins = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        // Page info (slot 45)
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName(ChatColor.of("#00FFFF") + "Page 1 of 1");
            pageInfo.setItemMeta(pageMeta);
        }
        inv.setItem(45, pageInfo);
        
        // Balance (slot 46)
        ItemStack balanceItem = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        if (balanceMeta != null) {
            balanceMeta.setDisplayName(ChatColor.of("#FFFF00") + "Your Coins: " + ChatColor.of("#FFFFFF") + String.format("%.0f", coins));
            balanceItem.setItemMeta(balanceMeta);
        }
        inv.setItem(46, balanceItem);
        
        // Back button (slot 53)
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.of("#FF5555") + "← Back");
            List<String> backLore = new ArrayList<>();
            backLore.add("");
            backLore.add(ChatColor.of("#808080") + "Return to main shop menu");
            backMeta.setLore(backLore);
            back.setItemMeta(backMeta);
        }
        inv.setItem(53, back);
        
        // Track menu
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "skillcoins_shop_section_" + category.toLowerCase(), 3));
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
    
    /**
     * Open item transaction menu - ONLY +1 button (and -1 for selling), max 5 items
     */
    public void openItemTransaction(Player player, ShopItemData item, boolean isBuying) {
        String title = (isBuying ? ChatColor.of("#55FF55") + "Buy: " : ChatColor.of("#FFD700") + "Sell: ") + 
                ChatColor.of("#FFFFFF") + item.name;
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Fill border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }
        
        // Get balance
        double coins = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        int quantity = 1; // Fixed at 1 for simplified menu
        double totalPrice = item.buyPrice * quantity;
        
        // Item display (slot 13)
        ItemStack display = new ItemStack(item.material, Math.min(quantity, 64));
        ItemMeta displayMeta = display.getItemMeta();
        if (displayMeta != null) {
            displayMeta.setDisplayName(ChatColor.of("#FFFFFF") + item.name);
            List<String> displayLore = new ArrayList<>();
            displayLore.add("");
            displayLore.add(ChatColor.of("#808080") + "Quantity: " + ChatColor.of("#FFFFFF") + quantity);
            displayLore.add(ChatColor.of("#808080") + "Price per item: " + ChatColor.of("#FFD700") + String.format("%.0f", item.buyPrice));
            displayLore.add("");
            displayLore.add(ChatColor.of("#FFD700") + "Total Cost: " + ChatColor.of("#FFFFFF") + String.format("%.0f", totalPrice) + " Coins");
            displayLore.add("");
            if (coins >= totalPrice) {
                displayLore.add(ChatColor.of("#55FF55") + "✔ Sufficient Funds");
            } else {
                displayLore.add(ChatColor.of("#FF5555") + "✖ Insufficient Funds");
            }
            displayMeta.setLore(displayLore);
            display.setItemMeta(displayMeta);
        }
        inv.setItem(13, display);
        
        // ONLY +1 button (slot 24) - simplified
        ItemStack plus1 = new ItemStack(Material.LIME_TERRACOTTA);
        ItemMeta plus1Meta = plus1.getItemMeta();
        if (plus1Meta != null) {
            plus1Meta.setDisplayName(ChatColor.of("#55FF55") + "▲ +1");
            List<String> plus1Lore = new ArrayList<>();
            plus1Lore.add("");
            plus1Lore.add(ChatColor.of("#808080") + "Add 1 item");
            plus1Lore.add(ChatColor.of("#808080") + "(Max: 5)");
            plus1Meta.setLore(plus1Lore);
            plus1.setItemMeta(plus1Meta);
        }
        inv.setItem(24, plus1);
        
        // -1 button only if selling (slot 20)
        if (!isBuying) {
            ItemStack minus1 = new ItemStack(Material.RED_TERRACOTTA);
            ItemMeta minus1Meta = minus1.getItemMeta();
            if (minus1Meta != null) {
                minus1Meta.setDisplayName(ChatColor.of("#FF5555") + "▼ -1");
                List<String> minus1Lore = new ArrayList<>();
                minus1Lore.add("");
                minus1Lore.add(ChatColor.of("#808080") + "Remove 1 item");
                minus1Meta.setLore(minus1Lore);
                minus1.setItemMeta(minus1Meta);
            }
            inv.setItem(20, minus1);
        }
        
        // Quantity display (slot 22)
        ItemStack qtyDisplay = new ItemStack(Material.PAPER, Math.min(quantity, 64));
        ItemMeta qtyMeta = qtyDisplay.getItemMeta();
        if (qtyMeta != null) {
            qtyMeta.setDisplayName(ChatColor.of("#FFFF00") + "Quantity: " + ChatColor.of("#FFFFFF") + quantity);
            qtyDisplay.setItemMeta(qtyMeta);
        }
        inv.setItem(22, qtyDisplay);
        
        // Confirm button (slot 49)
        boolean canAfford = coins >= totalPrice;
        ItemStack confirm = new ItemStack(canAfford ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            if (canAfford) {
                confirmMeta.setDisplayName(ChatColor.of("#55FF55") + "✔ CONFIRM PURCHASE");
                List<String> confirmLore = new ArrayList<>();
                confirmLore.add("");
                confirmLore.add(ChatColor.of("#808080") + "Purchase " + ChatColor.of("#FFFFFF") + quantity + "x " + item.name);
                confirmLore.add(ChatColor.of("#808080") + "Cost: " + ChatColor.of("#FFD700") + String.format("%.0f", totalPrice) + " Coins");
                confirmLore.add("");
                confirmLore.add(ChatColor.of("#55FF55") + "▸ Click to confirm!");
                confirmMeta.setLore(confirmLore);
            } else {
                confirmMeta.setDisplayName(ChatColor.of("#FF5555") + "✖ Cannot Purchase");
                List<String> confirmLore = new ArrayList<>();
                confirmLore.add("");
                confirmLore.add(ChatColor.of("#FF5555") + "Insufficient coins!");
                confirmMeta.setLore(confirmLore);
            }
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(49, confirm);
        
        // Back button (slot 53)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.of("#FFFF00") + "← Back");
            back.setItemMeta(backMeta);
        }
        inv.setItem(53, back);
        
        // Track
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "skillcoins_transaction_" + item.name.toLowerCase(), 3));
        
        // Store transaction data
        transactionItems.put(player.getUniqueId(), item);
        transactionQuantities.put(player.getUniqueId(), quantity);
    }
    
    // Transaction tracking
    private final ConcurrentHashMap<UUID, ShopItemData> transactionItems = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> transactionQuantities = new ConcurrentHashMap<>();
    
    /**
     * Open simplified shop menu for Quest 4 - Token Exchange EXACT SkillCoins style
     * Shows ALL categories but they are UNCLICKABLE, only token exchange works
     */
    public void openSimplifiedShop(Player player) {
        String title = ChatColor.of("#00FFFF") + "❖ " + ChatColor.of("#FFFFFF") + "SkillCoins Shop";
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Fill with black glass pane border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }
        
        // Get player balance
        double coins = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        // Player head (slot 0)
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = playerHead.getItemMeta();
        if (headMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) headMeta;
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ChatColor.of("#00FFFF") + "SkillCoins Shop " + 
                    ChatColor.of("#FFD700") + "♦ " + player.getName());
            List<String> headLore = new ArrayList<>();
            headLore.add("");
            headLore.add(ChatColor.of("#808080") + "Welcome to the SkillCoins shop!");
            headLore.add(ChatColor.of("#808080") + "Exchange your coins for tokens!");
            headLore.add("");
            headLore.add(ChatColor.of("#00FFFF") + "▸ Your balance is on the right →");
            skullMeta.setLore(headLore);
            playerHead.setItemMeta(skullMeta);
        }
        inv.setItem(0, playerHead);
        
        // Balance display (slot 8)
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        if (balanceMeta != null) {
            balanceMeta.setDisplayName(ChatColor.of("#FFD700") + "⬥ Your Balance");
            List<String> balanceLore = new ArrayList<>();
            balanceLore.add("");
            balanceLore.add(ChatColor.of("#FFFF00") + "SkillCoins: " + ChatColor.of("#FFFFFF") + String.format("%.0f", coins));
            balanceLore.add("");
            balanceLore.add(ChatColor.of("#808080") + "Earn more by leveling skills!");
            balanceMeta.setLore(balanceLore);
            balanceItem.setItemMeta(balanceMeta);
        }
        inv.setItem(8, balanceItem);
        
        // Shop categories - UNCLICKABLE (greyed out lore)
        inv.setItem(19, createShopCategory(Material.COOKED_BEEF, 
            ChatColor.of("#777777") + "✦ Food Shop",
            "Buy food items to keep your hunger full!",
            8, false));
        
        inv.setItem(21, createShopCategory(Material.IRON_PICKAXE,
            ChatColor.of("#777777") + "⚒ Tools Shop", 
            "Purchase tools for mining and building!",
            12, false));
        
        inv.setItem(23, createShopCategory(Material.OAK_LOG,
            ChatColor.of("#777777") + "❖ Resources Shop",
            "Stock up on building materials!",
            15, false));
        
        inv.setItem(25, createShopCategory(Material.STONE,
            ChatColor.of("#777777") + "▣ Blocks Shop",
            "Various blocks for construction!",
            20, false));
        
        // TOKEN EXCHANGE - CLICKABLE (slot 31)
        ItemStack tokenExchange = new ItemStack(Material.EMERALD);
        ItemMeta tokenMeta = tokenExchange.getItemMeta();
        if (tokenMeta != null) {
            tokenMeta.setDisplayName(ChatColor.of("#55FF55") + "✦ Token Exchange");
            List<String> tokenLore = new ArrayList<>();
            tokenLore.add("");
            tokenLore.add(ChatColor.of("#808080") + "Exchange SkillCoins for");
            tokenLore.add(ChatColor.of("#808080") + "powerful SkillTokens!");
            tokenLore.add("");
            tokenLore.add(ChatColor.of("#808080") + "Rate: " + ChatColor.of("#FFD700") + "100 Coins" + 
                    ChatColor.of("#808080") + " = " + ChatColor.of("#00FFFF") + "1 Token");
            tokenLore.add("");
            tokenLore.add(ChatColor.of("#FFFF00") + "▸ Click to open!");
            tokenMeta.setLore(tokenLore);
            tokenExchange.setItemMeta(tokenMeta);
        }
        addGlow(tokenExchange);
        inv.setItem(31, tokenExchange);
        
        // Close button (slot 53)
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(ChatColor.of("#FF5555") + "✖ Close");
            List<String> closeLore = new ArrayList<>();
            closeLore.add("");
            closeLore.add(ChatColor.of("#808080") + "Close this menu");
            closeMeta.setLore(closeLore);
            close.setItemMeta(closeMeta);
        }
        inv.setItem(53, close);
        
        // Open and track
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "skillcoins_shop_tokens", 4));
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }
    
    /**
     * Open token exchange menu - EXACT SkillCoins TokenExchangeMenu style
     * ONLY +1 button, max 1 token purchase
     */
    public void openTokenExchange(Player player) {
        String title = ChatColor.of("#55FF55") + "✦ " + ChatColor.of("#FFFFFF") + "Token Exchange";
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Fill border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }
        
        int tokenCost = plugin.getConfigManager().getQuest4TokenCost();
        int quantity = 1; // ONLY 1 token can be bought
        int totalCost = tokenCost * quantity;
        
        double coinBalance = 0;
        if (plugin.getVaultIntegration() != null) {
            coinBalance = plugin.getVaultIntegration().getBalance(player);
        }
        
        // Token display (slot 13)
        ItemStack tokenDisplay = new ItemStack(Material.PAPER, 1);
        ItemMeta tokenMeta = tokenDisplay.getItemMeta();
        if (tokenMeta != null) {
            tokenMeta.setDisplayName(ChatColor.of("#00FFFF") + "✦ Skill Tokens");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Tokens to Purchase: " + ChatColor.of("#FFFFFF") + quantity);
            lore.add(ChatColor.of("#808080") + "Exchange Rate: " + ChatColor.of("#FFD700") + tokenCost + " coins" + 
                    ChatColor.of("#808080") + " = " + ChatColor.of("#00FFFF") + "1 token");
            lore.add("");
            lore.add(ChatColor.of("#FFD700") + "Total Cost: " + ChatColor.of("#FFFFFF") + totalCost + " Coins");
            lore.add("");
            if (coinBalance >= totalCost) {
                lore.add(ChatColor.of("#55FF55") + "✔ Sufficient Funds");
            } else {
                lore.add(ChatColor.of("#FF5555") + "✖ Insufficient Funds");
            }
            tokenMeta.setLore(lore);
            tokenDisplay.setItemMeta(tokenMeta);
        }
        inv.setItem(13, tokenDisplay);
        
        // ONLY +1 button (slot 24) - other buttons removed for tutorial
        ItemStack plus1 = new ItemStack(Material.LIME_TERRACOTTA);
        ItemMeta plus1Meta = plus1.getItemMeta();
        if (plus1Meta != null) {
            plus1Meta.setDisplayName(ChatColor.of("#55FF55") + "▲ +1");
            List<String> plus1Lore = new ArrayList<>();
            plus1Lore.add("");
            plus1Lore.add(ChatColor.of("#808080") + "Add 1 token");
            plus1Lore.add(ChatColor.of("#808080") + "(Tutorial: Max 1)");
            plus1Meta.setLore(plus1Lore);
            plus1.setItemMeta(plus1Meta);
        }
        inv.setItem(24, plus1);
        
        // Quantity display (slot 22)
        ItemStack qtyDisplay = new ItemStack(Material.PAPER, 1);
        ItemMeta qtyMeta = qtyDisplay.getItemMeta();
        if (qtyMeta != null) {
            qtyMeta.setDisplayName(ChatColor.of("#FFFF00") + "Quantity: " + ChatColor.of("#FFFFFF") + quantity);
            List<String> qtyLore = new ArrayList<>();
            qtyLore.add("");
            qtyLore.add(ChatColor.of("#808080") + "Tokens to purchase");
            qtyMeta.setLore(qtyLore);
            qtyDisplay.setItemMeta(qtyMeta);
        }
        inv.setItem(22, qtyDisplay);
        
        // Balance display (slot 45)
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        if (balanceMeta != null) {
            balanceMeta.setDisplayName(ChatColor.of("#FFD700") + "Your Balance");
            List<String> balanceLore = new ArrayList<>();
            balanceLore.add("");
            balanceLore.add(ChatColor.of("#FFD700") + "Coins: " + ChatColor.of("#FFFFFF") + String.format("%.0f", coinBalance));
            balanceMeta.setLore(balanceLore);
            balanceItem.setItemMeta(balanceMeta);
        }
        inv.setItem(45, balanceItem);
        
        // Confirm button (slot 49)
        boolean canAfford = coinBalance >= totalCost;
        ItemStack confirm = new ItemStack(canAfford ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            if (canAfford) {
                confirmMeta.setDisplayName(ChatColor.of("#55FF55") + "✔ CONFIRM PURCHASE");
                List<String> confirmLore = new ArrayList<>();
                confirmLore.add("");
                confirmLore.add(ChatColor.of("#808080") + "Purchase " + ChatColor.of("#00FFFF") + quantity + 
                        ChatColor.of("#808080") + " token");
                confirmLore.add(ChatColor.of("#808080") + "Cost: " + ChatColor.of("#FFD700") + totalCost + " Coins");
                confirmLore.add("");
                confirmLore.add(ChatColor.of("#55FF55") + "▸ Click to confirm!");
                confirmMeta.setLore(confirmLore);
            } else {
                confirmMeta.setDisplayName(ChatColor.of("#FF5555") + "✖ Cannot Purchase");
                List<String> confirmLore = new ArrayList<>();
                confirmLore.add("");
                confirmLore.add(ChatColor.of("#FF5555") + "Insufficient coins!");
                confirmMeta.setLore(confirmLore);
            }
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(49, confirm);
        
        // Back button (slot 53)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.of("#FFFF00") + "← Back");
            List<String> backLore = new ArrayList<>();
            backLore.add("");
            backLore.add(ChatColor.of("#808080") + "Return to main shop");
            backMeta.setLore(backLore);
            back.setItemMeta(backMeta);
        }
        inv.setItem(53, back);
        
        // Track
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "token_exchange", 4));
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
    
    /**
     * Open simplified quest menu for Quest 5 - EXACT WDP-Quest structure
     * Double chest with ONE quest + 8 progress bar segments using Custom Model Data
     */
    public void openSimplifiedQuestView(Player player) {
        String title = ChatColor.of("#FFD700") + "Quest Menu Tutorial";
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        PlayerData.QuestProgress progress = data.getQuestProgress(5);
        
        // Get stone mining progress
        int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
        int stoneTarget = 5;
        double completion = Math.min(100.0, (stoneMined * 100.0) / stoneTarget);
        boolean isComplete = stoneMined >= stoneTarget;
        
        // Fill entire background with gray glass panes (like WDP-Quest)
        fillBackground(inv, Material.GRAY_STAINED_GLASS_PANE);
        
        // === ROW 0: Quest icon (slot 4) and status (slot 8) ===
        String statusPrefix = isComplete ? ChatColor.of("#55FF55") + "✓ " : ChatColor.of("#FFFF00") + "● ";
        
        ItemStack questIcon = createItem(
            isComplete ? Material.EMERALD : Material.COMPASS,
            statusPrefix + ChatColor.of("#FFD700") + "Quest Menu Tutorial",
            "",
            ChatColor.of("#808080") + "Learn how the quest menu works!",
            "",
            ChatColor.of("#808080") + "Progress: " + ChatColor.of("#FFFF00") + String.format("%.0f", completion) + "%"
        );
        if (isComplete) addGlow(questIcon);
        inv.setItem(4, questIcon);
        
        // Status indicator (slot 8)
        Material statusMat = isComplete ? Material.LIME_DYE : Material.YELLOW_DYE;
        String statusText = isComplete ? "§a§lCOMPLETED" : "§e§lACTIVE";
        inv.setItem(8, createItem(
            statusMat,
            statusText,
            "",
            ChatColor.of("#808080") + "Quest Status",
            isComplete ? ChatColor.of("#55FF55") + "Ready to turn in!" : ChatColor.of("#FFFF00") + "In progress..."
        ));
        
        // === ROW 1: 8 progress segments (slots 10-17) ===
        int segments = 8;
        for (int seg = 0; seg < segments; seg++) {
            int slot = 10 + seg;
            inv.setItem(slot, createWDPQuestProgressSegment(seg, completion, false));
        }
        
        // === ROW 2: Objectives (slots 18-26) ===
        inv.setItem(18, createItem(
            Material.PAPER,
            ChatColor.of("#FFD700") + "§lObjectives",
            ChatColor.of("#808080") + "Complete all to finish"
        ));
        
        // Objective - Mine 5 Stone (slot 19)
        Material objMat = isComplete ? Material.LIME_DYE : Material.GRAY_DYE;
        String objPrefix = isComplete ? ChatColor.of("#55FF55") + "✓ " : ChatColor.of("#808080") + "○ ";
        String objProgress = isComplete ? ChatColor.of("#55FF55") + "Complete!" : 
                ChatColor.of("#808080") + "" + stoneMined + "/" + stoneTarget;
        
        inv.setItem(19, createItem(
            objMat,
            objPrefix + ChatColor.of("#FFFFFF") + "Mine 5 Stone",
            objProgress
        ));
        
        // === ROW 3: Rewards (slots 27-34) ===
        inv.setItem(27, createItem(
            Material.CHEST,
            ChatColor.of("#FFD700") + "§lRewards",
            ChatColor.of("#808080") + "What you'll receive"
        ));
        
        inv.setItem(28, createItem(
            Material.GOLD_NUGGET,
            ChatColor.of("#FFFF00") + "20 SkillCoins"
        ));
        
        // === ROW 4: Empty (gray glass fills this) ===
        
        // === ROW 5: WDP-Quest style navbar (slots 45-53) ===
        // Fill navbar row with black glass panes
        for (int i = 45; i < 54; i++) {
            ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = pane.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                pane.setItemMeta(meta);
            }
            inv.setItem(i, pane);
        }
        
        // Page info (slot 45)
        inv.setItem(45, createItem(
            Material.BOOK,
            ChatColor.of("#FFFF00") + "§lPage 1/1",
            ChatColor.of("#808080") + "Viewing quest 1-1"
        ));
        
        // Player info (slot 46)
        double coins = 0;
        int tokens = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = playerHead.getItemMeta();
        if (headMeta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ChatColor.of("#FFD700") + player.getName());
            List<String> headLore = new ArrayList<>();
            headLore.add(ChatColor.of("#FFD700") + "Coins: " + ChatColor.of("#FFFF00") + String.format("%.0f", coins));
            headLore.add(ChatColor.of("#55FF55") + "Tokens: " + ChatColor.of("#00FF00") + tokens);
            skullMeta.setLore(headLore);
            playerHead.setItemMeta(skullMeta);
        }
        inv.setItem(46, playerHead);
        
        // Back button (slot 48)
        inv.setItem(48, createItem(
            Material.ARROW,
            ChatColor.of("#FF5555") + "§l← Back",
            ChatColor.of("#808080") + "Return to quest menu"
        ));
        
        // Complete button (slot 49) - only active when stone mined
        if (isComplete) {
            ItemStack completeButton = createItem(
                Material.EMERALD,
                ChatColor.of("#55FF55") + "§l✓ Complete Quest",
                "",
                ChatColor.of("#FFFFFF") + "Click to finish Quest 5!",
                ""
            );
            addGlow(completeButton);
            inv.setItem(49, completeButton);
        } else {
            ItemStack incompleteButton = createItem(
                Material.GRAY_DYE,
                ChatColor.of("#808080") + "§lIncomplete",
                "",
                ChatColor.of("#808080") + "Mine 5 stone to continue",
                ""
            );
            inv.setItem(49, incompleteButton);
        }
        
        // Close button (slot 53)
        inv.setItem(53, createItem(
            Material.BARRIER,
            ChatColor.of("#FF5555") + "§lClose",
            ChatColor.of("#808080") + "Click to close menu"
        ));
        
        // Track menu
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "simplified_quest", 5));
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }
    
    /**
     * Create WDP-Quest style progress segment with Custom Model Data
     * @param segmentIndex Segment number (0-7)
     * @param completion Overall completion percentage (0-100)
     * @param isHard Whether this is a hard quest (red vs green)
     */
    private ItemStack createWDPQuestProgressSegment(int segmentIndex, double completion, boolean isHard) {
        int segments = 8;
        int fillsPerSegment = 5;
        int totalUnits = segments * fillsPerSegment; // 40
        
        // Convert percentage to units (0-40)
        int totalFilledUnits = (int) Math.round(completion / 100.0 * totalUnits);
        
        // Calculate this segment's fill level (0-5)
        int unitsBeforeThis = segmentIndex * fillsPerSegment;
        int unitsInThisSegment = Math.max(0, Math.min(fillsPerSegment, totalFilledUnits - unitsBeforeThis));
        
        // Custom Model Data: 1000+fill for normal (green), 1010+fill for hard (red)
        int cmdBase = isHard ? 1010 : 1000;
        
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setCustomModelData(cmdBase + unitsInThisSegment);
            
            // Visual feedback (fallback without resource pack)
            String color = isHard ? ChatColor.of("#FF5555") + "" : ChatColor.of("#55FF55") + "";
            String emptyColor = ChatColor.of("#808080") + "";
            
            StringBuilder segmentBar = new StringBuilder();
            for (int i = 0; i < fillsPerSegment; i++) {
                segmentBar.append(i < unitsInThisSegment ? color + "█" : emptyColor + "░");
            }
            
            meta.setDisplayName(segmentBar.toString());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.of("#808080") + "Segment " + (segmentIndex + 1) + "/8");
            lore.add(ChatColor.of("#808080") + "Fill: " + color + unitsInThisSegment + "/5");
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Overall: " + ChatColor.of("#FFFFFF") + String.format("%.0f", completion) + "%");
            
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    // ==================== HELPER METHODS ====================
    
    private ItemStack createShopCategory(Material icon, String name, String description, int itemCount, boolean clickable) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#808080") + description);
            lore.add("");
            lore.add(ChatColor.of("#808080") + "Items: " + ChatColor.of("#FFFFFF") + itemCount);
            lore.add("");
            if (clickable) {
                lore.add(ChatColor.of("#FFFF00") + "▸ Click to open!");
            } else {
                lore.add(ChatColor.of("#777777") + "✖ Not available during tutorial");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createShopItemDisplay(ShopItemData item) {
        ItemStack display = new ItemStack(item.material);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FFFFFF") + item.name);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#55FF55") + "● Buy: " + ChatColor.of("#FFFFFF") + String.format("%.0f", item.buyPrice) + 
                    ChatColor.of("#FFFF00") + " Coins");
            lore.add(ChatColor.of("#808080") + "  └ " + ChatColor.of("#FFFF00") + "Left Click" + 
                    ChatColor.of("#808080") + " to purchase");
            lore.add("");
            if (item.canSell) {
                lore.add(ChatColor.of("#FFD700") + "● Sell: " + ChatColor.of("#FFFFFF") + String.format("%.0f", item.sellPrice) + 
                        ChatColor.of("#FFFF00") + " Coins");
                lore.add(ChatColor.of("#808080") + "  └ " + ChatColor.of("#FFFF00") + "Right Click" + 
                        ChatColor.of("#808080") + " to sell");
            }
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }
    
    private List<ShopItemData> getShopItemsForCategory(String category) {
        List<ShopItemData> items = new ArrayList<>();
        switch (category.toLowerCase()) {
            case "food":
                items.add(new ShopItemData(Material.APPLE, "Apple", 10, 5, true));
                items.add(new ShopItemData(Material.BREAD, "Bread", 15, 8, true));
                items.add(new ShopItemData(Material.COOKED_BEEF, "Steak", 25, 12, true));
                items.add(new ShopItemData(Material.COOKED_PORKCHOP, "Cooked Porkchop", 25, 12, true));
                items.add(new ShopItemData(Material.GOLDEN_APPLE, "Golden Apple", 500, 250, true));
                break;
            case "tools":
                items.add(new ShopItemData(Material.WOODEN_PICKAXE, "Wooden Pickaxe", 20, 5, true));
                items.add(new ShopItemData(Material.STONE_PICKAXE, "Stone Pickaxe", 50, 15, true));
                items.add(new ShopItemData(Material.IRON_PICKAXE, "Iron Pickaxe", 150, 50, true));
                items.add(new ShopItemData(Material.WOODEN_AXE, "Wooden Axe", 20, 5, true));
                items.add(new ShopItemData(Material.STONE_AXE, "Stone Axe", 50, 15, true));
                break;
            case "resources":
                items.add(new ShopItemData(Material.OAK_LOG, "Oak Log", 5, 2, true));
                items.add(new ShopItemData(Material.COBBLESTONE, "Cobblestone", 2, 1, true));
                items.add(new ShopItemData(Material.IRON_INGOT, "Iron Ingot", 100, 50, true));
                items.add(new ShopItemData(Material.COAL, "Coal", 10, 5, true));
                break;
            case "blocks":
                items.add(new ShopItemData(Material.STONE, "Stone", 3, 1, true));
                items.add(new ShopItemData(Material.OAK_PLANKS, "Oak Planks", 3, 1, true));
                items.add(new ShopItemData(Material.GLASS, "Glass", 10, 5, true));
                items.add(new ShopItemData(Material.TORCH, "Torch", 5, 2, true));
                break;
        }
        return items;
    }
    
    /**
     * Simple shop item data holder
     */
    public static class ShopItemData {
        public final Material material;
        public final String name;
        public final double buyPrice;
        public final double sellPrice;
        public final boolean canSell;
        
        public ShopItemData(Material material, String name, double buyPrice, double sellPrice, boolean canSell) {
            this.material = material;
            this.name = name;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.canSell = canSell;
        }
    }
    
    // ==================== INNER CLASS ====================
    
    public static class MenuSession {
        private final Player player;
        private final String menuType;
        private final int currentQuest;
        
        public MenuSession(Player player, String menuType, int currentQuest) {
            this.player = player;
            this.menuType = menuType;
            this.currentQuest = currentQuest;
        }
        
        public Player getPlayer() {
            return player;
        }
        
        public String getMenuType() {
            return menuType;
        }
        
        public int getCurrentQuest() {
            return currentQuest;
        }
    }
}
