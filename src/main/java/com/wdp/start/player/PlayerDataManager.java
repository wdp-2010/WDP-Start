package com.wdp.start.player;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.storage.DatabaseManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player data loading, saving, and caching
 * Uses SQLite database for persistent storage
 */
public class PlayerDataManager {
    
    private final WDPStartPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final File dataFolder;
    
    public PlayerDataManager(WDPStartPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.dataFolder = new File(plugin.getDataFolder(), "players");
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    /**
     * Get player data (loads from database if not cached)
     */
    public PlayerData getData(Player player) {
        return getData(player.getUniqueId(), player.getName());
    }
    
    /**
     * Get player data by UUID
     */
    public PlayerData getData(UUID uuid, String name) {
        // First check our local cache
        PlayerData cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }
        
        // Try database
        if (databaseManager != null && databaseManager.isConnected()) {
            PlayerData data = databaseManager.getData(uuid, name);
            cache.put(uuid, data);
            return data;
        }
        
        // Fallback to YAML
        return cache.computeIfAbsent(uuid, id -> {
            PlayerData data = loadDataFromYaml(id);
            if (data == null) {
                data = new PlayerData(id);
            }
            data.setPlayerName(name);
            return data;
        });
    }
    
    /**
     * Check if player has data (without loading)
     */
    public boolean hasData(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return true;
        }
        
        // Check database
        if (databaseManager != null && databaseManager.isConnected()) {
            return databaseManager.hasData(uuid);
        }
        
        // Fallback to file
        File file = getPlayerFile(uuid);
        return file.exists();
    }
    
    /**
     * Load player data from YAML file (fallback)
     */
    private PlayerData loadDataFromYaml(UUID uuid) {
        File file = getPlayerFile(uuid);
        
        if (!file.exists()) {
            return null;
        }
        
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            
            PlayerData data = new PlayerData(uuid);
            
            // Load basic data
            data.setPlayerName(yaml.getString("player.name", "Unknown"));
            data.setStarted(yaml.getBoolean("quest.started", false));
            data.setCurrentQuest(yaml.getInt("quest.current", 0));
            data.setCompleted(yaml.getBoolean("quest.completed", false));
            data.setStartedAt(yaml.getLong("quest.started-at", 0));
            data.setCompletedAt(yaml.getLong("quest.completed-at", 0));
            
            // Load coin tracking
            data.addCoinsGranted(yaml.getInt("coins.granted", 0));
            data.addCoinsSpent(yaml.getInt("coins.spent", 0));
            
            // Load quest progress
            for (int i = 1; i <= 6; i++) {
                String path = "progress.quest" + i;
                if (yaml.contains(path)) {
                    PlayerData.QuestProgress progress = data.getQuestProgress(i);
                    progress.setStarted(yaml.getBoolean(path + ".started", false));
                    progress.setCompleted(yaml.getBoolean(path + ".completed", false));
                    progress.setStep(yaml.getInt(path + ".step", 0));
                    progress.setStartedAt(yaml.getLong(path + ".started-at", 0));
                    progress.setCompletedAt(yaml.getLong(path + ".completed-at", 0));
                    
                    // Load custom data
                    if (yaml.contains(path + ".data")) {
                        for (String key : yaml.getConfigurationSection(path + ".data").getKeys(false)) {
                            progress.setData(key, yaml.get(path + ".data." + key));
                        }
                    }
                }
            }
            
            plugin.debug("Loaded data for " + uuid);
            return data;
            
        } catch (Exception e) {
            plugin.logError("Failed to load player data for " + uuid, e);
            return null;
        }
    }
    
    /**
     * Save player data to database and optionally to YAML
     */
    public void saveData(PlayerData data) {
        // Save to database (primary)
        if (databaseManager != null && databaseManager.isConnected()) {
            databaseManager.saveData(data);
        }
        
        // Also save to YAML as backup
        saveDataToYaml(data);
    }
    
    /**
     * Save player data to YAML file
     */
    private void saveDataToYaml(PlayerData data) {
        File file = getPlayerFile(data.getUuid());
        
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            
            // Save basic data
            yaml.set("player.uuid", data.getUuid().toString());
            yaml.set("player.name", data.getPlayerName());
            
            yaml.set("quest.started", data.isStarted());
            yaml.set("quest.current", data.getCurrentQuest());
            yaml.set("quest.completed", data.isCompleted());
            yaml.set("quest.started-at", data.getStartedAt());
            yaml.set("quest.completed-at", data.getCompletedAt());
            
            // Save coin tracking
            yaml.set("coins.granted", data.getCoinsGranted());
            yaml.set("coins.spent", data.getCoinsSpent());
            
            // Save quest progress
            for (int i = 1; i <= 6; i++) {
                String path = "progress.quest" + i;
                PlayerData.QuestProgress progress = data.getQuestProgress(i);
                
                yaml.set(path + ".started", progress.isStarted());
                yaml.set(path + ".completed", progress.isCompleted());
                yaml.set(path + ".step", progress.getStep());
                yaml.set(path + ".started-at", progress.getStartedAt());
                yaml.set(path + ".completed-at", progress.getCompletedAt());
                
                // Save custom data
                for (Map.Entry<String, Object> entry : progress.getAllData().entrySet()) {
                    yaml.set(path + ".data." + entry.getKey(), entry.getValue());
                }
            }
            
            yaml.save(file);
            plugin.debug("Saved data for " + data.getUuid());
            
        } catch (IOException e) {
            plugin.logError("Failed to save player data for " + data.getUuid(), e);
        }
    }
    
    /**
     * Save all cached data
     */
    public void saveAll() {
        for (PlayerData data : cache.values()) {
            saveData(data);
        }
        plugin.debug("Saved all player data (" + cache.size() + " players)");
    }
    
    /**
     * Unload player data (saves and removes from cache)
     */
    public void unloadData(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            saveData(data);
        }
        
        // Also unload from database cache
        if (databaseManager != null) {
            databaseManager.unloadData(uuid);
        }
    }
    
    /**
     * Force save player data immediately
     */
    public void forceSave(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            if (databaseManager != null && databaseManager.isConnected()) {
                databaseManager.forceSave(uuid);
            }
            saveDataToYaml(data);
            plugin.debug("Force saved data for " + uuid);
        }
    }
    
    /**
     * Get the file for a player's data
     */
    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }
    
    /**
     * Check if player is new (never seen before)
     */
    public boolean isNewPlayer(UUID uuid) {
        return !hasData(uuid);
    }
    
    /**
     * Delete player data
     */
    public void deleteData(UUID uuid) {
        cache.remove(uuid);
        File file = getPlayerFile(uuid);
        if (file.exists()) {
            file.delete();
        }
    }
    
    /**
     * Get all cached players
     */
    public Map<UUID, PlayerData> getCache() {
        return cache;
    }
}
