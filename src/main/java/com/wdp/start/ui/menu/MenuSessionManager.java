package com.wdp.start.ui.menu;

import com.wdp.start.WDPStartPlugin;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active menu sessions for players.
 * Provides session management, state tracking, and cleanup.
 */
public class MenuSessionManager {
    
    private final WDPStartPlugin plugin;
    private final ConcurrentHashMap<UUID, MenuSession> activeSessions = new ConcurrentHashMap<>();
    
    public MenuSessionManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Create and register a new menu session
     */
    public MenuSession createSession(Player player, MenuType type, int currentQuest) {
        MenuSession session = new MenuSession(player, type, currentQuest);
        activeSessions.put(player.getUniqueId(), session);
        return session;
    }
    
    /**
     * Create and register a new menu session with extra data
     */
    public MenuSession createSession(Player player, MenuType type, int currentQuest, String extraData) {
        MenuSession session = new MenuSession(player, type, currentQuest, extraData);
        activeSessions.put(player.getUniqueId(), session);
        return session;
    }
    
    /**
     * Get the active session for a player
     */
    public MenuSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }
    
    /**
     * Get the active session for a UUID
     */
    public MenuSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }
    
    /**
     * Check if player has an active session
     */
    public boolean hasSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
    
    /**
     * Remove a player's session
     */
    public void removeSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }
    
    /**
     * Remove a session by UUID
     */
    public void removeSession(UUID uuid) {
        activeSessions.remove(uuid);
    }
    
    /**
     * Clear all sessions (for plugin disable)
     */
    public void clearAll() {
        activeSessions.clear();
    }
    
    /**
     * Get count of active sessions
     */
    public int getActiveCount() {
        return activeSessions.size();
    }
    
    // ==================== MENU SESSION CLASS ====================
    
    /**
     * Represents an active menu session for a player
     */
    public static class MenuSession {
        private final Player player;
        private final MenuType type;
        private final int currentQuest;
        private final String extraData;
        private final long createdAt;
        
        public MenuSession(Player player, MenuType type, int currentQuest) {
            this(player, type, currentQuest, null);
        }
        
        public MenuSession(Player player, MenuType type, int currentQuest, String extraData) {
            this.player = player;
            this.type = type;
            this.currentQuest = currentQuest;
            this.extraData = extraData;
            this.createdAt = System.currentTimeMillis();
        }
        
        public Player getPlayer() {
            return player;
        }
        
        public MenuType getType() {
            return type;
        }
        
        public int getCurrentQuest() {
            return currentQuest;
        }
        
        public String getExtraData() {
            return extraData;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        /**
         * Get session age in milliseconds
         */
        public long getAge() {
            return System.currentTimeMillis() - createdAt;
        }
        
        /**
         * Compatibility method for old code using getMenuType()
         */
        public String getMenuType() {
            if (extraData != null && !extraData.isEmpty()) {
                return type.getLegacyId() + "_" + extraData;
            }
            return type.getLegacyId();
        }
    }
    
    // ==================== MENU TYPE ENUM ====================
    
    /**
     * Enumeration of all menu types
     */
    public enum MenuType {
        /** Main quest menu showing all 6 quests */
        MAIN_MENU("main"),
        
        /** Welcome menu for new players */
        WELCOME("welcome"),
        
        /** Quest detail view */
        QUEST_DETAIL("quest_detail"),
        
        /** Simplified quest view (Quest 5) */
        SIMPLIFIED_QUEST("simplified_quest"),
        
        /** SkillCoins shop main menu */
        SHOP_MAIN("skillcoins_shop_main"),
        
        /** SkillCoins shop category section */
        SHOP_SECTION("skillcoins_shop_section"),
        
        /** Shop item transaction (buy/sell) */
        SHOP_TRANSACTION("skillcoins_transaction"),
        
        /** Token exchange menu (Quest 4) */
        TOKEN_EXCHANGE("token_exchange"),
        
        /** Shop for tokens only (Quest 4 main) */
        SHOP_TOKENS("skillcoins_shop_tokens");
        
        private final String legacyId;
        
        MenuType(String legacyId) {
            this.legacyId = legacyId;
        }
        
        public String getLegacyId() {
            return legacyId;
        }
        
        /**
         * Get MenuType from legacy ID string
         */
        public static MenuType fromLegacyId(String id) {
            if (id == null) return MAIN_MENU;
            
            for (MenuType type : values()) {
                if (id.startsWith(type.legacyId)) {
                    return type;
                }
            }
            return MAIN_MENU;
        }
    }
}
