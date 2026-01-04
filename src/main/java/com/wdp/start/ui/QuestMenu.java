package com.wdp.start.ui;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quest Menu GUI - Main entry point
 * Refactored to use separate component classes
 * All shop functionality moved to com.wdp.start.shop package
 */
public class QuestMenu {
    
    private final WDPStartPlugin plugin;
    
    // Components
    private final MenuHelper helper;
    private final NavbarManager navbarManager;
    private final QuestMenuBuilder menuBuilder;
    private final Quest5Menu quest5Menu;
    
    // Track open menus
    private final ConcurrentHashMap<UUID, MenuSession> openMenus = new ConcurrentHashMap<>();
    
    // Track Quest 6 reminder tasks
    private final ConcurrentHashMap<UUID, Integer> quest6ReminderTasks = new ConcurrentHashMap<>();
    
    // Menu identifier
    private static final String MENU_ID = "§8§l";
    
    public QuestMenu(WDPStartPlugin plugin) {
        this.plugin = plugin;
        this.helper = new MenuHelper(plugin);
        this.navbarManager = new NavbarManager(plugin);
        this.menuBuilder = new QuestMenuBuilder(plugin, helper, navbarManager);
        this.quest5Menu = new Quest5Menu(plugin, helper, navbarManager, openMenus, MENU_ID);
    }
    
    /**
     * Open the main quest menu
     */
    public void openMainMenu(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Check if current quest is completed and advance if needed
        if (data.isStarted() && !data.isCompleted() && data.isQuestCompleted(data.getCurrentQuest())) {
            plugin.debug("[QuestMenu] Current quest " + data.getCurrentQuest() + " is completed, advancing...");
            data.advanceQuest();
            plugin.getPlayerDataManager().saveData(data);
            plugin.getPlayerDataManager().forceSave(player.getUniqueId());
        }
        
        plugin.debug("[QuestMenu] Opening menu for " + player.getName() + 
            " | Started: " + data.isStarted() + 
            " | CurrentQuest: " + data.getCurrentQuest() + 
            " | Completed: " + data.isCompleted());
        
        String title = helper.hex(plugin.getMessageManager().get("menu.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        if (!data.isStarted()) {
            plugin.debug("[QuestMenu] Showing welcome menu (not started)");
            menuBuilder.buildWelcomeMenu(inv);
        } else {
            plugin.debug("[QuestMenu] Showing normal menu with progress");
            menuBuilder.buildNormalMenu(inv, player, data);
        }
        
        menuBuilder.addNavbar(inv, player, data);
        
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "main", data.getCurrentQuest()));
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }
    
    /**
     * Open simplified quest view for Quest 5
     */
    public void openSimplifiedQuestView(Player player) {
        quest5Menu.openSimplifiedQuestView(player);
    }
    
    /**
     * Handle menu click
     */
    public void handleClick(Player player, int slot, Inventory inv) {
        MenuSession session = openMenus.get(player.getUniqueId());
        if (session == null) return;
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        
        String menuType = session.getMenuType();
        
        // Handle navbar clicks (slots 45-53)
        if (slot >= 45 && slot <= 53) {
            if (slot == 45) { // Back button
                if ("quest_detail".equals(menuType)) {
                    quest5Menu.openSimplifiedQuestView(player);
                } else if ("simplified_quest".equals(menuType)) {
                    openMainMenu(player);
                }
                return;
            }
            if (slot == 53) { // Close button
                player.closeInventory();
                return;
            }
            return;
        }
        
        // Handle Quest 5 simplified menu
        if (quest5Menu.handleClick(player, slot, menuType, data)) {
            return;
        }
        
        // Welcome menu - start button
        if (!data.isStarted() && slot == 31) {
            plugin.getQuestManager().startQuests(player);
            return;
        }
        
        // Quest 5 WDP-Quest menu handlers
        if (data.getCurrentQuest() == 5 && !data.isQuestCompleted(5)) {
            if (slot == 48) {
                openMainMenu(player);
                return;
            }
            
            if (slot == 49) {
                PlayerData.QuestProgress progress = data.getQuestProgress(5);
                int stoneMined = progress.getCounter("stone_mined", 0);
                
                if (stoneMined >= 5) {
                    plugin.getQuestManager().completeQuest(player, 5);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        openMainMenu(player);
                    }, 5L);
                } else {
                    player.sendMessage(helper.hex(plugin.getMessageManager().get("errors.mine-stone-first",
                            "current", String.valueOf(stoneMined))));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                return;
            }
            
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
        int quest = menuBuilder.getQuestFromSlot(slot);
        if (quest > 0 && quest == data.getCurrentQuest()) {
            showQuestDetails(player, quest);
        }
    }
    
    /**
     * Show detailed quest view
     */
    private void showQuestDetails(Player player, int quest) {
        player.closeInventory();
        
        // Quest 6: Check if already completed
        if (quest == 6) {
            PlayerData data = plugin.getPlayerDataManager().getData(player);
            PlayerData.QuestProgress progress = data.getQuestProgress(6);
            
            if (progress.isCompleted()) {
                player.sendMessage(helper.hex(plugin.getMessageManager().get("quest.already-completed")));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            
            // Complete Quest 6
            plugin.getQuestManager().completeQuest(player, 6);
            player.sendMessage(helper.hex(plugin.getMessageManager().get("success.quest-complete-welcome")));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
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
        
        player.sendMessage(helper.hex(plugin.getMessageManager().get("success.quest-hint", "hint", hint)));
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
        cancelQuest6Reminders(player);
    }
    
    /**
     * Start Quest 6 reminder and auto-complete system
     */
    public void startQuest6Reminders(Player player) {
        cancelQuest6Reminders(player);
        
        UUID uuid = player.getUniqueId();
        
        player.sendMessage(helper.hex(plugin.getMessageManager().get("quest6.instruction")));
        player.sendMessage(helper.hex(plugin.getMessageManager().get("quest6.instruction-detail")));
        
        int task1 = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                PlayerData data = plugin.getPlayerDataManager().getData(player);
                if (data.getCurrentQuest() == 6 && !data.isQuestCompleted(6)) {
                    player.sendMessage(helper.hex(plugin.getMessageManager().get("quest6.reminder-10s")));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
                }
            }
        }, 200L).getTaskId();
        
        int task2 = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                PlayerData data = plugin.getPlayerDataManager().getData(player);
                if (data.getCurrentQuest() == 6 && !data.isQuestCompleted(6)) {
                    player.sendMessage(helper.hex(plugin.getMessageManager().get("quest6.reminder-30s")));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.7f);
                }
            }
        }, 600L).getTaskId();
        
        int task3 = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                PlayerData data = plugin.getPlayerDataManager().getData(player);
                if (data.getCurrentQuest() == 6 && !data.isQuestCompleted(6)) {
                    player.sendMessage(helper.hex(plugin.getMessageManager().get("quest6.auto-complete")));
                    plugin.getQuestManager().completeQuest(player, 6);
                    player.closeInventory();
                }
            }
            quest6ReminderTasks.remove(uuid);
        }, 700L).getTaskId();
        
        quest6ReminderTasks.put(uuid, task1);
    }
    
    /**
     * Cancel Quest 6 reminder tasks for a player
     */
    private void cancelQuest6Reminders(Player player) {
        UUID uuid = player.getUniqueId();
        if (quest6ReminderTasks.containsKey(uuid)) {
            plugin.getServer().getScheduler().getPendingTasks().stream()
                .filter(task -> task.getOwner().equals(plugin) && 
                        task.getTaskId() >= quest6ReminderTasks.get(uuid))
                .forEach(task -> plugin.getServer().getScheduler().cancelTask(task.getTaskId()));
            
            quest6ReminderTasks.remove(uuid);
        }
    }
    
    // Getters for components
    public MenuHelper getHelper() {
        return helper;
    }
    
    public NavbarManager getNavbarManager() {
        return navbarManager;
    }
}
