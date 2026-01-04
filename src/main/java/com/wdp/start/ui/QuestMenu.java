package com.wdp.start.ui;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import com.wdp.start.quest.QuestManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Quest Menu GUI - SkillCoins-style navigation
 */
public class QuestMenu {
    
    private final WDPStartPlugin plugin;
    
    // Track open menus
    private final ConcurrentHashMap<UUID, MenuSession> openMenus = new ConcurrentHashMap<>();
    
    // Track Quest 6 reminder tasks
    private final ConcurrentHashMap<UUID, Integer> quest6ReminderTasks = new ConcurrentHashMap<>();
    
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
        
        // Check if current quest is completed and advance if needed
        if (data.isStarted() && !data.isCompleted() && data.isQuestCompleted(data.getCurrentQuest())) {
            plugin.debug("[QuestMenu] Current quest " + data.getCurrentQuest() + " is completed, advancing...");
            data.advanceQuest();
            plugin.getPlayerDataManager().saveData(data);
            plugin.getPlayerDataManager().forceSave(player.getUniqueId());
        }
        
        // Debug log to console
        plugin.debug("[QuestMenu] Opening menu for " + player.getName() + 
            " | Started: " + data.isStarted() + 
            " | CurrentQuest: " + data.getCurrentQuest() + 
            " | Completed: " + data.isCompleted());
        
        String title = hex(plugin.getMessageManager().get("menu.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        // Determine menu type based on quest progress
        if (!data.isStarted()) {
            // Not started - show welcome/start menu
            plugin.debug("[QuestMenu] Showing welcome menu (not started)");
            buildWelcomeMenu(inv);
        } else {
            // Normal menu - show all quests with current progress
            plugin.debug("[QuestMenu] Showing normal menu with progress");
            buildNormalMenu(inv, player, data);
        }
        
        // Add navbar (always present)
        addNavbar(inv, player, data);
        
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
    private void buildWelcomeMenu(Inventory inv) {
        // Center content area
        
        // Welcome title item (slot 13)
        List<String> welcomeLore = plugin.getMessageManager().getList("menu.welcome.description");
        String[] welcomeLoreArray = welcomeLore.stream().map(this::hex).toArray(String[]::new);
        ItemStack welcome = createItem(Material.NETHER_STAR,
            hex(plugin.getMessageManager().get("menu.welcome.title")),
            welcomeLoreArray
        );
        inv.setItem(13, welcome);
        
        // Start button (slot 31)
        List<String> startLore = plugin.getMessageManager().getList("menu.welcome.start-button.lore");
        String[] startLoreArray = startLore.stream().map(this::hex).toArray(String[]::new);
        ItemStack start = createItem(Material.LIME_CONCRETE,
            hex(plugin.getMessageManager().get("menu.welcome.start-button.name")),
            startLoreArray
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
        
        // Bottom row (45-53) is handled by universal navbar
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
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        
        // Status line
        if (progress.isCompleted()) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-status.completed")));
        } else if (data.getCurrentQuest() == quest) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-status.in-progress")));
        } else if (data.getCurrentQuest() > quest) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-status.completed")));
        } else {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-status.locked")));
        }
        
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        lore.add(" ");
        
        // Objective
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.objective")));
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.objective-format", "description", desc)));
        lore.add(" ");
        
        // Progress (if in progress)
        if (data.getCurrentQuest() == quest && !progress.isCompleted()) {
            // Quest 2: Show level percentage instead of step progress
            if (quest == 2 && player != null) {
                int targetLevel = plugin.getConfigManager().getQuest2TargetLevel();
                int levelPercent = getLevelProgressPercent(player, data);
                lore.add(hex(plugin.getMessageManager().get("menu.quest-item.foraging-level", "target", String.valueOf(targetLevel))));
                lore.add(createProgressBar(levelPercent, 100));
                lore.add(" ");
            } else {
                lore.add(hex(plugin.getMessageManager().get("menu.quest-item.progress", 
                    "current", String.valueOf(currentStep), "total", String.valueOf(totalSteps))));
                lore.add(createProgressBar(currentStep, totalSteps));
                lore.add(" ");
            }
        }
        
        // Rewards
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.reward")));
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.reward-coins", "amount", String.valueOf(reward))));
        
        // Extra rewards for specific quests
        if (quest == 1) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.reward-apples")));
        } else if (quest == 6) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.reward-tokens")));
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.reward-items")));
        }
        
        lore.add(" ");
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        
        // Action hint
        if (progress.isCompleted()) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.quest-completed")));
        } else if (data.getCurrentQuest() == quest) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.click-details")));
        } else if (data.getCurrentQuest() > quest) {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.quest-completed")));
        } else {
            lore.add(hex(plugin.getMessageManager().get("menu.quest-item.complete-previous")));
        }
        
        lore.add(hex(plugin.getMessageManager().get("menu.quest-item.divider")));
        
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
     * Add the Quest-style navbar (matching WDP-Quest plugin exactly)
     * Row 5 (slots 45-53): Navigation bar
     */
    private void addNavbar(Inventory inv, Player player, PlayerData data) {
        // Use unified navbar system
        Map<String, Object> context = new HashMap<>();
        context.put("menu_name", "Get Started Quests");
        context.put("menu_description", "Complete starter quests to learn about the server");
        context.put("page", 1);
        context.put("total_pages", 1);
        
        // Add quest-specific info
        int completed = data.getCompletedQuestCount();
        context.put("completed_quests", completed);
        context.put("total_quests", 6);
        
        // Add progress bar
        context.put("progress_bar", createProgressBar(completed, 6));
        
        // Add current quest info
        String currentQuestName = data.isCompleted() 
            ? plugin.getMessageManager().get("menu.progress.all-complete")
            : plugin.getQuestManager().getQuestName(data.getCurrentQuest());
        context.put("current_quest", currentQuestName);
        
        // Add currency info if available
        double coins = 0;
        double tokens = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        // Get tokens from AuraSkills via command output parsing
        if (plugin.getAuraSkillsIntegration() != null && plugin.getAuraSkillsIntegration().isEnabled()) {
            try {
                // Use dispatchCommand to get token balance
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                org.bukkit.command.ConsoleCommandSender consoleSender = Bukkit.getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, "skillcoins check " + player.getName());
                // Try reading from AuraSkills API directly if available
                org.bukkit.plugin.Plugin auraSkills = Bukkit.getPluginManager().getPlugin("AuraSkills");
                if (auraSkills != null) {
                    // Access economy provider through reflection
                    try {
                        Class<?> apiClass = Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
                        Object api = apiClass.getMethod("get").invoke(null);
                        Object economyProvider = apiClass.getMethod("getEconomyProvider").invoke(api);
                        Class<?> currencyTypeClass = Class.forName("dev.aurelium.auraskills.common.skillcoins.CurrencyType");
                        Object tokensEnum = currencyTypeClass.getField("TOKENS").get(null);
                        Object balance = economyProvider.getClass().getMethod("getBalance", java.util.UUID.class, currencyTypeClass)
                            .invoke(economyProvider, player.getUniqueId(), tokensEnum);
                        tokens = ((Number) balance).doubleValue();
                    } catch (Exception reflectionError) {
                        plugin.debug("Could not access AuraSkills economy via reflection: " + reflectionError.getMessage());
                        tokens = 0;
                    }
                }
            } catch (Exception e) {
                plugin.debug("Failed to get token balance: " + e.getMessage());
                tokens = 0;
            }
        }
        context.put("balance", coins);
        context.put("coins", String.format("%.0f", coins)); // For navbar display
        context.put("tokens", String.format("%.0f", tokens));
        
        applyUniversalNavbar(inv, player, "main", context);
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
        
        // ========== TRANSACTION MENU (Quest 3 buy/sell) - Handle before navbar ==========
        if (menuType.startsWith("skillcoins_transaction")) {
            ShopItemData item = transactionItems.get(player.getUniqueId());
            Integer quantity = transactionQuantities.get(player.getUniqueId());
            if (item == null || quantity == null) return;
            
            // +1 button (slot 24)
            if (slot == 24 && quantity < 3) {
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
                    ItemStack itemStack = new ItemStack(item.material, quantity);
                    java.util.Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
                    
                    // Check if items were dropped due to full inventory
                    boolean itemsDropped = !leftovers.isEmpty();

                    // Track spending for refund and analytics
                    com.wdp.start.api.WDPStartAPI.trackCoinSpending(player, (int) Math.round(cost));

                    player.sendMessage(hex(plugin.getMessageManager().get("success.purchased-item",
                            "quantity", String.valueOf(quantity),
                            "item", item.name,
                            "cost", String.format("%.0f", cost))));
                    
                    if (itemsDropped) {
                        player.sendMessage(hex(plugin.getMessageManager().get("errors.inventory-full")));
                    }
                    
                    // Notify quest listener for Quest 3
                    plugin.getQuestListener().onShopItemPurchase(player);
                    
                    // Clear transaction data
                    transactionItems.remove(player.getUniqueId());
                    transactionQuantities.remove(player.getUniqueId());
                    
                    // Reopen main menu after short delay to show updated progress
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        openMainMenu(player);
                    }, 10L);
                } else {
                    player.sendMessage(hex(plugin.getMessageManager().get("errors.not-enough-coins",
                            "amount", String.format("%.0f", cost))));
                }
                return;
            }
            
            // Back button (slot 53) - handle here for transaction menu
            if (slot == 53) {
                // Clear transaction data
                transactionItems.remove(player.getUniqueId());
                transactionQuantities.remove(player.getUniqueId());
                openSimplifiedShopItems(player);
                return;
            }
            return;
        }
        
        // ========== TOKEN EXCHANGE MENU (Quest 4 sub-menu) ==========
        if (menuType.equals("token_exchange")) {
            // Handle token exchange clicks here
            // Confirm button (slot 49)
            if (slot == 49) {
                int tokenCost = plugin.getConfigManager().getQuest4TokenCost();
                double balance = 0;
                if (plugin.getVaultIntegration() != null) {
                    balance = plugin.getVaultIntegration().getBalance(player);
                }
                
                if (balance >= tokenCost) {
                    plugin.getVaultIntegration().withdraw(player, tokenCost);

                    // Give SkillTokens via AuraSkills integration if available
                    if (plugin.getAuraSkillsIntegration() != null) {
                        plugin.getAuraSkillsIntegration().giveSkillTokens(player, 1);
                    }

                    // Track spending and notify systems
                    com.wdp.start.api.WDPStartAPI.trackCoinSpending(player, tokenCost);
                    com.wdp.start.api.WDPStartAPI.notifyTokenPurchase(player, 1);

                    player.sendMessage(hex(plugin.getMessageManager().get("success.purchased-token",
                            "quantity", "1",
                            "cost", String.valueOf(tokenCost))));
                    
                    // Notify quest listener for Quest 4
                    plugin.getQuestListener().onTokenPurchase(player, 1);
                    
                    // Reopen main menu after short delay to show updated progress
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        openMainMenu(player);
                    }, 10L);
                } else {
                    player.sendMessage(hex(plugin.getMessageManager().get("errors.not-enough-coins",
                            "amount", String.valueOf(tokenCost))));
                }
                return;
            }
            
            // Back button (slot 53) for token exchange
            if (slot == 53) {
                openSimplifiedShop(player);
                return;
            }
            
            // Handle quantity adjustments for token exchange
            // This would need to be implemented based on the token exchange menu layout
            return;
        }
        
        // Handle universal navbar clicks (slots 45-53) - AFTER specific menu handling
        if (slot >= 45 && slot <= 53) {
            if (slot == 45) { // Back button
                if ("quest_detail".equals(menuType)) {
                    openSimplifiedQuestView(player);
                } else if (menuType.startsWith("skillcoins_shop_section")) {
                    openSimplifiedShopItems(player);
                }
                return;
            }
            if (slot == 53) { // Close/Back button
                // For shop menus, go back instead of closing
                if (menuType.startsWith("skillcoins_shop_section")) {
                    openSimplifiedShopItems(player);
                } else if (menuType.equals("skillcoins_transaction")) {
                    // Get category from transaction data
                    ShopItemData item = transactionItems.get(player.getUniqueId());
                    if (item != null) {
                        // Go back to section - need to find section name from item
                        openSimplifiedShopItems(player); // For now just go to main
                    } else {
                        player.closeInventory();
                    }
                } else if (menuType.equals("skillcoins_token_exchange")) {
                    openSimplifiedShop(player);
                } else if (menuType.equals("skillcoins_shop_main")) {
                    openMainMenu(player); // Go back to main quest menu from shop
                } else {
                    player.closeInventory();
                }
                return;
            }
            // Other navbar slots are decorative
            return;
        }
        
        // ========== SKILLCOINS SHOP MAIN MENU (Quest 3) ==========
        if (menuType.equals("skillcoins_shop_main")) {
            // Check player's current quest for token exchange handling
            boolean isQuest4 = data.getCurrentQuest() == 4 && !data.isQuestCompleted(4);
            
            // Dynamically resolve category files by slot from SkillCoinsShop/sections/
            File sectionsDir = new File(plugin.getDataFolder(), "SkillCoinsShop/sections");
            if (sectionsDir.exists() && sectionsDir.isDirectory()) {
                File[] sectionFiles = sectionsDir.listFiles((dir, name) -> name.endsWith(".yml"));
                if (sectionFiles != null) {
                    for (File sectionFile : sectionFiles) {
                        try {
                            YamlConfiguration sec = YamlConfiguration.loadConfiguration(sectionFile);
                            int secSlot = sec.getInt("slot", -1);
                            String display = sec.getString("displayname", sectionFile.getName().replace(".yml", ""));
                            if (secSlot == slot) {
                                String fname = sectionFile.getName().replace(".yml", "").toLowerCase();
                                // Handle token exchange differently for quest 4
                                if (fname.contains("tokenexchange")) {
                                    if (isQuest4) {
                                        openTokenExchange(player);
                                        return;
                                    } else {
                                        player.sendMessage(hex(plugin.getMessageManager().get("errors.token-not-available")));
                                        return;
                                    }
                                }
                                // Skip skilllevels/tokens - not available in simplified view
                                if (fname.contains("skilllevels") || fname.contains("tokens")) {
                                    player.sendMessage(hex(plugin.getMessageManager().get("errors.feature-not-available")));
                                    return;
                                }

                                String materialName = sec.getString("material", "STONE");
                                Material icon = Material.matchMaterial(materialName);
                                if (icon == null) icon = Material.STONE;
                                // Strip color codes from display name for file lookup
                                String cleanName = ChatColor.stripColor(hex(display));
                                openShopSection(player, cleanName, icon);
                                return;
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load section for click: " + sectionFile.getName() + " - " + e.getMessage());
                        }
                    }
                }
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
        
        // ========== SKILLCOINS SHOP FOR TOKENS (Quest 4 main) ==========
        if (menuType.equals("skillcoins_shop_tokens")) {
            // Dynamically resolve token exchange button by checking sections
            File sectionsDir = new File(plugin.getDataFolder(), "SkillCoinsShop/sections");
            if (sectionsDir.exists() && sectionsDir.isDirectory()) {
                File[] sectionFiles = sectionsDir.listFiles((dir, name) -> name.endsWith(".yml"));
                if (sectionFiles != null) {
                    for (File sectionFile : sectionFiles) {
                        try {
                            YamlConfiguration sec = YamlConfiguration.loadConfiguration(sectionFile);
                            int secSlot = sec.getInt("slot", -1);
                            if (secSlot == slot) {
                                String fname = sectionFile.getName().replace(".yml", "").toLowerCase();
                                if (fname.contains("tokenexchange")) {
                                    openTokenExchange(player);
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load section for click: " + sectionFile.getName() + " - " + e.getMessage());
                        }
                    }
                }
            }
            
            // Fallback: if no dynamic resolution worked, check hardcoded slots for backwards compatibility
            if (slot == 31) {
                openTokenExchange(player);
                return;
            }
            
            // Other categories are NOT clickable (greyed out)
            player.sendMessage(hex(plugin.getMessageManager().get("errors.section-not-available")));
            return;
        }
        
        // ========== SIMPLIFIED QUEST MENU (Quest 5) ==========
        if (menuType.equals("simplified_quest")) {
            // Quest icon click (slot 9) - recalculate progress, complete if ready, otherwise refresh
            if (slot == 9) {
                PlayerData.QuestProgress progress = data.getQuestProgress(5);
                int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
                if (stoneMined >= 5) {
                    // Quest is complete - complete it instantly
                    plugin.getQuestManager().completeQuest(player, 5);
                    player.sendMessage(hex(plugin.getMessageManager().get("success.quest-complete")));
                    player.closeInventory();
                } else {
                    // Quest not complete - show progress feedback and refresh the view
                    player.sendMessage(hex(plugin.getMessageManager().get("quest-progress.stone-progress", 
                            "current", String.valueOf(stoneMined), 
                            "required", "5")));
                    
                    // Refresh the menu to show latest progress bar
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        openSimplifiedQuestView(player);
                    }, 2L);
                }
                return;
            }
            
            // Progress bar click (slots 10-17) - also refresh/show progress
            if (slot >= 10 && slot <= 17) {
                PlayerData.QuestProgress progress = data.getQuestProgress(5);
                int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
                player.sendMessage(hex(plugin.getMessageManager().get("quest-progress.stone-progress", 
                        "current", String.valueOf(stoneMined), 
                        "required", "5")));
                return;
            }
            
            // Navbar handling
            if (slot == 45) { // Back button
                openMainMenu(player);
                return;
            }
            if (slot == 53) { // Close button
                player.closeInventory();
                return;
            }
            
            return;
        }
        
        // ========== QUEST DETAIL MENU ==========
        if (menuType.equals("quest_detail")) {
            // No specific clicks needed - navbar handles back/close
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
                    ItemStack itemStack = new ItemStack(itemMat, itemAmount);
                    java.util.Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
                    
                    // Check if items were dropped due to full inventory
                    boolean itemsDropped = !leftovers.isEmpty();
                    
                    player.sendMessage(hex(plugin.getMessageManager().get("success.purchased-item",
                            "quantity", "1", "item", itemName, "cost", String.valueOf(cost))));
                    
                    if (itemsDropped) {
                        player.sendMessage(hex(plugin.getMessageManager().get("errors.inventory-full")));
                    }
                    
                    plugin.getQuestListener().onShopItemPurchase(player);
                    
                    // Reopen main menu after short delay to show updated progress
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        openMainMenu(player);
                    }, 10L);
                } else {
                    player.sendMessage(hex(plugin.getMessageManager().get("errors.not-enough-coins",
                            "amount", String.valueOf(cost))));
                }
            }
            return;
        }
        
        // Simplified token exchange menu (Quest 4) - legacy
        if (menuType.equals("simplified_shop") && slot == 31) {
            plugin.getQuestListener().onTokenPurchase(player, 1);
            player.sendMessage(hex(plugin.getMessageManager().get("success.purchased-token",
                    "quantity", "1", "cost", "100")));
            
            // Reopen main menu after short delay to show updated progress
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                openMainMenu(player);
            }, 10L);
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
                    player.sendMessage(hex(plugin.getMessageManager().get("errors.mine-stone-first",
                            "current", String.valueOf(stoneMined))));
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
        
        // Cancel button (slot 45)
        if (slot == 45 && data.isStarted() && !data.isCompleted()) {
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
        
        // Quest 6: Check if already completed to prevent multiple rewards
        if (quest == 6) {
            PlayerData data = plugin.getPlayerDataManager().getData(player);
            PlayerData.QuestProgress progress = data.getQuestProgress(6);
            
            // Prevent multiple completions
            if (progress.isCompleted()) {
                player.sendMessage(hex(plugin.getMessageManager().get("quest.already-completed")));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            
            // Complete Quest 6
            plugin.getQuestManager().completeQuest(player, 6);
            player.sendMessage(hex(plugin.getMessageManager().get("success.quest-complete-welcome")));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            // Cancel any pending reminder tasks
            cancelQuest6Reminders(player);
            return;
        }
        
        String hint = switch (quest) {
            case 1 -> plugin.getMessageManager().get("quests.hints.quest1");
            case 2 -> plugin.getMessageManager().get("quests.hints.quest2");
            case 3 -> plugin.getMessageManager().get("quests.hints.quest3");
            case 4 -> plugin.getMessageManager().get("quests.hints.quest4");
            case 5 -> plugin.getMessageManager().get("quests.hints.quest5");
            default -> plugin.getMessageManager().get("quests.hints.default");
        };
        
        player.sendMessage(hex(plugin.getMessageManager().get("success.quest-hint", "hint", hint)));
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
        // Cancel Quest 6 reminders when menu is closed
        cancelQuest6Reminders(player);
    }
    
    /**
     * Start Quest 6 reminder and auto-complete system
     */
    private void startQuest6Reminders(Player player) {
        // Cancel any existing reminders first
        cancelQuest6Reminders(player);
        
        UUID uuid = player.getUniqueId();
        
        // Send immediate instruction
        player.sendMessage(hex(plugin.getMessageManager().get("quest6.instruction")));
        player.sendMessage(hex(plugin.getMessageManager().get("quest6.instruction-detail")));
        
        // Schedule reminder at 10 seconds
        int task1 = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                PlayerData data = plugin.getPlayerDataManager().getData(player);
                if (data.getCurrentQuest() == 6 && !data.isQuestCompleted(6)) {
                    player.sendMessage(hex(plugin.getMessageManager().get("quest6.reminder-10s")));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
                }
            }
        }, 200L).getTaskId(); // 10 seconds
        
        // Schedule reminder at 30 seconds
        int task2 = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                PlayerData data = plugin.getPlayerDataManager().getData(player);
                if (data.getCurrentQuest() == 6 && !data.isQuestCompleted(6)) {
                    player.sendMessage(hex(plugin.getMessageManager().get("quest6.reminder-30s")));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.7f);
                }
            }
        }, 600L).getTaskId(); // 30 seconds
        
        // Schedule auto-complete at 35 seconds
        int task3 = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                PlayerData data = plugin.getPlayerDataManager().getData(player);
                if (data.getCurrentQuest() == 6 && !data.isQuestCompleted(6)) {
                    player.sendMessage(hex(plugin.getMessageManager().get("quest6.auto-complete")));
                    plugin.getQuestManager().completeQuest(player, 6);
                    player.closeInventory();
                }
            }
            quest6ReminderTasks.remove(uuid);
        }, 700L).getTaskId(); // 35 seconds
        
        // Store the tasks for later cancellation
        quest6ReminderTasks.put(uuid, task1); // We'll cancel all scheduled tasks below
    }
    
    /**
     * Cancel Quest 6 reminder tasks for a player
     */
    private void cancelQuest6Reminders(Player player) {
        UUID uuid = player.getUniqueId();
        if (quest6ReminderTasks.containsKey(uuid)) {
            // Cancel all scheduled tasks for this player
            plugin.getServer().getScheduler().getPendingTasks().stream()
                .filter(task -> task.getOwner().equals(plugin) && 
                        task.getTaskId() >= quest6ReminderTasks.get(uuid))
                .forEach(task -> plugin.getServer().getScheduler().cancelTask(task.getTaskId()));
            
            quest6ReminderTasks.remove(uuid);
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
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

    // --- AURASKILLS shop integration helpers ---
    private File findAuraShopBaseDir() {
        // Use WDP-Start's own SkillCoinsShop directory
        File base = new File(plugin.getDataFolder(), "SkillCoinsShop");
        if (base.exists() && base.isDirectory()) {
            return base;
        }
        return null;
    }

    private List<File> listSectionFiles() {
        File base = findAuraShopBaseDir();
        if (base == null) return Collections.emptyList();
        File sections = new File(base, "sections");
        if (!sections.exists() || !sections.isDirectory()) return Collections.emptyList();
        File[] files = sections.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return Collections.emptyList();
        // Sort by slot if available
        List<File> fileList = Arrays.stream(files).collect(Collectors.toList());
        fileList.sort(Comparator.comparingInt(f -> getSectionSlotSafe(f)));
        return fileList;
    }

    private int getSectionSlotSafe(File sectionFile) {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(sectionFile);
            return cfg.contains("slot") ? cfg.getInt("slot") : 999;
        } catch (Exception e) {
            return 999;
        }
    }

    private List<ShopItemData> loadShopPageItems(String sectionName) {
        File base = findAuraShopBaseDir();
        if (base == null) return Collections.emptyList();
        File shops = new File(base, "shops");
        if (!shops.exists() || !shops.isDirectory()) return Collections.emptyList();
        File shopFile = new File(shops, sectionName + ".yml");
        if (!shopFile.exists()) {
            // Try several common filename variants
            List<String> variants = Arrays.asList(
                sectionName,
                sectionName.replace(" ", ""),
                sectionName.toLowerCase(),
                sectionName.substring(0,1).toUpperCase() + (sectionName.length() > 1 ? sectionName.substring(1) : ""),
                sectionName.substring(0,1).toUpperCase() + sectionName.substring(1).toLowerCase()
            );
            boolean found = false;
            for (String v : variants) {
                File f = new File(shops, v + ".yml");
                if (f.exists()) {
                    shopFile = f;
                    found = true;
                    break;
                }
            }
            if (!found) return Collections.emptyList();
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);
        if (!cfg.contains("pages")) return Collections.emptyList();
        String firstPage = "page1";
        if (!cfg.contains("pages." + firstPage + ".items")) {
            // If pages are named differently, pick first key
            if (cfg.getConfigurationSection("pages") != null) {
                for (String key : cfg.getConfigurationSection("pages").getKeys(false)) {
                    if (cfg.contains("pages." + key + ".items")) {
                        firstPage = key;
                        break;
                    }
                }
            }
        }

        if (!cfg.contains("pages." + firstPage + ".items")) return Collections.emptyList();

        List<ShopItemData> items = new ArrayList<>();
        Map<String, Object> itemsSection = cfg.getConfigurationSection("pages." + firstPage + ".items").getValues(false);

        // Sort keys numerically if possible
        List<String> keys = new ArrayList<>(itemsSection.keySet());
        keys.sort((a, b) -> {
            try { return Integer.compare(Integer.parseInt(a), Integer.parseInt(b)); } catch (Exception e) { return a.compareTo(b); }
        });

        for (String key : keys) {
            Object val = itemsSection.get(key);
            if (!(val instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> itemCfg = (Map<String, Object>) val;
            String materialStr = (String) itemCfg.get("material");
            if (materialStr == null) continue;
            Material mat = Material.getMaterial(materialStr.toUpperCase());
            if (mat == null) continue;
            double buy = itemCfg.containsKey("buy") ? Double.parseDouble(String.valueOf(itemCfg.get("buy"))) : 0;
            double sell = itemCfg.containsKey("sell") ? Double.parseDouble(String.valueOf(itemCfg.get("sell"))) : 0;
            String prettyName = prettifyMaterialName(mat);
            items.add(new ShopItemData(mat, prettyName, buy, sell, sell > 0));
        }

        return items;
    }

    private int loadShopItemCount(String sectionName) {
        return loadShopPageItems(sectionName).size();
    }

    private String prettifyMaterialName(Material mat) {
        String s = mat.name().toLowerCase().replace('_', ' ');
        String[] parts = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }    
    /**
     * Open simplified shop menu for Quest 3 - EXACT SkillCoins ShopMainMenu style
     * Shows item categories (NOT token exchange) - player must buy an item
     */
    public void openSimplifiedShopItems(Player player) {
        String title = hex(plugin.getMessageManager().get("shop.main.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        

        
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
        double tokens = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        // Get tokens from AuraSkills
        if (plugin.getAuraSkillsIntegration() != null && plugin.getAuraSkillsIntegration().isEnabled()) {
            try {
                org.bukkit.plugin.Plugin auraSkills = Bukkit.getPluginManager().getPlugin("AuraSkills");
                if (auraSkills != null) {
                    try {
                        Class<?> apiClass = Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
                        Object api = apiClass.getMethod("get").invoke(null);
                        Object economyProvider = apiClass.getMethod("getEconomyProvider").invoke(api);
                        Class<?> currencyTypeClass = Class.forName("dev.aurelium.auraskills.common.skillcoins.CurrencyType");
                        Object tokensEnum = currencyTypeClass.getField("TOKENS").get(null);
                        Object balance = economyProvider.getClass().getMethod("getBalance", java.util.UUID.class, currencyTypeClass)
                            .invoke(economyProvider, player.getUniqueId(), tokensEnum);
                        tokens = ((Number) balance).doubleValue();
                    } catch (Exception reflectionError) {
                        tokens = -1;
                    }
                }
            } catch (Exception e) {
                tokens = -1;
            }
        }
        
        // Player head (slot 0) - EXACT SkillCoins style
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = playerHead.getItemMeta();
        if (headMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) headMeta;
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.player-head.name",
                    "player", player.getName())));
            List<String> headLore = plugin.getMessageManager().getList("shop.main.player-head.lore");
            skullMeta.setLore(headLore.stream().map(this::hex).toList());
            playerHead.setItemMeta(skullMeta);
        }
        inv.setItem(0, playerHead);
        
        // Balance display (slot 8) - EXACT SkillCoins style
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        if (balanceMeta != null) {
            balanceMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.balance.name")));
            List<String> balanceLore = plugin.getMessageManager().getList("shop.main.balance.lore",
                    "coins", String.format("%.0f", coins));
            balanceMeta.setLore(balanceLore.stream().map(this::hex).toList());
            balanceItem.setItemMeta(balanceMeta);
        }
        inv.setItem(8, balanceItem);
        
        // Load sections from SkillCoinsShop/sections/*.yml files
        File sectionsDir = new File(plugin.getDataFolder(), "SkillCoinsShop/sections");
        if (sectionsDir.exists() && sectionsDir.isDirectory()) {
            File[] sectionFiles = sectionsDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (sectionFiles != null) {
                for (File sectionFile : sectionFiles) {
                    try {
                        YamlConfiguration sec = YamlConfiguration.loadConfiguration(sectionFile);
                        boolean enabled = sec.getBoolean("enable", true);
                        if (!enabled) continue;
                        
                        String display = sec.getString("displayname", sectionFile.getName().replace(".yml", ""));
                        int slot = sec.getInt("slot", -1);
                        String materialName = sec.getString("material", "STONE");
                        
                        // Skip invalid slots
                        if (slot < 0 || slot >= 54) continue;
                        
                        // Skip SkillLevels and Tokens sections for simplified view
                        String fname = sectionFile.getName().replace(".yml", "").toLowerCase();
                        if (fname.contains("skilllevels") || fname.contains("tokens")) continue;
                        
                        Material icon = Material.matchMaterial(materialName);
                        if (icon == null) icon = Material.STONE;
                        
                        // Strip color codes from display name to get clean section name for file lookup
                        String cleanName = ChatColor.stripColor(hex(display));
                        
                        // Load item count from corresponding shop file
                        int itemCount = loadShopItemCount(cleanName);
                        // Ensure count shows at least 1 even if shop file isn't found (fallback items)
                        if (itemCount == 0) {
                            itemCount = getShopItemsForCategory(cleanName).size();
                        }
                        
                        inv.setItem(slot, createShopCategory(icon, 
                                hex(display),
                                "Open " + cleanName + " shop",
                                itemCount, true));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load section: " + sectionFile.getName() + " - " + e.getMessage());
                    }
                }
            }
        }
        
        // Token Exchange button (slot 38) - hardcoded for simplified view
        ItemStack tokenItem = new ItemStack(Material.EMERALD);
        ItemMeta tokenMeta = tokenItem.getItemMeta();
        if (tokenMeta != null) {
            tokenMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.token-exchange.name")));
            List<String> tokenLore = plugin.getMessageManager().getList("shop.main.token-exchange.lore");
            tokenMeta.setLore(tokenLore.stream().map(this::hex).toList());
            tokenItem.setItemMeta(tokenMeta);
        }
        inv.setItem(38, tokenItem);
        
        // Close button (slot 53) - EXACT SkillCoins style
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.close.name")));
            List<String> closeLore = plugin.getMessageManager().getList("shop.main.close.lore");
            closeMeta.setLore(closeLore.stream().map(this::hex).toList());
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
        
        // Add shop items based on category - load one page from AuraSkills shops if available
        List<ShopItemData> items = getShopItemsForCategory(category);
        int slot = 0;
        for (ShopItemData item : items) {
            if (slot >= 45) break; // leave navbar row free for applyUniversalNavbar
            inv.setItem(slot, createShopItemDisplay(item));
            slot++;
        }

        // Get balance
        double coins = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }

        // Apply navbar row (page info / balance / back buttons) using universal navbar
        Map<String, Object> context = new HashMap<>();
        context.put("page", 1);
        context.put("total_pages", 1);
        context.put("menu_name", category + " Shop");
        context.put("menu_description", "Shop category: " + category);
        context.put("balance", (int) Math.round(coins));
        context.put("previous_menu", "main_shop"); // Add previous_menu to show Back instead of Close
        applyUniversalNavbar(inv, player, "shop_section", context);
        
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
        String title = hex(plugin.getMessageManager().get(isBuying ? "shop.transaction.title-buy" : "shop.transaction.title-sell",
                "item", item.name));
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
        
        // Get current quantity from stored data, default to 1
        int quantity = transactionQuantities.getOrDefault(player.getUniqueId(), 1);
        double totalPrice = item.buyPrice * quantity;
        
        // Item display (slot 13)
        ItemStack display = new ItemStack(item.material, quantity);
        ItemMeta displayMeta = display.getItemMeta();
        if (displayMeta != null) {
            displayMeta.setDisplayName(ChatColor.of("#FFFFFF") + item.name);
            List<String> displayLore = new ArrayList<>();
            displayLore.add("");
            displayLore.add(hex(plugin.getMessageManager().get("shop.transaction.quantity", "quantity", String.valueOf(quantity))));
            displayLore.add(hex(plugin.getMessageManager().get("shop.transaction.price-per", "price", String.format("%.0f", item.buyPrice))));
            displayLore.add("");
            displayLore.add(hex(plugin.getMessageManager().get("shop.transaction.total-cost", "total", String.format("%.0f", totalPrice))));
            displayLore.add("");
            if (coins >= totalPrice) {
                displayLore.add(hex(plugin.getMessageManager().get("shop.transaction.sufficient-funds")));
            } else {
                displayLore.add(hex(plugin.getMessageManager().get("shop.transaction.insufficient-funds")));
            }
            displayMeta.setLore(displayLore);
            display.setItemMeta(displayMeta);
        }
        inv.setItem(13, display);
        
        // +1 button (slot 24) - only if under max (3)
        if (quantity < 3) {
            ItemStack plus1 = new ItemStack(Material.LIME_TERRACOTTA);
            ItemMeta plus1Meta = plus1.getItemMeta();
            if (plus1Meta != null) {
                plus1Meta.setDisplayName(hex(plugin.getMessageManager().get("shop.transaction.plus-one.name")));
                List<String> plus1Lore = plugin.getMessageManager().getList("shop.transaction.plus-one.lore");
                plus1Meta.setLore(plus1Lore.stream().map(this::hex).toList());
                plus1.setItemMeta(plus1Meta);
            }
            inv.setItem(24, plus1);
        }
        
        // -1 button (slot 20) - only if above minimum (1)
        if (quantity > 1) {
            ItemStack minus1 = new ItemStack(Material.RED_TERRACOTTA);
            ItemMeta minus1Meta = minus1.getItemMeta();
            if (minus1Meta != null) {
                minus1Meta.setDisplayName(hex(plugin.getMessageManager().get("shop.transaction.minus-one.name")));
                List<String> minus1Lore = plugin.getMessageManager().getList("shop.transaction.minus-one.lore");
                minus1Meta.setLore(minus1Lore.stream().map(this::hex).toList());
                minus1.setItemMeta(minus1Meta);
            }
            inv.setItem(20, minus1);
        }
        
        // Quantity display (slot 22)
        ItemStack qtyDisplay = new ItemStack(Material.PAPER, quantity);
        ItemMeta qtyMeta = qtyDisplay.getItemMeta();
        if (qtyMeta != null) {
            qtyMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.transaction.quantity-display.name", 
                    "quantity", String.valueOf(quantity))));
            qtyDisplay.setItemMeta(qtyMeta);
        }
        inv.setItem(22, qtyDisplay);
        
        // Confirm button (slot 49)
        boolean canAfford = coins >= totalPrice;
        ItemStack confirm = new ItemStack(canAfford ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            if (canAfford) {
                confirmMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.transaction.confirm.can-buy.name")));
                List<String> confirmLore = plugin.getMessageManager().getList("shop.transaction.confirm.can-buy.lore",
                        "quantity", String.valueOf(quantity),
                        "item", item.name,
                        "total", String.format("%.0f", totalPrice));
                confirmMeta.setLore(confirmLore.stream().map(this::hex).toList());
            } else {
                confirmMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.transaction.confirm.cannot-buy.name")));
                List<String> confirmLore = plugin.getMessageManager().getList("shop.transaction.confirm.cannot-buy.lore");
                confirmMeta.setLore(confirmLore.stream().map(this::hex).toList());
            }
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(49, confirm);
        
        // Back button (slot 53)
        ItemStack back = new ItemStack(Material.SPYGLASS);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.transaction.back.name")));
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
        String title = hex(plugin.getMessageManager().get("shop.main.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
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
            skullMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.player-head.name",
                    "player", player.getName())));
            List<String> headLore = plugin.getMessageManager().getList("shop.main.player-head.lore");
            skullMeta.setLore(headLore.stream().map(this::hex).toList());
            playerHead.setItemMeta(skullMeta);
        }
        inv.setItem(0, playerHead);
        
        // Balance display (slot 8)
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        if (balanceMeta != null) {
            balanceMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.balance.name")));
            List<String> balanceLore = plugin.getMessageManager().getList("shop.main.balance.lore",
                    "coins", String.format("%.0f", coins));
            balanceMeta.setLore(balanceLore.stream().map(this::hex).toList());
            balanceItem.setItemMeta(balanceMeta);
        }
        inv.setItem(8, balanceItem);
        
        // Shop categories - mirror AuraSkills sections but greyed out and skipping one row
        List<File> sections = listSectionFiles();
        for (File sectionFile : sections) {
            try {
                YamlConfiguration sec = YamlConfiguration.loadConfiguration(sectionFile);
                boolean enabled = sec.getBoolean("enable", true);
                if (!enabled) continue;
                String display = sec.getString("displayname", sectionFile.getName().replace(".yml", ""));
                int slot = sec.getInt("slot", -1);

                if (slot < 0 || slot >= 54) continue;
                // Skip the removed row (18-26)
                if (slot >= 18 && slot <= 26) continue;

                // Keep TokenExchange clickable if present
                String fname = sectionFile.getName().replace(".yml", "").toLowerCase();
                if (fname.contains("tokenexchange")) {
                    ItemStack tokenExchange = new ItemStack(Material.EMERALD);
                    ItemMeta tokenMeta = tokenExchange.getItemMeta();
                    if (tokenMeta != null) {
                        tokenMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.main.token-exchange.name")));
                        List<String> tokenLore = plugin.getMessageManager().getList("shop.main.token-exchange.lore");
                        tokenMeta.setLore(tokenLore.stream().map(this::hex).toList());
                        tokenExchange.setItemMeta(tokenMeta);
                    }
                    addGlow(tokenExchange);
                    inv.setItem(slot, tokenExchange);
                    continue;
                }

                // Skip SkillLevels and Tokens as requested
                if (fname.contains("skilllevels") || fname.contains("tokens")) continue;

                Material icon = Material.matchMaterial(sec.getString("material", "STONE"));
                if (icon == null) icon = Material.STONE;
                int itemCount = loadShopPageItems(display).size();
                inv.setItem(slot, createShopCategory(icon, ChatColor.of("#777777") + display, 
                        plugin.getMessageManager().get("shop.category.not-available"), Math.max(1, itemCount), false));
            } catch (Exception e) {
                // ignore
            }
        }
        
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
        String title = hex(plugin.getMessageManager().get("shop.token-exchange.title"));
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
            tokenMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.token-exchange.token-display.name")));
            List<String> lore = plugin.getMessageManager().getList("shop.token-exchange.token-display.lore",
                    "quantity", String.valueOf(quantity),
                    "cost", String.valueOf(tokenCost),
                    "total", String.valueOf(totalCost));
            List<String> processedLore = new ArrayList<>(lore.stream().map(this::hex).toList());
            if (coinBalance >= totalCost) {
                processedLore.add(hex(plugin.getMessageManager().get("shop.transaction.sufficient-funds")));
            } else {
                processedLore.add(hex(plugin.getMessageManager().get("shop.transaction.insufficient-funds")));
            }
            tokenMeta.setLore(processedLore);
            tokenDisplay.setItemMeta(tokenMeta);
        }
        inv.setItem(13, tokenDisplay);
        
        // ONLY +1 button (slot 24) - other buttons removed for tutorial
        ItemStack plus1 = new ItemStack(Material.LIME_TERRACOTTA);
        ItemMeta plus1Meta = plus1.getItemMeta();
        if (plus1Meta != null) {
            plus1Meta.setDisplayName(hex(plugin.getMessageManager().get("shop.token-exchange.plus-one.name")));
            List<String> plus1Lore = plugin.getMessageManager().getList("shop.token-exchange.plus-one.lore");
            plus1Meta.setLore(plus1Lore.stream().map(this::hex).toList());
            plus1.setItemMeta(plus1Meta);
        }
        inv.setItem(24, plus1);
        
        // Quantity display (slot 22)
        ItemStack qtyDisplay = new ItemStack(Material.PAPER, 1);
        ItemMeta qtyMeta = qtyDisplay.getItemMeta();
        if (qtyMeta != null) {
            qtyMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.token-exchange.quantity-display.name",
                    "quantity", String.valueOf(quantity))));
            List<String> qtyLore = plugin.getMessageManager().getList("shop.token-exchange.quantity-display.lore");
            qtyMeta.setLore(qtyLore.stream().map(this::hex).toList());
            qtyDisplay.setItemMeta(qtyMeta);
        }
        inv.setItem(22, qtyDisplay);
        
        // Confirm button (slot 49)
        boolean canAfford = coinBalance >= totalCost;
        ItemStack confirm = new ItemStack(canAfford ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            if (canAfford) {
                confirmMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.token-exchange.confirm.can-buy.name")));
                List<String> confirmLore = plugin.getMessageManager().getList("shop.token-exchange.confirm.can-buy.lore",
                        "quantity", String.valueOf(quantity),
                        "total", String.valueOf(totalCost));
                confirmMeta.setLore(confirmLore.stream().map(this::hex).toList());
            } else {
                confirmMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.token-exchange.confirm.cannot-buy.name")));
                List<String> confirmLore = plugin.getMessageManager().getList("shop.token-exchange.confirm.cannot-buy.lore");
                confirmMeta.setLore(confirmLore.stream().map(this::hex).toList());
            }
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(49, confirm);

        // Balance display (slot 45)
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        if (balanceMeta != null) {
            balanceMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.token-exchange.balance.name")));
            List<String> balanceLore = plugin.getMessageManager().getList("shop.token-exchange.balance.lore",
                    "coins", String.format("%.0f", coinBalance),
                    "required", String.valueOf(totalCost));
            balanceMeta.setLore(balanceLore.stream().map(this::hex).toList());
            balanceItem.setItemMeta(balanceMeta);
        }
        inv.setItem(45, balanceItem);
        
        // Back button (slot 53)
        ItemStack back = new ItemStack(Material.SPYGLASS);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(hex(plugin.getMessageManager().get("shop.token-exchange.back.name")));
            List<String> backLore = plugin.getMessageManager().getList("shop.token-exchange.back.lore");
            backMeta.setLore(backLore.stream().map(this::hex).toList());
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
     * Open simplified quest menu for Quest 5 - EXACT WDP-Quest main menu structure
     * Double chest with ONE quest + 8 progress bar segments using Custom Model Data
     * Universal navbar from navbar.yml
     */
    public void openSimplifiedQuestView(Player player) {
        String title = hex(plugin.getMessageManager().get("quest-view.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        PlayerData.QuestProgress progress = data.getQuestProgress(5);
        
        // Get stone mining progress
        int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
        int stoneTarget = 5;
        double completion = Math.min(100.0, (stoneMined * 100.0) / stoneTarget);
        boolean isComplete = stoneMined >= stoneTarget;
        
        // Get player balance
        double coins = 0;
        int tokens = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        // === HEADER ROW (Row 0: slots 0-8) ===
        // Player head (slot 0)
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = playerHead.getItemMeta();
        if (headMeta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(hex(plugin.getMessageManager().get("quest-view.player-head.name",
                    "player", player.getName())));
            List<String> headLore = plugin.getMessageManager().getList("quest-view.player-head.lore",
                    "coins", String.format("%.0f", coins),
                    "tokens", String.valueOf(tokens));
            skullMeta.setLore(headLore.stream().map(this::hex).toList());
            playerHead.setItemMeta(skullMeta);
        }
        inv.setItem(0, playerHead);
        
        // Title (slot 4)
        List<String> titleLore = plugin.getMessageManager().getList("quest-view.title-item.lore");
        inv.setItem(4, createItem(
            Material.BOOK,
            hex(plugin.getMessageManager().get("quest-view.title-item.name")),
            titleLore.stream().map(this::hex).toArray(String[]::new)
        ));
        
        // Progress display (slot 8)
        List<String> progressLore = plugin.getMessageManager().getList("quest-view.progress-item.lore",
                "progress", String.format("%.0f", completion));
        inv.setItem(8, createItem(
            Material.EXPERIENCE_BOTTLE,
            hex(plugin.getMessageManager().get("quest-view.progress-item.name")),
            progressLore.stream().map(this::hex).toArray(String[]::new)
        ));
        
        // === QUEST ROW (Row 1: slots 9-17) ===
        // Quest icon (slot 9)
        String questName = isComplete 
            ? hex(plugin.getMessageManager().get("quest-view.quest-icon.complete.name"))
            : hex(plugin.getMessageManager().get("quest-view.quest-icon.incomplete.name"));
        List<String> questLore = plugin.getMessageManager().getList("quest-view.quest-icon.lore",
                "current", String.valueOf(stoneMined),
                "total", String.valueOf(stoneTarget));
        ItemStack questIcon = createItem(
            isComplete ? Material.EMERALD : Material.COMPASS,
            questName,
            questLore.stream().map(this::hex).toArray(String[]::new)
        );
        if (isComplete) addGlow(questIcon);
        inv.setItem(9, questIcon);
        
        // Progress bar (8 segments, slots 10-17)
        for (int seg = 0; seg < 8; seg++) {
            inv.setItem(10 + seg, createWDPQuestProgressSegment(seg, completion, false));
        }
        
        // === SEPARATOR (Row 2: slots 18-26) ===
        fillRow(inv, 2, Material.GRAY_STAINED_GLASS_PANE);
        
        // === EMPTY QUEST SLOTS (Rows 3-4: simulate 2 more quests but empty) ===
        // Row 3 separator
        fillRow(inv, 3, Material.GRAY_STAINED_GLASS_PANE);
        // Row 4 separator  
        fillRow(inv, 4, Material.GRAY_STAINED_GLASS_PANE);
        
        // === UNIVERSAL NAVBAR (Row 5: slots 45-53) ===
        applyUniversalNavbar(inv, player, "main", new HashMap<>() {{
            put("page", 1);
            put("total_pages", 1);
            put("menu_name", "Quest Menu");
            put("menu_description", "Daily quest management");
        }});
        
        // Track menu
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "simplified_quest", 5));
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }
    
    /**
     * Apply universal navbar from navbar.yml configuration
     */
    private void applyUniversalNavbar(Inventory inv, Player player, String menuType, Map<String, Object> context) {
        // Load navbar configuration from navbar.yml (not config.yml!)
        File navbarFile = new File(plugin.getDataFolder(), "navbar.yml");
        FileConfiguration config;
        if (navbarFile.exists()) {
            config = YamlConfiguration.loadConfiguration(navbarFile);
        } else {
            // No navbar config - skip
            return;
        }
        
        if (!config.contains("navbar")) {
            // No navbar section - skip
            return;
        }

        // Apply each navbar item
        for (String itemName : config.getConfigurationSection("navbar").getKeys(false)) {
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
                // Only show back button if there's a previous menu
                if (context == null || !context.containsKey("previous_menu")) {
                    // Skip back button for main menu
                    continue;
                }
            }
            
            if ("close".equals(itemName)) {
                // Only show close button if there's NO previous menu
                if (context != null && context.containsKey("previous_menu")) {
                    // Skip close button if there's a back button
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
    private ItemStack createGlassPane() {
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
    private void fillRow(Inventory inv, int row, Material material) {
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
    // ==================== HELPER METHODS ====================
    
    private ItemStack createShopCategory(Material icon, String name, String description, int itemCount, boolean clickable) {
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
            if (clickable) {
                lore.add(hex(plugin.getMessageManager().get("shop.category.click-to-open")));
            } else {
                lore.add(hex(plugin.getMessageManager().get("shop.category.not-available")));
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
            lore.add(hex(plugin.getMessageManager().get("shop.item.buy", "price", String.format("%.0f", item.buyPrice))));
            lore.add(hex(plugin.getMessageManager().get("shop.item.buy-action")));
            lore.add("");
            if (item.canSell) {
                lore.add(hex(plugin.getMessageManager().get("shop.item.sell", "price", String.format("%.0f", item.sellPrice))));
                lore.add(hex(plugin.getMessageManager().get("shop.item.sell-action")));
            }
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }
    
    private List<ShopItemData> getShopItemsForCategory(String category) {
        // Try to load from AuraSkills shop file first (page1)
        List<ShopItemData> loaded = loadShopPageItems(category);
        if (!loaded.isEmpty()) return loaded;

        // Fallback to the expanded built-in list with more items
        List<ShopItemData> items = new ArrayList<>();
        switch (category.toLowerCase()) {
            case "food":
                items.add(new ShopItemData(Material.APPLE, "Apple", 10, 5, true));
                items.add(new ShopItemData(Material.BREAD, "Bread", 15, 8, true));
                items.add(new ShopItemData(Material.COOKED_BEEF, "Steak", 25, 12, true));
                items.add(new ShopItemData(Material.COOKED_PORKCHOP, "Cooked Porkchop", 25, 12, true));
                items.add(new ShopItemData(Material.GOLDEN_APPLE, "Golden Apple", 500, 250, true));
                items.add(new ShopItemData(Material.COOKED_CHICKEN, "Cooked Chicken", 20, 10, true));
                items.add(new ShopItemData(Material.BAKED_POTATO, "Baked Potato", 12, 6, true));
                items.add(new ShopItemData(Material.COOKED_MUTTON, "Cooked Mutton", 22, 11, true));
                break;
            case "tools":
                items.add(new ShopItemData(Material.WOODEN_PICKAXE, "Wooden Pickaxe", 20, 5, true));
                items.add(new ShopItemData(Material.STONE_PICKAXE, "Stone Pickaxe", 50, 15, true));
                items.add(new ShopItemData(Material.IRON_PICKAXE, "Iron Pickaxe", 150, 50, true));
                items.add(new ShopItemData(Material.WOODEN_AXE, "Wooden Axe", 20, 5, true));
                items.add(new ShopItemData(Material.STONE_AXE, "Stone Axe", 50, 15, true));
                items.add(new ShopItemData(Material.IRON_AXE, "Iron Axe", 140, 45, true));
                items.add(new ShopItemData(Material.WOODEN_SHOVEL, "Wooden Shovel", 18, 4, true));
                items.add(new ShopItemData(Material.STONE_SHOVEL, "Stone Shovel", 45, 12, true));
                items.add(new ShopItemData(Material.IRON_SHOVEL, "Iron Shovel", 130, 40, true));
                break;
            case "resources":
                items.add(new ShopItemData(Material.OAK_LOG, "Oak Log", 5, 2, true));
                items.add(new ShopItemData(Material.COBBLESTONE, "Cobblestone", 2, 1, true));
                items.add(new ShopItemData(Material.IRON_INGOT, "Iron Ingot", 100, 50, true));
                items.add(new ShopItemData(Material.COAL, "Coal", 10, 5, true));
                items.add(new ShopItemData(Material.COPPER_INGOT, "Copper Ingot", 30, 15, true));
                items.add(new ShopItemData(Material.GOLD_INGOT, "Gold Ingot", 150, 75, true));
                items.add(new ShopItemData(Material.REDSTONE, "Redstone", 20, 10, true));
                items.add(new ShopItemData(Material.LAPIS_LAZULI, "Lapis Lazuli", 25, 12, true));
                break;
            case "blocks":
                items.add(new ShopItemData(Material.STONE, "Stone", 3, 1, true));
                items.add(new ShopItemData(Material.OAK_PLANKS, "Oak Planks", 3, 1, true));
                items.add(new ShopItemData(Material.GLASS, "Glass", 10, 5, true));
                items.add(new ShopItemData(Material.TORCH, "Torch", 5, 2, true));
                items.add(new ShopItemData(Material.DIRT, "Dirt", 2, 1, true));
                items.add(new ShopItemData(Material.SAND, "Sand", 4, 2, true));
                items.add(new ShopItemData(Material.GRAVEL, "Gravel", 4, 2, true));
                items.add(new ShopItemData(Material.BRICK, "Brick", 8, 4, true));
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
    
    /**
     * Create WDP-Quest style progress segment with Custom Model Data
     * @param segmentIndex Segment number (0-7)
     * @param completion Overall completion percentage (0-100)
     * @param isHard Whether this is a hard quest (red vs green)
     */
    private ItemStack createWDPQuestProgressSegment(int segmentIndex, double completion, boolean isHard) {
        // Progress bar constants (EXACTLY matching WDP-Quest)
        final int SEGMENTS = 8;
        final int FILLS_PER_SEGMENT = 5;
        final int TOTAL_UNITS = SEGMENTS * FILLS_PER_SEGMENT; // 40
        final int CMD_NORMAL = 1000;
        final int CMD_HARD = 1010;
        
        // Convert percentage to units (0-40)
        int totalFilledUnits = (int) Math.round(completion / 100.0 * TOTAL_UNITS);
        
        // Calculate this segment's fill level (0-5)
        int unitsBeforeThis = segmentIndex * FILLS_PER_SEGMENT;
        int unitsInThisSegment = Math.max(0, Math.min(FILLS_PER_SEGMENT, totalFilledUnits - unitsBeforeThis));
        
        // Simple CMD: 1000+fill for normal, 1010+fill for hard
        int cmdBase = isHard ? CMD_HARD : CMD_NORMAL;
        
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setCustomModelData(cmdBase + unitsInThisSegment);
            
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
     * Open quest detail view for the simplified quest (Quest 5)
     */
    private void openQuestDetailView(Player player) {
        String title = hex(plugin.getMessageManager().get("quest-detail.title"));
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
            ? hex(plugin.getMessageManager().get("quest-view.quest-icon.complete.name"))
            : hex(plugin.getMessageManager().get("quest-view.quest-icon.incomplete.name"));
        List<String> questLore = plugin.getMessageManager().getList("quest-detail.quest-icon.lore",
                "required", "0");
        ItemStack questIcon = createItem(
            isComplete ? Material.EMERALD : Material.COMPASS,
            questName,
            questLore.stream().map(this::hex).toArray(String[]::new)
        );
        if (isComplete) addGlow(questIcon);
        inv.setItem(4, questIcon);
        
        // Status indicator (slot 8)
        Material statusMat = isComplete ? Material.LIME_DYE : Material.YELLOW_DYE;
        String statusText = isComplete 
            ? hex(plugin.getMessageManager().get("quest-detail.status.completed.name"))
            : hex(plugin.getMessageManager().get("quest-detail.status.active.name"));
        List<String> statusLore = plugin.getMessageManager().getList("quest-detail.status.lore");
        inv.setItem(8, createItem(
            statusMat,
            statusText,
            statusLore.stream().map(this::hex).toArray(String[]::new)
        ));
        
        // === ROW 1: Full-width progress bar ===
        for (int seg = 0; seg < 9; seg++) {
            int slot = 9 + seg; // slots 9-17
            inv.setItem(slot, createWDPQuestProgressSegment(seg, completion, false));
        }
        
        // === ROW 2: Objectives ===
        inv.setItem(18, createItem(
            Material.PAPER,
            hex(plugin.getMessageManager().get("quest-detail.objectives.title")),
            hex(plugin.getMessageManager().get("quest-detail.objectives.subtitle"))
        ));
        
        // Objective - Mine 5 Stone (slot 19)
        Material objMat = isComplete ? Material.LIME_DYE : Material.GRAY_DYE;
        String objStatus = isComplete 
            ? hex(plugin.getMessageManager().get("quest-detail.objectives.complete", "objective", "Mine 5 Stone"))
            : hex(plugin.getMessageManager().get("quest-detail.objectives.incomplete", "objective", "Mine 5 Stone"));
        String objProgress = isComplete 
            ? hex(plugin.getMessageManager().get("quest-detail.objectives.progress-complete"))
            : hex(plugin.getMessageManager().get("quest-detail.objectives.progress-incomplete", 
                "current", String.valueOf(stoneMined), "total", String.valueOf(stoneTarget)));
        
        inv.setItem(19, createItem(
            objMat,
            objStatus,
            objProgress
        ));
        
        // === ROW 3: Rewards ===
        inv.setItem(27, createItem(
            Material.CHEST,
            hex(plugin.getMessageManager().get("quest-detail.rewards.title")),
            hex(plugin.getMessageManager().get("quest-detail.rewards.subtitle"))
        ));
        
        // Reward - 20 SkillCoins (slot 28)
        List<String> rewardLore = plugin.getMessageManager().getList("quest-detail.rewards.skillcoins.lore");
        inv.setItem(28, createItem(
            Material.GOLD_NUGGET,
            hex(plugin.getMessageManager().get("quest-detail.rewards.skillcoins.name", "amount", "20")),
            rewardLore.stream().map(this::hex).toArray(String[]::new)
        ));
        
        // === ROW 4: Empty ===
        
        // === ROW 5: Universal navbar ===
        Map<String, Object> context = new HashMap<>();
        context.put("previous_menu", "simplified_quest");
        context.put("menu_name", "Quest Details");
        context.put("menu_description", "Detailed quest information");
        
        applyUniversalNavbar(inv, player, "quest_detail", context);
        
        // Track menu
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "quest_detail", 5));
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
    
    /**
     * Creates a visual representation of a single segment (fallback).
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
