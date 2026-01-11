package com.wdp.start.ui.menu;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static com.wdp.start.ui.menu.MenuUtils.*;

/**
 * Builds the Quest 5 simplified quest view menu.
 * Shows a single quest with 8-segment progress bar.
 */
public class Quest5MenuBuilder {
    
    private final WDPStartPlugin plugin;
    
    public Quest5MenuBuilder(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Build the Quest 5 simplified view
     */
    public Inventory build(Player player) {
        return build(player, plugin.getPlayerDataManager().getData(player));
    }
    
    /**
     * Build the Quest 5 simplified view with provided data
     */
    public Inventory build(Player player, PlayerData data) {
        String title = hex(plugin.getMessageManager().get("quest-view.title"));
        Inventory inv = Bukkit.createInventory(null, 54, MENU_ID + title);
        
        PlayerData.QuestProgress progress = data.getQuestProgress(5);
        
        // Get stone mining progress
        int stoneMined = progress != null ? progress.getCounter("stone_mined", 0) : 0;
        int stoneTarget = 5;
        double completion = Math.min(100.0, (stoneMined * 100.0) / stoneTarget);
        boolean isComplete = stoneMined >= stoneTarget;
        
        // Get player balance
        double coins = 0;
        if (plugin.getVaultIntegration() != null) {
            coins = plugin.getVaultIntegration().getBalance(player);
        }
        
        // === HEADER ROW (Row 0: slots 0-8) ===
        // Player head (slot 0)
        List<String> headLore = plugin.getMessageManager().getList("quest-view.player-head.lore",
            "coins", String.format("%.0f", coins),
            "tokens", "0");
        inv.setItem(0, createPlayerHead(player,
            hex(plugin.getMessageManager().get("quest-view.player-head.name", "player", player.getName())),
            headLore.stream().map(MenuUtils::hex).toList()
        ));
        
        // Title (slot 4)
        List<String> titleLore = plugin.getMessageManager().getList("quest-view.title-item.lore");
        inv.setItem(4, createItem(Material.BOOK,
            hex(plugin.getMessageManager().get("quest-view.title-item.name")),
            titleLore.stream().map(MenuUtils::hex).toArray(String[]::new)
        ));
        
        // Progress display (slot 8)
        List<String> progressLore = plugin.getMessageManager().getList("quest-view.progress-item.lore",
            "progress", String.format("%.0f", completion));
        inv.setItem(8, createItem(Material.EXPERIENCE_BOTTLE,
            hex(plugin.getMessageManager().get("quest-view.progress-item.name")),
            progressLore.stream().map(MenuUtils::hex).toArray(String[]::new)
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
            questLore.stream().map(MenuUtils::hex).toArray(String[]::new)
        );
        if (isComplete) addGlow(questIcon);
        inv.setItem(9, questIcon);
        
        // Progress bar (8 segments, slots 10-17)
        for (int seg = 0; seg < 8; seg++) {
            inv.setItem(10 + seg, createProgressSegment(seg, completion, false));
        }
        
        // === SEPARATOR ROWS ===
        fillRow(inv, 2, Material.GRAY_STAINED_GLASS_PANE);
        fillRow(inv, 3, Material.GRAY_STAINED_GLASS_PANE);
        fillRow(inv, 4, Material.GRAY_STAINED_GLASS_PANE);
        
        return inv;
    }
    
    /**
     * Create a progress bar segment
     */
    private ItemStack createProgressSegment(int segmentIndex, double completion, boolean isHard) {
        final int SEGMENTS = 8;
        final int FILLS_PER_SEGMENT = 5;
        final int TOTAL_UNITS = SEGMENTS * FILLS_PER_SEGMENT;
        
        int totalFilledUnits = (int) Math.round(completion / 100.0 * TOTAL_UNITS);
        int unitsBeforeThis = segmentIndex * FILLS_PER_SEGMENT;
        int unitsInThisSegment = Math.max(0, Math.min(FILLS_PER_SEGMENT, totalFilledUnits - unitsBeforeThis));
        
        // Create visual segment
        String color = isHard ? "§c" : "§a";
        String filled = isHard ? "§c█" : "§a█";
        String empty = "§7░";
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            bar.append(i < unitsInThisSegment ? filled : empty);
        }
        
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(bar.toString());
            
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
}
