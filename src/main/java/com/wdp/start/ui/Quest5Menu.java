package com.wdp.start.ui;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Quest 5 simplified view menu
 * WDP-Quest style with progress bar segments
 */
public class Quest5Menu {
    
    private final WDPStartPlugin plugin;
    private final MenuHelper helper;
    private final NavbarManager navbarManager;
    private final ConcurrentHashMap<UUID, MenuSession> openMenus;
    private final String menuId;
    
    public Quest5Menu(WDPStartPlugin plugin, MenuHelper helper, NavbarManager navbarManager, 
                      ConcurrentHashMap<UUID, MenuSession> openMenus, String menuId) {
        this.plugin = plugin;
        this.helper = helper;
        this.navbarManager = navbarManager;
        this.openMenus = openMenus;
        this.menuId = menuId;
    }
    
    /**
     * Open simplified quest view for Quest 5
     * WDP-Quest main menu structure with progress bar
     */
    public void openSimplifiedQuestView(Player player) {
        String title = helper.hex(plugin.getMessageManager().get("quest-view.title"));
        Inventory inv = Bukkit.createInventory(null, 54, menuId + title);
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        PlayerData.QuestProgress progress = data.getQuestProgress(5);
        
        int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
        int stoneTarget = 5;
        double completion = Math.min(100.0, (stoneMined * 100.0) / stoneTarget);
        boolean isComplete = stoneMined >= stoneTarget;
        
        double coins = 0;
        int tokens = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        // === HEADER ROW (Row 0: slots 0-8) ===
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = playerHead.getItemMeta();
        if (headMeta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(helper.hex(plugin.getMessageManager().get("quest-view.player-head.name",
                    "player", player.getName())));
            List<String> headLore = plugin.getMessageManager().getList("quest-view.player-head.lore",
                    "coins", String.format("%.0f", coins),
                    "tokens", String.valueOf(tokens));
            skullMeta.setLore(headLore.stream().map(helper::hex).toList());
            playerHead.setItemMeta(skullMeta);
        }
        inv.setItem(0, playerHead);
        
        // Title (slot 4)
        List<String> titleLore = plugin.getMessageManager().getList("quest-view.title-item.lore");
        inv.setItem(4, helper.createItem(
            Material.BOOK,
            helper.hex(plugin.getMessageManager().get("quest-view.title-item.name")),
            titleLore.stream().map(helper::hex).toArray(String[]::new)
        ));
        
        // Progress display (slot 8)
        List<String> progressLore = plugin.getMessageManager().getList("quest-view.progress-item.lore",
                "progress", String.format("%.0f", completion));
        inv.setItem(8, helper.createItem(
            Material.EXPERIENCE_BOTTLE,
            helper.hex(plugin.getMessageManager().get("quest-view.progress-item.name")),
            progressLore.stream().map(helper::hex).toArray(String[]::new)
        ));
        
        // === QUEST ROW (Row 1: slots 9-17) ===
        String questName = isComplete 
            ? helper.hex(plugin.getMessageManager().get("quest-view.quest-icon.complete.name"))
            : helper.hex(plugin.getMessageManager().get("quest-view.quest-icon.incomplete.name"));
        List<String> questLore = plugin.getMessageManager().getList("quest-view.quest-icon.lore",
                "current", String.valueOf(stoneMined),
                "total", String.valueOf(stoneTarget));
        ItemStack questIcon = helper.createItem(
            isComplete ? Material.EMERALD : Material.COMPASS,
            questName,
            questLore.stream().map(helper::hex).toArray(String[]::new)
        );
        if (isComplete) helper.addGlow(questIcon);
        inv.setItem(9, questIcon);
        
        // Progress bar (8 segments, slots 10-17)
        for (int seg = 0; seg < 8; seg++) {
            inv.setItem(10 + seg, helper.createProgressSegment(seg, completion, false));
        }
        
        // === SEPARATOR ROWS ===
        helper.fillRow(inv, 2, Material.GRAY_STAINED_GLASS_PANE);
        helper.fillRow(inv, 3, Material.GRAY_STAINED_GLASS_PANE);
        helper.fillRow(inv, 4, Material.GRAY_STAINED_GLASS_PANE);
        
        // === UNIVERSAL NAVBAR (Row 5) ===
        Map<String, Object> context = new HashMap<>();
        context.put("page", 1);
        context.put("total_pages", 1);
        context.put("menu_name", "Quest Menu");
        context.put("menu_description", "Daily quest management");
        
        navbarManager.applyNavbar(inv, player, "main", context);
        
        // Track menu
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "simplified_quest", 5));
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }
    
    /**
     * Open quest detail view for Quest 5
     */
    public void openQuestDetailView(Player player) {
        String title = helper.hex(plugin.getMessageManager().get("quest-detail.title"));
        Inventory inv = Bukkit.createInventory(null, 54, menuId + title);
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        PlayerData.QuestProgress progress = data.getQuestProgress(5);
        
        int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
        int stoneTarget = 5;
        double completion = Math.min(100.0, (stoneMined * 100.0) / stoneTarget);
        boolean isComplete = stoneMined >= stoneTarget;
        
        // === ROW 0: Header ===
        String questName = isComplete 
            ? helper.hex(plugin.getMessageManager().get("quest-view.quest-icon.complete.name"))
            : helper.hex(plugin.getMessageManager().get("quest-view.quest-icon.incomplete.name"));
        List<String> questLore = plugin.getMessageManager().getList("quest-detail.quest-icon.lore",
                "required", "0");
        ItemStack questIcon = helper.createItem(
            isComplete ? Material.EMERALD : Material.COMPASS,
            questName,
            questLore.stream().map(helper::hex).toArray(String[]::new)
        );
        if (isComplete) helper.addGlow(questIcon);
        inv.setItem(4, questIcon);
        
        // Status indicator (slot 8)
        Material statusMat = isComplete ? Material.LIME_DYE : Material.YELLOW_DYE;
        String statusText = isComplete 
            ? helper.hex(plugin.getMessageManager().get("quest-detail.status.completed.name"))
            : helper.hex(plugin.getMessageManager().get("quest-detail.status.active.name"));
        List<String> statusLore = plugin.getMessageManager().getList("quest-detail.status.lore");
        inv.setItem(8, helper.createItem(
            statusMat,
            statusText,
            statusLore.stream().map(helper::hex).toArray(String[]::new)
        ));
        
        // === ROW 1: Full-width progress bar ===
        for (int seg = 0; seg < 9; seg++) {
            inv.setItem(9 + seg, helper.createProgressSegment(seg, completion, false));
        }
        
        // === ROW 2: Objectives ===
        inv.setItem(18, helper.createItem(
            Material.PAPER,
            helper.hex(plugin.getMessageManager().get("quest-detail.objectives.title")),
            helper.hex(plugin.getMessageManager().get("quest-detail.objectives.subtitle"))
        ));
        
        Material objMat = isComplete ? Material.LIME_DYE : Material.GRAY_DYE;
        String objStatus = isComplete 
            ? helper.hex(plugin.getMessageManager().get("quest-detail.objectives.complete", "objective", "Mine 5 Stone"))
            : helper.hex(plugin.getMessageManager().get("quest-detail.objectives.incomplete", "objective", "Mine 5 Stone"));
        String objProgress = isComplete 
            ? helper.hex(plugin.getMessageManager().get("quest-detail.objectives.progress-complete"))
            : helper.hex(plugin.getMessageManager().get("quest-detail.objectives.progress-incomplete", 
                "current", String.valueOf(stoneMined), "total", String.valueOf(stoneTarget)));
        
        inv.setItem(19, helper.createItem(objMat, objStatus, objProgress));
        
        // === ROW 3: Rewards ===
        inv.setItem(27, helper.createItem(
            Material.CHEST,
            helper.hex(plugin.getMessageManager().get("quest-detail.rewards.title")),
            helper.hex(plugin.getMessageManager().get("quest-detail.rewards.subtitle"))
        ));
        
        List<String> rewardLore = plugin.getMessageManager().getList("quest-detail.rewards.skillcoins.lore");
        inv.setItem(28, helper.createItem(
            Material.GOLD_NUGGET,
            helper.hex(plugin.getMessageManager().get("quest-detail.rewards.skillcoins.name", "amount", "20")),
            rewardLore.stream().map(helper::hex).toArray(String[]::new)
        ));
        
        // === ROW 5: Universal navbar ===
        Map<String, Object> context = new HashMap<>();
        context.put("previous_menu", "simplified_quest");
        context.put("menu_name", "Quest Details");
        context.put("menu_description", "Detailed quest information");
        
        navbarManager.applyNavbar(inv, player, "quest_detail", context);
        
        // Track menu
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new MenuSession(player, "quest_detail", 5));
        
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
    
    /**
     * Handle clicks in Quest 5 menu
     * @return true if click was handled
     */
    public boolean handleClick(Player player, int slot, String menuType, PlayerData data) {
        if (!menuType.equals("simplified_quest") && !menuType.equals("quest_detail")) {
            return false;
        }
        
        if (menuType.equals("simplified_quest")) {
            // Quest icon click (slot 9)
            if (slot == 9) {
                PlayerData.QuestProgress progress = data.getQuestProgress(5);
                int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
                if (stoneMined >= 5) {
                    plugin.getQuestManager().completeQuest(player, 5);
                    player.sendMessage(helper.hex(plugin.getMessageManager().get("success.quest-complete")));
                    player.closeInventory();
                } else {
                    player.sendMessage(helper.hex(plugin.getMessageManager().get("quest-progress.stone-progress", 
                            "current", String.valueOf(stoneMined), 
                            "required", "5")));
                    
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        openSimplifiedQuestView(player);
                    }, 2L);
                }
                return true;
            }
            
            // Progress bar click (slots 10-17)
            if (slot >= 10 && slot <= 17) {
                PlayerData.QuestProgress progress = data.getQuestProgress(5);
                int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
                player.sendMessage(helper.hex(plugin.getMessageManager().get("quest-progress.stone-progress", 
                        "current", String.valueOf(stoneMined), 
                        "required", "5")));
                return true;
            }
            
            // Navbar handling
            if (slot == 45) {
                return true; // Let main menu handle back
            }
            if (slot == 53) {
                player.closeInventory();
                return true;
            }
        }
        
        return false;
    }
}
