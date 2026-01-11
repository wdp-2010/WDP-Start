package com.wdp.start.ui.menu;

import com.wdp.start.WDPStartPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static com.wdp.start.ui.menu.MenuUtils.*;

/**
 * Builds the welcome menu for players who haven't started quests yet.
 * Shows a welcoming message and a "Start" button.
 */
public class WelcomeMenuBuilder {
    
    private final WDPStartPlugin plugin;
    
    public WelcomeMenuBuilder(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Build the welcome menu content
     * 
     * @param inv The inventory to populate
     */
    public void build(Inventory inv) {
        // Welcome title item (slot 13)
        List<String> welcomeLore = plugin.getMessageManager().getList("menu.welcome.description");
        String[] welcomeLoreArray = welcomeLore.stream().map(MenuUtils::hex).toArray(String[]::new);
        ItemStack welcome = createItem(Material.NETHER_STAR,
            hex(plugin.getMessageManager().get("menu.welcome.title")),
            welcomeLoreArray
        );
        inv.setItem(13, welcome);
        
        // Start button (slot 31)
        List<String> startLore = plugin.getMessageManager().getList("menu.welcome.start-button.lore");
        String[] startLoreArray = startLore.stream().map(MenuUtils::hex).toArray(String[]::new);
        ItemStack start = createItem(Material.LIME_CONCRETE,
            hex(plugin.getMessageManager().get("menu.welcome.start-button.name")),
            startLoreArray
        );
        addGlow(start);
        inv.setItem(31, start);
    }
    
    /**
     * Handle click in welcome menu
     * 
     * @param slot The clicked slot
     * @return true if click was handled
     */
    public boolean handleClick(int slot) {
        // Start button is slot 31
        return slot == 31;
    }
}
