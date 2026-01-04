package com.wdp.start.ui;

import org.bukkit.entity.Player;

/**
 * Tracks menu session state for a player
 * Moved from inner class in QuestMenu to standalone class
 */
public class MenuSession {
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
