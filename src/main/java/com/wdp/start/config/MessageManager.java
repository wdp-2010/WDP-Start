package com.wdp.start.config;

import com.wdp.start.WDPStartPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Manages all message translations and localization
 */
public class MessageManager {
    
    private final WDPStartPlugin plugin;
    private FileConfiguration messages;
    private File messagesFile;
    
    public MessageManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load defaults from jar
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream));
            messages.setDefaults(defaultConfig);
        }
    }
    
    public void reload() {
        loadMessages();
    }
    
    /**
     * Get a message with color codes translated
     */
    public String get(String path) {
        String message = messages.getString(path);
        if (message == null) {
            return "§cMissing message: " + path;
        }
        return WDPStartPlugin.hex(message);
    }
    
    /**
     * Get a message with placeholders replaced
     */
    public String get(String path, String... placeholders) {
        String message = get(path);
        
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        
        return message;
    }
    
    /**
     * Get a list of messages
     */
    public List<String> getList(String path) {
        List<String> list = messages.getStringList(path);
        return list.stream()
            .map(WDPStartPlugin::hex)
            .toList();
    }
    
    /**
     * Get a list with placeholders replaced
     */
    public List<String> getList(String path, String... placeholders) {
        List<String> list = getList(path);
        
        return list.stream().map(line -> {
            String result = line;
            for (int i = 0; i < placeholders.length - 1; i += 2) {
                result = result.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
            return result;
        }).toList();
    }
    
    /**
     * Send a prefixed message to a player
     */
    public void send(Player player, String path) {
        player.sendMessage(getPrefix() + get(path));
    }
    
    /**
     * Send a prefixed message with placeholders
     */
    public void send(Player player, String path, String... placeholders) {
        player.sendMessage(getPrefix() + get(path, placeholders));
    }
    
    /**
     * Send a raw message (no prefix)
     */
    public void sendRaw(Player player, String path) {
        player.sendMessage(get(path));
    }
    
    /**
     * Send a raw message with placeholders (no prefix)
     */
    public void sendRaw(Player player, String path, String... placeholders) {
        player.sendMessage(get(path, placeholders));
    }
    
    /**
     * Send a list of messages (no prefix)
     */
    public void sendList(Player player, String path) {
        getList(path).forEach(player::sendMessage);
    }
    
    /**
     * Send a list of messages with placeholders (no prefix)
     */
    public void sendList(Player player, String path, String... placeholders) {
        getList(path, placeholders).forEach(player::sendMessage);
    }
    
    /**
     * Get the message prefix
     */
    public String getPrefix() {
        return get("prefix");
    }
    
    // ==================== CONVENIENCE METHODS ====================
    
    public int getWelcomeDelayTicks() {
        return messages.getInt("welcome.delay-ticks", 60);
    }
    
    public boolean isWelcomeEnabled() {
        return messages.getBoolean("welcome.enabled", true);
    }
}
