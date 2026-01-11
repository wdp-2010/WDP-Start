package com.wdp.start.ui.menu;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import com.wdp.start.ui.animation.BlinkAnimationManager;
import com.wdp.start.ui.reminder.QuestReminderManager;
import com.wdp.start.ui.shop.ShopDataLoader;
import com.wdp.start.ui.shop.ShopItemData;
import com.wdp.start.ui.shop.ShopMenuBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QuestMenuCoordinator - Central coordinator for all quest menu operations.
 * 
 * This class acts as the entry point and router, delegating actual menu 
 * building and handling to specialized builder classes:
 * - WelcomeMenuBuilder: Welcome screen for new players
 * - QuestItemBuilder: Individual quest item creation
 * - Quest5MenuBuilder: Quest 5 simplified view
 * - ShopMenuBuilder: All shop-related menus
 * - NavbarRenderer: Universal navigation bar
 * - BlinkAnimationManager: Quest item blink effects
 * - QuestReminderManager: Quest 5/6 reminder system
 * 
 * @author WDP-Start Rewrite
 */
public class QuestMenuCoordinator {
    
    private final WDPStartPlugin plugin;
    
    // Builders and managers
    private final WelcomeMenuBuilder welcomeBuilder;
    private final QuestItemBuilder questItemBuilder;
    private final Quest5MenuBuilder quest5Builder;
    private final ShopMenuBuilder shopBuilder;
    private final ShopDataLoader shopDataLoader;
    private final NavbarRenderer navbarRenderer;
    private final BlinkAnimationManager blinkManager;
    private final QuestReminderManager reminderManager;
    private final MenuSessionManager sessionManager;
    
    // Menu identifier for our menus
    private static final String MENU_ID = "§8§l";
    
    // Transaction tracking (for shop purchases)
    private final ConcurrentHashMap<UUID, ShopItemData> transactionItems = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> transactionQuantities = new ConcurrentHashMap<>();
    
    // Track blinking animation tasks
    private final ConcurrentHashMap<UUID, Integer> blinkingTasks = new ConcurrentHashMap<>();
    
    public QuestMenuCoordinator(WDPStartPlugin plugin) {
        this.plugin = plugin;
        
        // Initialize builders
        this.welcomeBuilder = new WelcomeMenuBuilder(plugin);
        this.questItemBuilder = new QuestItemBuilder(plugin);
        this.quest5Builder = new Quest5MenuBuilder(plugin);
        this.shopDataLoader = new ShopDataLoader(plugin);
        this.shopBuilder = new ShopMenuBuilder(plugin, shopDataLoader);
        this.navbarRenderer = new NavbarRenderer(plugin);
        this.sessionManager = new MenuSessionManager();
        
        // Initialize managers with callbacks
        this.blinkManager = new BlinkAnimationManager(plugin);
        this.reminderManager = new QuestReminderManager(plugin, this::onReminderAutoComplete);
    }
    
    // ==================== PUBLIC MENU OPENERS ====================
    
    /**
     * Open the main quest menu for a player.
     * Entry point for /quests command.
     */
    public void openMainMenu(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Auto-advance if current quest is complete
        if (data.isStarted() && !data.isCompleted() && data.isQuestCompleted(data.getCurrentQuest())) {
            plugin.debug("[QuestMenuCoordinator] Current quest " + data.getCurrentQuest() + " is completed, advancing...");
            data.advanceQuest();
            plugin.getPlayerDataManager().saveData(data);
            plugin.getPlayerDataManager().forceSave(player.getUniqueId());
        }
        
        plugin.debug("[QuestMenuCoordinator] Opening menu for " + player.getName() + 
            " | Started: " + data.isStarted() + 
            " | CurrentQuest: " + data.getCurrentQuest() + 
            " | Completed: " + data.isCompleted());
        
        String title = MenuUtils.hex(plugin.getMessageManager().get("menu.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        if (!data.isStarted()) {
            // Welcome menu for new players
            plugin.debug("[QuestMenuCoordinator] Showing welcome menu (not started)");
            welcomeBuilder.build(inv);
        } else {
            // Normal quest grid
            plugin.debug("[QuestMenuCoordinator] Showing normal menu with progress");
            buildQuestGrid(inv, player, data);
        }
        
        // Add universal navbar
        addNavbar(inv, player, data);
        
        // Open and track
        player.openInventory(inv);
        sessionManager.startSession(player.getUniqueId(), MenuSessionManager.MenuType.MAIN, data.getCurrentQuest());
        
        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }
    
    /**
     * Open shop menu for Quest 3 (buy items)
     */
    public void openShopMenu(Player player) {
        Inventory inv = shopBuilder.buildMainShopMenu(player, false);
        player.openInventory(inv);
        sessionManager.startSession(player.getUniqueId(), MenuSessionManager.MenuType.SHOP_MAIN, 3);
        playSound(player);
    }
    
    /**
     * Open token shop for Quest 4
     */
    public void openTokenShop(Player player) {
        Inventory inv = shopBuilder.buildMainShopMenu(player, true);
        player.openInventory(inv);
        sessionManager.startSession(player.getUniqueId(), MenuSessionManager.MenuType.TOKEN_SHOP, 4);
        playSound(player);
    }
    
    /**
     * Open shop section (e.g., "Tools", "Food")
     */
    public void openShopSection(Player player, String category, Material icon) {
        Inventory inv = shopBuilder.buildShopSection(player, category, icon);
        player.openInventory(inv);
        sessionManager.startSession(player.getUniqueId(), MenuSessionManager.MenuType.SHOP_SECTION, 3, category);
        playSound(player);
    }
    
    /**
     * Open item transaction menu (buy confirmation)
     */
    public void openItemTransaction(Player player, ShopItemData item, boolean isBuying) {
        int quantity = transactionQuantities.getOrDefault(player.getUniqueId(), 1);
        Inventory inv = shopBuilder.buildTransactionMenu(player, item, quantity, isBuying);
        player.openInventory(inv);
        
        transactionItems.put(player.getUniqueId(), item);
        transactionQuantities.put(player.getUniqueId(), quantity);
        
        sessionManager.startSession(player.getUniqueId(), MenuSessionManager.MenuType.TRANSACTION, 3);
        playSound(player);
    }
    
    /**
     * Open token exchange menu for Quest 4
     */
    public void openTokenExchange(Player player) {
        Inventory inv = shopBuilder.buildTokenExchangeMenu(player);
        player.openInventory(inv);
        sessionManager.startSession(player.getUniqueId(), MenuSessionManager.MenuType.TOKEN_EXCHANGE, 4);
        playSound(player);
    }
    
    /**
     * Open Quest 5 simplified view
     */
    public void openQuest5View(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        Inventory inv = quest5Builder.build(player, data);
        
        // Add navbar
        Map<String, Object> context = new HashMap<>();
        context.put("menu_name", "Quest 5 - Simple Mining");
        context.put("menu_description", "Complete your first mining quest");
        context.put("page", 1);
        context.put("total_pages", 1);
        
        int completed = data.getCompletedQuestCount();
        context.put("completed_quests", completed);
        context.put("total_quests", 6);
        context.put("progress_bar", MenuUtils.createProgressBar(completed, 6));
        
        String currentQuestName = data.isCompleted() 
            ? plugin.getMessageManager().get("menu.progress.all-complete")
            : plugin.getQuestManager().getQuestName(data.getCurrentQuest());
        context.put("current_quest", currentQuestName);
        
        // Currency info
        double coins = plugin.getVaultIntegration() != null ? plugin.getVaultIntegration().getBalance(player) : 0;
        context.put("balance", coins);
        context.put("coins", String.format("%.0f", coins));
        context.put("tokens", "0");
        
        navbarRenderer.apply(inv, player, "quest_view", context);
        
        player.openInventory(inv);
        sessionManager.startSession(player.getUniqueId(), MenuSessionManager.MenuType.QUEST_VIEW, 5);
        // If Quest 5 can be completed (5 stone mined and not completed), make emerald blink
        PlayerData.QuestProgress progress = data.getQuestProgress(5);
        int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
        boolean canComplete = stoneMined >= 5 && !data.isQuestCompleted(5);
        if (canComplete) {
            startBlinkingAnimation(player, 9);
        }
        playSound(player);
    }
    
    /**
     * Open the quest detail view for Quest 5
     */
    public void openQuestDetailView(Player player, int questId) {
        if (questId != 5) return; // Only Quest 5 has detail view for now
        
        String title = MenuUtils.hex(plugin.getMessageManager().get("quest-detail.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        PlayerData.QuestProgress progress = data.getQuestProgress(5);
        
        // Get stone mining progress
        int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
        int stoneTarget = 5;
        double completion = Math.min(100.0, (stoneMined * 100.0) / stoneTarget);
        boolean isComplete = stoneMined >= stoneTarget;
        
        // === ROW 0: Header ===
        // Quest icon (slot 4)
        String questName = isComplete 
            ? MenuUtils.hex(plugin.getMessageManager().get("quest-view.quest-icon.complete.name"))
            : MenuUtils.hex(plugin.getMessageManager().get("quest-view.quest-icon.incomplete.name"));
        List<String> questLore = plugin.getMessageManager().getList("quest-detail.quest-icon.lore",
                "required", "0");
        ItemStack questIcon = MenuUtils.createItem(
            isComplete ? Material.EMERALD : Material.COMPASS,
            questName,
            questLore.stream().map(MenuUtils::hex).toArray(String[]::new)
        );
        if (isComplete) MenuUtils.addGlow(questIcon);
        inv.setItem(4, questIcon);
        
        // Status indicator (slot 8)
        Material statusMat = isComplete ? Material.LIME_DYE : Material.YELLOW_DYE;
        String statusText = isComplete 
            ? MenuUtils.hex(plugin.getMessageManager().get("quest-detail.status.completed.name"))
            : MenuUtils.hex(plugin.getMessageManager().get("quest-detail.status.active.name"));
        List<String> statusLore = plugin.getMessageManager().getList("quest-detail.status.lore");
        inv.setItem(8, MenuUtils.createItem(
            statusMat,
            statusText,
            statusLore.stream().map(MenuUtils::hex).toArray(String[]::new)
        ));
        
        // === ROW 1: Full-width progress bar ===
        for (int seg = 0; seg < 9; seg++) {
            int slot = 9 + seg; // slots 9-17
            inv.setItem(slot, createWDPQuestProgressSegment(seg, completion, false));
        }
        
        // === ROW 2: Objectives ===
        inv.setItem(18, MenuUtils.createItem(
            Material.PAPER,
            MenuUtils.hex(plugin.getMessageManager().get("quest-detail.objectives.title")),
            MenuUtils.hex(plugin.getMessageManager().get("quest-detail.objectives.subtitle"))
        ));
        
        // Objective - Mine 5 Stone (slot 19)
        Material objMat = isComplete ? Material.LIME_DYE : Material.GRAY_DYE;
        String objStatus = isComplete 
            ? MenuUtils.hex(plugin.getMessageManager().get("quest-detail.objectives.complete", "objective", "Mine 5 Stone"))
            : MenuUtils.hex(plugin.getMessageManager().get("quest-detail.objectives.incomplete", "objective", "Mine 5 Stone"));
        String objProgress = isComplete 
            ? MenuUtils.hex(plugin.getMessageManager().get("quest-detail.objectives.progress-complete"))
            : MenuUtils.hex(plugin.getMessageManager().get("quest-detail.objectives.progress-incomplete", 
                "current", String.valueOf(stoneMined), "total", String.valueOf(stoneTarget)));
        
        inv.setItem(19, MenuUtils.createItem(
            objMat,
            objStatus,
            objProgress
        ));
        
        // === ROW 3: Rewards ===
        inv.setItem(27, MenuUtils.createItem(
            Material.CHEST,
            MenuUtils.hex(plugin.getMessageManager().get("quest-detail.rewards.title")),
            MenuUtils.hex(plugin.getMessageManager().get("quest-detail.rewards.subtitle"))
        ));
        
        // Reward - 20 SkillCoins (slot 28)
        List<String> rewardLore = plugin.getMessageManager().getList("quest-detail.rewards.skillcoins.lore");
        inv.setItem(28, MenuUtils.createItem(
            Material.GOLD_NUGGET,
            MenuUtils.hex(plugin.getMessageManager().get("quest-detail.rewards.skillcoins.name", "amount", "20")),
            rewardLore.stream().map(MenuUtils::hex).toArray(String[]::new)
        ));
        
        // === ROW 4: Empty ===
        
        // === ROW 5: Universal navbar ===
        Map<String, Object> context = new HashMap<>();
        context.put("previous_menu", "quest_view");
        context.put("menu_name", "Quest Details");
        context.put("menu_description", "Detailed quest information");
        
        // Currency info for navbar
        double coins = plugin.getVaultIntegration() != null ? plugin.getVaultIntegration().getBalance(player) : 0;
        context.put("balance", coins);
        context.put("coins", String.format("%.0f", coins));
        context.put("tokens", "0");
        
        navbarRenderer.apply(inv, player, "quest_detail", context);
        
        // Track menu
        player.openInventory(inv);
        sessionManager.startSession(player.getUniqueId(), MenuSessionManager.MenuType.QUEST_DETAIL, 5);
        
        playSound(player);
    }
    
    // ==================== CLICK HANDLING ====================
    
    /**
     * Handle menu click - routes to appropriate handler
     */
    public void handleClick(Player player, int slot, Inventory inv) {
        MenuSessionManager.Session session = sessionManager.getSession(player.getUniqueId());
        if (session == null) return;
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Play click sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        
        // Route to appropriate handler based on menu type
        switch (session.menuType()) {
            case MAIN -> handleMainMenuClick(player, slot, data);
            case SHOP_MAIN -> handleShopMainClick(player, slot, data);
            case SHOP_SECTION -> handleShopSectionClick(player, slot, session.context());
            case TRANSACTION -> handleTransactionClick(player, slot);
            case TOKEN_SHOP -> handleTokenShopClick(player, slot);
            case TOKEN_EXCHANGE -> handleTokenExchangeClick(player, slot);
            case QUEST_VIEW -> handleQuestViewClick(player, slot, data);
            case QUEST_DETAIL -> handleQuestDetailClick(player, slot);
        }
    }
    
    private void handleMainMenuClick(Player player, int slot, PlayerData data) {
        // Navbar close button
        if (slot == 53) {
            player.closeInventory();
            return;
        }
        
        // Navbar back button (shouldn't appear on main, but safety)
        if (slot == 45) {
            return;
        }
        
        // Welcome menu start button
        if (!data.isStarted() && slot == 31) {
            plugin.getQuestManager().startQuests(player);
            return;
        }
        
        // Quest clicks - allow clicking Quest 5 and 6 even if not current quest
        int quest = getQuestFromSlot(slot);
        if (quest > 0 && quest == data.getCurrentQuest()) {
            handleQuestClick(player, quest, data);
        } else if (quest == 5 || quest == 6) {
            // For Quest 5 and 6, show objective instead of completing
            showQuestObjective(player, quest, data);
        }
    }
    
    private void handleQuestClick(Player player, int quest, PlayerData data) {
        player.closeInventory();
        
        PlayerData.QuestProgress progress = data.getQuestProgress(quest);
        
        // Quest 5: Open simplified view or complete
        if (quest == 5) {
            if (progress.isCompleted()) {
                player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("quest.already-completed")));
                return;
            }
            plugin.getQuestManager().completeQuest(player, 5);
            reminderManager.cancelReminders(player, 5);
            return;
        }
        
        // Quest 6: Complete directly
        if (quest == 6) {
            if (progress.isCompleted()) {
                player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("quest.already-completed")));
                return;
            }
            plugin.getQuestManager().completeQuest(player, 6);
            reminderManager.cancelReminders(player, 6);
            return;
        }
        
        // Other quests: Show hint
        String hint = switch (quest) {
            case 1 -> plugin.getMessageManager().get("quests.hints.quest1");
            case 2 -> plugin.getMessageManager().get("quests.hints.quest2");
            case 3 -> plugin.getMessageManager().get("quests.hints.quest3");
            case 4 -> plugin.getMessageManager().get("quests.hints.quest4");
            default -> plugin.getMessageManager().get("quests.hints.default");
        };
        player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("success.quest-hint", "hint", hint)));
    }
    
    private void handleShopMainClick(Player player, int slot, PlayerData data) {
        if (slot == 53) {
            openMainMenu(player);
            return;
        }
        
        // Check for category clicks via shop data loader
        ShopDataLoader.SectionInfo section = shopDataLoader.getSectionInfoBySlot(slot);
        if (section != null) {
            String fname = section.fileName().toLowerCase();
            
            // Token exchange
            if (fname.contains("token_exchange") || fname.contains("token exchange")) {
                if (data.getCurrentQuest() == 4 && !data.isQuestCompleted(4)) {
                    openTokenExchange(player);
                } else {
                    player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("errors.token-not-available")));
                }
                return;
            }
            
            // Skip unavailable sections
            if (fname.contains("skilllevels") || fname.contains("tokens")) {
                player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("errors.feature-not-available")));
                return;
            }
            
            openShopSection(player, section.displayName(), section.icon());
        }
    }
    
    private void handleShopSectionClick(Player player, int slot, String category) {
        if (slot == 53 || slot == 45) {
            openShopMenu(player);
            return;
        }
        
        // Check for item click
        java.util.List<ShopItemData> items = shopDataLoader.getItemsForCategory(category);
        if (slot >= 0 && slot < items.size()) {
            ShopItemData item = items.get(slot);
            openItemTransaction(player, item, true);
        }
    }
    
    private void handleTransactionClick(Player player, int slot) {
        ShopItemData item = transactionItems.get(player.getUniqueId());
        Integer quantity = transactionQuantities.get(player.getUniqueId());
        if (item == null || quantity == null) return;
        
        // +1 button
        if (slot == 24 && quantity < 3) {
            transactionQuantities.put(player.getUniqueId(), quantity + 1);
            openItemTransaction(player, item, true);
            return;
        }
        
        // -1 button
        if (slot == 20 && quantity > 1) {
            transactionQuantities.put(player.getUniqueId(), quantity - 1);
            openItemTransaction(player, item, true);
            return;
        }
        
        // Confirm button
        if (slot == 49) {
            executePurchase(player, item, quantity);
            return;
        }
        
        // Back button
        if (slot == 53) {
            clearTransactionData(player);
            openShopMenu(player);
        }
    }
    
    private void handleTokenShopClick(Player player, int slot) {
        if (slot == 53) {
            openMainMenu(player);
            return;
        }
        
        // Check for token exchange via slot
        ShopDataLoader.SectionInfo section = shopDataLoader.getSectionInfoBySlot(slot);
        if (section != null) {
            String fname = section.fileName().toLowerCase();
            if (fname.contains("token_exchange") || fname.contains("token exchange")) {
                openTokenExchange(player);
                return;
            }
        }
        
        // Fallback slot 31
        if (slot == 31) {
            openTokenExchange(player);
            return;
        }
        
        player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("errors.section-not-available")));
    }
    
    private void handleTokenExchangeClick(Player player, int slot) {
        // +1 button (locked during tutorial)
        if (slot == 24) {
            player.sendMessage(MenuUtils.hex("&#777777🔒 &#AAAAAATutorial limitation: You can only purchase 1 token."));
            player.sendMessage(MenuUtils.hex("&#AAAAAAThe full shop unlocks after completing starter quests!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            return;
        }
        
        // Confirm button
        if (slot == 49) {
            executeTokenPurchase(player);
            return;
        }
        
        // Back button
        if (slot == 53) {
            openTokenShop(player);
        }
    }
    
    private void handleQuestViewClick(Player player, int slot, PlayerData data) {
        // Quest icon click
        if (slot == 9) {
            PlayerData.QuestProgress progress = data.getQuestProgress(5);
            int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
            
            if (data.isQuestCompleted(5)) {
                // Already completed
                player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("quest.already-completed")));
            } else if (stoneMined >= 5) {
                // Can complete - complete the quest
                plugin.getQuestManager().completeQuest(player, 5);
                reminderManager.cancelReminders(player, 5);
                plugin.getServer().getScheduler().runTaskLater(plugin, player::closeInventory, 5L);
            } else {
                // Not ready - open quest details
                openQuestDetailView(player, 5);
            }
            return;
        }
        
        // Progress bar click
        if (slot >= 10 && slot <= 17) {
            PlayerData.QuestProgress progress = data.getQuestProgress(5);
            int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
            player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("quest-progress.stone-progress", 
                    "current", String.valueOf(stoneMined), "required", "5")));
            return;
        }
        
        // Navbar
        if (slot == 45) {
            openMainMenu(player);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
        }
    }
    
    private void handleQuestDetailClick(Player player, int slot) {
        // Quest icon click in details view - complete quest
        if (slot == 4) {
            PlayerData data = plugin.getPlayerDataManager().getData(player);
            PlayerData.QuestProgress progress = data.getQuestProgress(5);
            int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
            
            if (stoneMined >= 5) {
                plugin.getQuestManager().completeQuest(player, 5);
                reminderManager.cancelReminders(player, 5);
                plugin.getServer().getScheduler().runTaskLater(plugin, player::closeInventory, 5L);
            } else {
                player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("quest-progress.stone-progress", 
                        "current", String.valueOf(stoneMined), "required", "5")));
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> openQuestDetailView(player, 5), 2L);
            }
            return;
        }
        
        // Back to quest view (slot 53 according to navbar.yml)
        if (slot == 53) {
            openQuest5View(player);
        }
    }
    
    // ==================== PURCHASE EXECUTION ====================
    
    private void executePurchase(Player player, ShopItemData item, int quantity) {
        double cost = item.buyPrice() * quantity;
        double balance = plugin.getVaultIntegration() != null ? plugin.getVaultIntegration().getBalance(player) : 0;
        
        if (balance >= cost) {
            plugin.getVaultIntegration().withdraw(player, cost);
            org.bukkit.inventory.ItemStack itemStack = new org.bukkit.inventory.ItemStack(item.material(), quantity);
            java.util.Map<Integer, org.bukkit.inventory.ItemStack> leftovers = player.getInventory().addItem(itemStack);
            
            com.wdp.start.api.WDPStartAPI.trackCoinSpending(player, (int) Math.round(cost));
            
            player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("success.purchased-item",
                    "quantity", String.valueOf(quantity),
                    "item", item.name(),
                    "cost", String.format("%.0f", cost))));
            
            if (!leftovers.isEmpty()) {
                player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("errors.inventory-full")));
            }
            
            plugin.getQuestListener().onShopItemPurchase(player);
            clearTransactionData(player);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> openMainMenu(player), 10L);
        } else {
            player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("errors.not-enough-coins",
                    "amount", String.format("%.0f", cost))));
        }
    }
    
    private void executeTokenPurchase(Player player) {
        int tokenCost = plugin.getConfigManager().getQuest4TokenCost();
        double balance = plugin.getVaultIntegration() != null ? plugin.getVaultIntegration().getBalance(player) : 0;
        
        if (balance >= tokenCost) {
            plugin.getVaultIntegration().withdraw(player, tokenCost);
            
            if (plugin.getAuraSkillsIntegration() != null) {
                plugin.getAuraSkillsIntegration().giveSkillTokens(player, 1);
            }
            
            com.wdp.start.api.WDPStartAPI.trackCoinSpending(player, tokenCost);
            com.wdp.start.api.WDPStartAPI.notifyTokenPurchase(player, 1);
            
            plugin.getQuestListener().onTokenPurchase(player, 1);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> openMainMenu(player), 10L);
        } else {
            player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("errors.not-enough-coins",
                    "amount", String.valueOf(tokenCost))));
        }
    }
    
    private void clearTransactionData(Player player) {
        transactionItems.remove(player.getUniqueId());
        transactionQuantities.remove(player.getUniqueId());
    }
    
    // ==================== MENU BUILDING HELPERS ====================
    
    private void buildQuestGrid(Inventory inv, Player player, PlayerData data) {
        // Row 1: Quests 1, 2, 3 (slots 11, 13, 15)
        inv.setItem(11, questItemBuilder.build(1, data, null));
        inv.setItem(13, questItemBuilder.build(2, data, player));
        inv.setItem(15, questItemBuilder.build(3, data, null));
        
        // Row 3: Quests 4, 5, 6 (slots 29, 31, 33)
        inv.setItem(29, questItemBuilder.build(4, data, null));
        inv.setItem(31, questItemBuilder.build(5, data, null));
        inv.setItem(33, questItemBuilder.build(6, data, null));
        
        // Start reminders for Quest 5 when it's active and not completed
        if (data.getCurrentQuest() == 5 && !data.isQuestCompleted(5)) {
            reminderManager.triggerReminders(player, 5);
        }

        // Quest 5: blink only when the quest is COMPLETE (emerald flashes when completed)
        if (data.isQuestCompleted(5)) {
            // Blink emerald in main menu when Quest 5 is complete
            blinkManager.startBlinking(player, 31, 5, data, true);
        }
        
        // Quest 6: blink and reminders when it's the current quest and not completed
        if (data.getCurrentQuest() == 6 && !data.isQuestCompleted(6)) {
            // Disk should flash while quest 6 is active and not yet completed
            blinkManager.startBlinking(player, 33, 6, data, false);
            reminderManager.triggerReminders(player, 6);
        }
    }
    
    private void addNavbar(Inventory inv, Player player, PlayerData data) {
        Map<String, Object> context = new HashMap<>();
        context.put("menu_name", "Get Started Quests");
        context.put("menu_description", "Complete starter quests to learn about the server");
        context.put("page", 1);
        context.put("total_pages", 1);
        
        int completed = data.getCompletedQuestCount();
        context.put("completed_quests", completed);
        context.put("total_quests", 6);
        context.put("progress_bar", MenuUtils.createProgressBar(completed, 6));
        
        String currentQuestName = data.isCompleted() 
            ? plugin.getMessageManager().get("menu.progress.all-complete")
            : plugin.getQuestManager().getQuestName(data.getCurrentQuest());
        context.put("current_quest", currentQuestName);
        
        // Currency info
        double coins = plugin.getVaultIntegration() != null ? plugin.getVaultIntegration().getBalance(player) : 0;
        context.put("balance", coins);
        context.put("coins", String.format("%.0f", coins));
        context.put("tokens", "0");
        
        navbarRenderer.apply(inv, player, "main", context);
    }
    
    // ==================== UTILITY ====================
    
    private int getQuestFromSlot(int slot) {
        return switch (slot) {
            case 11 -> 1;
            case 13 -> 2;
            case 15 -> 3;
            case 29 -> 4;
            case 31 -> 5;
            case 33 -> 6;
            default -> 0;
        };
    }
    
    /**
     * Show quest objective/progress for informational clicks
     */
    private void showQuestObjective(Player player, int quest, PlayerData data) {
        if (quest == 5) {
            PlayerData.QuestProgress progress = data.getQuestProgress(5);
            int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
            player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("quest-progress.stone-progress", 
                    "current", String.valueOf(stoneMined), "required", "5")));
        } else if (quest == 6) {
            // For Quest 6, just show a hint since it's more complex
            String hint = plugin.getMessageManager().get("quests.hints.quest6");
            player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get("success.quest-hint", "hint", hint)));
        }
    }
    
    /**
     * Check if an inventory belongs to our menu system
     */
    public boolean isQuestMenu(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        String title = inv.getViewers().get(0).getOpenInventory().getTitle();
        return title != null && title.startsWith(MENU_ID);
    }
    
    /**
     * Handle menu close - cleanup
     */
    public void handleClose(Player player) {
        UUID uuid = player.getUniqueId();
        sessionManager.endSession(uuid);
        blinkManager.stopBlinking(uuid);
        reminderManager.cancelAllReminders(player);
    }
    
    private void playSound(Player player) {
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }
    
    /**
     * Callback for auto-complete from reminder manager
     */
    private void onReminderAutoComplete(Player player, int quest) {
        plugin.getQuestManager().completeQuest(player, quest);
        player.closeInventory();
    }
    
    // ==================== ACCESSORS FOR LEGACY COMPATIBILITY ====================
    
    /**
     * Create a progress segment using WDP-Quest resource pack
     */
    private ItemStack createWDPQuestProgressSegment(int segmentIndex, double completion, boolean isHard) {
        // Progress bar constants (EXACTLY matching WDP-Quest)
        final int SEGMENTS = 8;
        final int FILLS_PER_SEGMENT = 5;
        final int TOTAL_UNITS = SEGMENTS * FILLS_PER_SEGMENT; // 40
        
        // Convert percentage to units (0-40)
        int totalFilledUnits = (int) Math.round(completion / 100.0 * TOTAL_UNITS);
        
        // Calculate this segment's fill level (0-5)
        int unitsBeforeThis = segmentIndex * FILLS_PER_SEGMENT;
        int unitsInThisSegment = Math.max(0, Math.min(FILLS_PER_SEGMENT, totalFilledUnits - unitsBeforeThis));
        
        // Use the NEW 1.21+ item model system
        // Each progress level has its own item definition in wdp_quest namespace
        String modelType = isHard ? "hard" : "normal";
        String modelName = "progress_" + modelType + "_" + unitsInThisSegment;
        
        // Create item using any base material (will be replaced by resource pack)
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set the item model component (NEW 1.21.4+ way)
            try {
                item = Bukkit.getItemFactory().createItemStack(
                    "minecraft:paper[minecraft:item_model=\"wdp_quest:" + modelName + "\"]"
                );
                meta = item.getItemMeta();
            } catch (Exception e) {
                // Fallback for older versions
                plugin.getLogger().warning("Failed to set item_model, falling back to plain item: " + e.getMessage());
            }
            
            // Visual feedback in name and lore (fallback without resource pack)
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
     * Create visual segment bar for fallback
     */
    private String createSegmentVisual(int fillLevel, boolean isHard) {
        String filled = isHard ? "§c█" : "§a█";
        String empty = "§7░";
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            bar.append(i < fillLevel ? filled : empty);
        }
        
        return bar.toString();
    }
    
    /**
     * Start blinking animation for quest completion (same as main menu)
     */
    private void startBlinkingAnimation(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        
        // Cancel any existing blinking task FIRST
        if (blinkingTasks.containsKey(uuid)) {
            try {
                plugin.getServer().getScheduler().cancelTask(blinkingTasks.get(uuid));
            } catch (Exception e) {
                // Task already cancelled or invalid
            }
            blinkingTasks.remove(uuid);
        }
        
        // Check if blinking is enabled
        if (!plugin.getConfigManager().isQuest5BlinkingEnabled()) {
            return; // No blinking, just show the completed quest
        }
        
        // Get config values
        final List<Boolean> pattern = plugin.getConfigManager().getQuest5BlinkingPattern();
        final int intervalTicks = plugin.getConfigManager().getQuest5BlinkingIntervalTicks();
        
        // If no pattern defined, use default pattern
        final List<Boolean> finalPattern = pattern.isEmpty() 
            ? List.of(false, true, false, true, true, true, true, true, true, false, true, false, true, true, true, true, true)
            : pattern;
        
        // Start the blinking animation
        int taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int tick = 0;
            
            @Override
            public void run() {
                try {
                    if (!player.isOnline()) {
                        // Player went offline - cancel animation
                        if (blinkingTasks.containsKey(uuid)) {
                            plugin.getServer().getScheduler().cancelTask(blinkingTasks.get(uuid));
                            blinkingTasks.remove(uuid);
                        }
                        return;
                    }
                    
                    Inventory topInv = player.getOpenInventory().getTopInventory();
                    if (!isQuestViewMenu(topInv)) {
                        // Player closed menu - cancel animation
                        if (blinkingTasks.containsKey(uuid)) {
                            plugin.getServer().getScheduler().cancelTask(blinkingTasks.get(uuid));
                            blinkingTasks.remove(uuid);
                        }
                        return;
                    }
                    
                    PlayerData data = plugin.getPlayerDataManager().getData(player);
                    
                    // Get current pattern state
                    boolean shouldGlow = finalPattern.get(tick % finalPattern.size());
                    
                    // Create the quest item (always quest 5 for this view)
                    ItemStack item = quest5Builder.createQuestIcon(data);
                    
                    // Add glow based on pattern
                    if (shouldGlow) {
                        MenuUtils.addGlow(item);
                    }
                    
                    // Update the item in the inventory
                    topInv.setItem(slot, item);
                    
                    // CRITICAL: Update player's inventory view to refresh client-side display
                    player.updateInventory();
                    
                    tick++;
                } catch (Exception e) {
                    plugin.debug("Blinking animation error: " + e.getMessage());
                    // Silently fail if there's an error
                    if (blinkingTasks.containsKey(uuid)) {
                        try {
                            plugin.getServer().getScheduler().cancelTask(blinkingTasks.get(uuid));
                        } catch (Exception ex) {
                            // Already cancelled
                        }
                        blinkingTasks.remove(uuid);
                    }
                }
            }
        }, 0L, intervalTicks).getTaskId(); // Use configurable interval
        
        blinkingTasks.put(uuid, taskId);
    }
    
    /**
     * Check if inventory is a quest view menu
     */
    private boolean isQuestViewMenu(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        String title = inv.getViewers().get(0).getOpenInventory().getTitle();
        return title != null && title.contains("Quest Menu");
    }
    
    public MenuSessionManager getSessionManager() {
        return sessionManager;
    }
    
    public ShopDataLoader getShopDataLoader() {
        return shopDataLoader;
    }
}
