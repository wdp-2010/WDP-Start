package com.wdp.start.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores player quest progress data
 */
public class PlayerData {
    
    private final UUID uuid;
    private String playerName;
    
    // Quest state
    private boolean started = false;
    private int currentQuest = 0; // 0 = not started, 1-6 = active quest
    private boolean completed = false;
    
    // Quest progress (for multi-step quests)
    private Map<Integer, QuestProgress> questProgress = new HashMap<>();
    
    // Coins tracking for refund
    private int coinsGranted = 0;
    private int coinsSpent = 0;
    private long lastCoinGrantTime = 0;
    
    // Timestamps
    private long startedAt = 0;
    private long completedAt = 0;
    private long lastUpdated = System.currentTimeMillis();
    
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        
        // Initialize progress for all quests
        for (int i = 1; i <= 6; i++) {
            questProgress.put(i, new QuestProgress());
        }
    }
    
    // ==================== GETTERS ====================
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public boolean isStarted() {
        return started;
    }
    
    public int getCurrentQuest() {
        return currentQuest;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public QuestProgress getQuestProgress(int quest) {
        return questProgress.computeIfAbsent(quest, k -> new QuestProgress());
    }
    
    public int getCoinsGranted() {
        return coinsGranted;
    }
    
    public int getCoinsSpent() {
        return coinsSpent;
    }
    
    public int getRefundableCoins() {
        return Math.max(0, coinsGranted - coinsSpent);
    }
    
    public long getLastCoinGrantTime() {
        return lastCoinGrantTime;
    }
    
    public long getStartedAt() {
        return startedAt;
    }
    
    public long getCompletedAt() {
        return completedAt;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    // ==================== TRANSIENT SUPPRESSION ====================
    /**
     * Suppress AuraSkills level-up messages until the specified epoch millis.
     * Used to avoid race conditions during quest completion.
     */
    private long suppressLevelUpUntil = 0;

    public void setSuppressLevelUpUntil(long untilMillis) {
        this.suppressLevelUpUntil = untilMillis;
        this.lastUpdated = System.currentTimeMillis();
    }

    public void clearSuppressLevelUp() {
        this.suppressLevelUpUntil = 0;
        this.lastUpdated = System.currentTimeMillis();
    }

    public boolean isSuppressLevelUpActive() {
        return System.currentTimeMillis() < suppressLevelUpUntil;
    }

    // ==================== SETTERS ====================
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setStarted(boolean started) {
        this.started = started;
        if (started && startedAt == 0) {
            this.startedAt = System.currentTimeMillis();
        }
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setCurrentQuest(int currentQuest) {
        this.currentQuest = currentQuest;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && completedAt == 0) {
            this.completedAt = System.currentTimeMillis();
        }
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void addCoinsGranted(int amount) {
        this.coinsGranted += amount;
        this.lastCoinGrantTime = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void addCoinsSpent(int amount) {
        this.coinsSpent += amount;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void resetCoinsTracking() {
        this.coinsGranted = 0;
        this.coinsSpent = 0;
        this.lastCoinGrantTime = 0;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }
    
    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }
    
    // ==================== QUEST PROGRESS ====================
    
    /**
     * Advance to the next quest
     */
    public void advanceQuest() {
        if (currentQuest < 6) {
            currentQuest++;
            getQuestProgress(currentQuest).setStarted(true);
            this.lastUpdated = System.currentTimeMillis();
        } else {
            completed = true;
            completedAt = System.currentTimeMillis();
        }
    }
    
    /**
     * Complete the current quest and advance
     */
    public void completeCurrentQuest() {
        QuestProgress progress = getQuestProgress(currentQuest);
        progress.setCompleted(true);
        advanceQuest();
    }
    
    /**
     * Reset all progress
     */
    public void reset() {
        started = false;
        currentQuest = 0;
        completed = false;
        startedAt = 0;
        completedAt = 0;
        coinsGranted = 0;
        coinsSpent = 0;
        lastCoinGrantTime = 0;
        
        questProgress.clear();
        for (int i = 1; i <= 6; i++) {
            questProgress.put(i, new QuestProgress());
        }
        
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Check if a specific quest is completed
     */
    public boolean isQuestCompleted(int quest) {
        return getQuestProgress(quest).isCompleted();
    }
    
    /**
     * Get the number of completed quests
     */
    public int getCompletedQuestCount() {
        int count = 0;
        for (int i = 1; i <= 6; i++) {
            if (isQuestCompleted(i)) count++;
        }
        return count;
    }
    
    // ==================== INNER CLASS ====================
    
    /**
     * Progress tracking for individual quests
     */
    public static class QuestProgress {
        private boolean started = false;
        private boolean completed = false;
        private int step = 0;
        private Map<String, Object> data = new HashMap<>();
        private long startedAt = 0;
        private long completedAt = 0;
        
        public boolean isStarted() {
            return started;
        }
        
        public void setStarted(boolean started) {
            this.started = started;
            if (started && startedAt == 0) {
                startedAt = System.currentTimeMillis();
            }
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
            if (completed && completedAt == 0) {
                completedAt = System.currentTimeMillis();
            }
        }
        
        public int getStep() {
            return step;
        }
        
        public void setStep(int step) {
            this.step = step;
        }
        
        public void advanceStep() {
            this.step++;
        }
        
        public Object getData(String key) {
            return data.get(key);
        }
        
        public void setData(String key, Object value) {
            data.put(key, value);
        }
        
        public boolean hasData(String key) {
            return data.containsKey(key);
        }
        
        public Map<String, Object> getAllData() {
            return data;
        }
        
        public void setAllData(Map<String, Object> data) {
            this.data = data;
        }
        
        public void clearData() {
            this.data.clear();
        }
        
        public long getStartedAt() {
            return startedAt;
        }
        
        public void setStartedAt(long startedAt) {
            this.startedAt = startedAt;
        }
        
        public long getCompletedAt() {
            return completedAt;
        }
        
        public void setCompletedAt(long completedAt) {
            this.completedAt = completedAt;
        }
        
        /**
         * Get counter value with default
         */
        public int getCounter(String key, int defaultValue) {
            Object value = data.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return defaultValue;
        }
        
        /**
         * Increment counter by amount
         */
        public void incrementCounter(String key, int amount) {
            int current = getCounter(key, 0);
            data.put(key, current + amount);
        }
    }
}
