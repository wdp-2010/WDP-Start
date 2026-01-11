package com.wdp.start.ui.menu;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active menu sessions for players.
 * Provides session management, state tracking, and cleanup.
 */
public class MenuSessionManager {
    
    private final ConcurrentHashMap<UUID, Session> activeSessions = new ConcurrentHashMap<>();
    
    public MenuSessionManager() {
    }
    
    /**
     * Start a new session
     */
    public void startSession(UUID uuid, MenuType type, int currentQuest) {
        activeSessions.put(uuid, new Session(type, currentQuest, null));
    }
    
    /**
     * Start a new session with context
     */
    public void startSession(UUID uuid, MenuType type, int currentQuest, String context) {
        activeSessions.put(uuid, new Session(type, currentQuest, context));
    }
    
    /**
     * Get the active session for a UUID
     */
    public Session getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }
    
    /**
     * Check if player has an active session
     */
    public boolean hasSession(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }
    
    /**
     * End a session
     */
    public void endSession(UUID uuid) {
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
    
    // ==================== SESSION RECORD ====================
    
    /**
     * Represents an active menu session
     */
    public record Session(MenuType menuType, int currentQuest, String context) {}
    
    // ==================== MENU TYPE ENUM ====================
    
    /**
     * Enumeration of all menu types
     */
    public enum MenuType {
        /** Main quest menu showing all 6 quests */
        MAIN,
        
        /** Welcome menu for new players */
        WELCOME,
        
        /** Quest detail view */
        QUEST_DETAIL,
        
        /** Simplified quest view (Quest 5) */
        QUEST_VIEW,
        
        /** SkillCoins shop main menu */
        SHOP_MAIN,
        
        /** SkillCoins shop category section */
        SHOP_SECTION,
        
        /** Shop item transaction (buy/sell) */
        TRANSACTION,
        
        /** Token exchange menu (Quest 4) */
        TOKEN_EXCHANGE,
        
        /** Shop for tokens only (Quest 4 main) */
        TOKEN_SHOP
    }
}
