package com.wdp.start.storage;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQLite Database Manager for persistent player data storage
 * Ensures data is actively saved and persists across server restarts
 */
public class DatabaseManager {
    
    private final WDPStartPlugin plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private Connection connection;
    private final File databaseFile;
    
    public DatabaseManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "playerdata.db");
        
        initDatabase();
    }
    
    /**
     * Initialize the SQLite database
     */
    private void initDatabase() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
            
            // Create connection
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            // Create tables
            createTables();
            
            plugin.getLogger().info("[Database] SQLite database initialized: " + databaseFile.getName());
            
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("[Database] SQLite driver not found! Using fallback YAML storage.");
            plugin.logError("SQLite driver not found", e);
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] Failed to initialize database!");
            plugin.logError("Database initialization failed", e);
        }
    }
    
    /**
     * Create the required database tables
     */
    private void createTables() throws SQLException {
        String createPlayerTable = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                started INTEGER DEFAULT 0,
                current_quest INTEGER DEFAULT 0,
                completed INTEGER DEFAULT 0,
                coins_granted INTEGER DEFAULT 0,
                coins_spent INTEGER DEFAULT 0,
                started_at INTEGER DEFAULT 0,
                completed_at INTEGER DEFAULT 0,
                last_updated INTEGER DEFAULT 0
            )
            """;
        
        String createQuestProgressTable = """
            CREATE TABLE IF NOT EXISTS quest_progress (
                uuid TEXT NOT NULL,
                quest_number INTEGER NOT NULL,
                started INTEGER DEFAULT 0,
                completed INTEGER DEFAULT 0,
                step INTEGER DEFAULT 0,
                started_at INTEGER DEFAULT 0,
                completed_at INTEGER DEFAULT 0,
                data TEXT,
                PRIMARY KEY (uuid, quest_number),
                FOREIGN KEY (uuid) REFERENCES player_data(uuid) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayerTable);
            stmt.execute(createQuestProgressTable);
            
            // Create indexes for faster lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_name ON player_data(player_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_quest_uuid ON quest_progress(uuid)");
        }
        
        plugin.debug("[Database] Tables created/verified successfully");
    }
    
    /**
     * Check if database is connected
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Get player data (loads from DB if not cached)
     */
    public PlayerData getData(UUID uuid, String playerName) {
        return cache.computeIfAbsent(uuid, id -> {
            PlayerData data = loadFromDatabase(id);
            if (data == null) {
                data = new PlayerData(id);
                plugin.debug("[Database] Created new PlayerData for " + playerName);
            }
            data.setPlayerName(playerName);
            return data;
        });
    }
    
    /**
     * Load player data from database
     */
    private PlayerData loadFromDatabase(UUID uuid) {
        if (!isConnected()) return null;
        
        String sql = "SELECT * FROM player_data WHERE uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                PlayerData data = new PlayerData(uuid);
                
                data.setPlayerName(rs.getString("player_name"));
                data.setStarted(rs.getInt("started") == 1);
                data.setCurrentQuest(rs.getInt("current_quest"));
                data.setCompleted(rs.getInt("completed") == 1);
                data.addCoinsGranted(rs.getInt("coins_granted"));
                data.addCoinsSpent(rs.getInt("coins_spent"));
                data.setStartedAt(rs.getLong("started_at"));
                data.setCompletedAt(rs.getLong("completed_at"));
                
                // Load quest progress
                loadQuestProgress(data);
                
                plugin.debug("[Database] Loaded data for " + uuid);
                return data;
            }
            
        } catch (SQLException e) {
            plugin.logError("[Database] Failed to load data for " + uuid, e);
        }
        
        return null;
    }
    
    /**
     * Load quest progress for a player
     */
    private void loadQuestProgress(PlayerData data) throws SQLException {
        String sql = "SELECT * FROM quest_progress WHERE uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, data.getUuid().toString());
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int questNum = rs.getInt("quest_number");
                PlayerData.QuestProgress progress = data.getQuestProgress(questNum);
                
                progress.setStarted(rs.getInt("started") == 1);
                progress.setCompleted(rs.getInt("completed") == 1);
                progress.setStep(rs.getInt("step"));
                progress.setStartedAt(rs.getLong("started_at"));
                progress.setCompletedAt(rs.getLong("completed_at"));
                
                // Parse custom data (JSON format)
                String dataJson = rs.getString("data");
                if (dataJson != null && !dataJson.isEmpty()) {
                    parseCustomData(progress, dataJson);
                }
            }
        }
    }
    
    /**
     * Parse custom data from JSON string
     */
    private void parseCustomData(PlayerData.QuestProgress progress, String json) {
        try {
            // Simple JSON parsing for key-value pairs
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
                String[] pairs = json.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        
                        // Try to parse as boolean or number
                        if (value.equals("true")) {
                            progress.setData(key, true);
                        } else if (value.equals("false")) {
                            progress.setData(key, false);
                        } else {
                            try {
                                progress.setData(key, Integer.parseInt(value));
                            } catch (NumberFormatException e1) {
                                try {
                                    progress.setData(key, Double.parseDouble(value));
                                } catch (NumberFormatException e2) {
                                    progress.setData(key, value);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.debug("[Database] Failed to parse custom data: " + json);
        }
    }
    
    /**
     * Save player data to database
     */
    public void saveData(PlayerData data) {
        if (!isConnected()) return;
        
        CompletableFuture.runAsync(() -> {
            saveDataSync(data);
        });
    }
    
    /**
     * Save player data synchronously
     */
    public void saveDataSync(PlayerData data) {
        if (!isConnected()) return;
        
        String upsertPlayer = """
            INSERT INTO player_data (uuid, player_name, started, current_quest, completed, 
                                     coins_granted, coins_spent, started_at, completed_at, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                player_name = excluded.player_name,
                started = excluded.started,
                current_quest = excluded.current_quest,
                completed = excluded.completed,
                coins_granted = excluded.coins_granted,
                coins_spent = excluded.coins_spent,
                started_at = excluded.started_at,
                completed_at = excluded.completed_at,
                last_updated = excluded.last_updated
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(upsertPlayer)) {
            stmt.setString(1, data.getUuid().toString());
            stmt.setString(2, data.getPlayerName());
            stmt.setInt(3, data.isStarted() ? 1 : 0);
            stmt.setInt(4, data.getCurrentQuest());
            stmt.setInt(5, data.isCompleted() ? 1 : 0);
            stmt.setInt(6, data.getCoinsGranted());
            stmt.setInt(7, data.getCoinsSpent());
            stmt.setLong(8, data.getStartedAt());
            stmt.setLong(9, data.getCompletedAt());
            stmt.setLong(10, System.currentTimeMillis());
            
            stmt.executeUpdate();
            
            // Save quest progress
            saveQuestProgress(data);
            
            plugin.debug("[Database] Saved data for " + data.getUuid());
            
        } catch (SQLException e) {
            plugin.logError("[Database] Failed to save data for " + data.getUuid(), e);
        }
    }
    
    /**
     * Save quest progress for a player
     */
    private void saveQuestProgress(PlayerData data) throws SQLException {
        String upsertProgress = """
            INSERT INTO quest_progress (uuid, quest_number, started, completed, step, started_at, completed_at, data)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid, quest_number) DO UPDATE SET
                started = excluded.started,
                completed = excluded.completed,
                step = excluded.step,
                started_at = excluded.started_at,
                completed_at = excluded.completed_at,
                data = excluded.data
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(upsertProgress)) {
            for (int i = 1; i <= 6; i++) {
                PlayerData.QuestProgress progress = data.getQuestProgress(i);
                
                stmt.setString(1, data.getUuid().toString());
                stmt.setInt(2, i);
                stmt.setInt(3, progress.isStarted() ? 1 : 0);
                stmt.setInt(4, progress.isCompleted() ? 1 : 0);
                stmt.setInt(5, progress.getStep());
                stmt.setLong(6, progress.getStartedAt());
                stmt.setLong(7, progress.getCompletedAt());
                stmt.setString(8, serializeCustomData(progress));
                
                stmt.executeUpdate();
            }
        }
    }
    
    /**
     * Serialize custom data to JSON string
     */
    private String serializeCustomData(PlayerData.QuestProgress progress) {
        Map<String, Object> data = progress.getAllData();
        if (data.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Save all cached data
     */
    public void saveAll() {
        for (PlayerData data : cache.values()) {
            saveDataSync(data);
        }
        plugin.getLogger().info("[Database] Saved all player data (" + cache.size() + " players)");
    }
    
    /**
     * Unload player data (saves and removes from cache)
     */
    public void unloadData(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            saveDataSync(data);
        }
    }
    
    /**
     * Check if player has data
     */
    public boolean hasData(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return true;
        }
        
        if (!isConnected()) return false;
        
        String sql = "SELECT 1 FROM player_data WHERE uuid = ? LIMIT 1";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Delete player data
     */
    public void deleteData(UUID uuid) {
        cache.remove(uuid);
        
        if (!isConnected()) return;
        
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM player_data WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
            plugin.debug("[Database] Deleted data for " + uuid);
        } catch (SQLException e) {
            plugin.logError("[Database] Failed to delete data for " + uuid, e);
        }
    }
    
    /**
     * Get cache for direct access
     */
    public Map<UUID, PlayerData> getCache() {
        return cache;
    }
    
    /**
     * Close the database connection
     */
    public void close() {
        saveAll();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("[Database] Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.logError("[Database] Failed to close connection", e);
        }
    }
    
    /**
     * Force immediate save for a player
     */
    public void forceSave(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            saveDataSync(data);
            plugin.debug("[Database] Force saved data for " + uuid);
        }
    }
}
