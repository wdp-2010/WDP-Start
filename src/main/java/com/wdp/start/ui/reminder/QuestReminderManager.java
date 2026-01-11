package com.wdp.start.ui.reminder;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import com.wdp.start.ui.menu.MenuUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified quest reminder system for Quest 5 and Quest 6.
 * 
 * Handles:
 * - Immediate instructions when quest becomes active
 * - Timed reminder messages
 * - Auto-complete after configurable delay
 * - Blinking animation triggers
 * 
 * Replaces the duplicated Quest 5 and Quest 6 reminder systems.
 */
public class QuestReminderManager {
    
    private final WDPStartPlugin plugin;
    
    // Track scheduled tasks per player per quest
    private final ConcurrentHashMap<UUID, Set<Integer>> reminderTasks = new ConcurrentHashMap<>();
    
    public QuestReminderManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Trigger reminders for a quest
     * 
     * @param player The player
     * @param quest The quest number (5 or 6)
     */
    public void triggerReminders(Player player, int quest) {
        if (quest != 5 && quest != 6) {
            plugin.debug("[QuestReminder] Invalid quest for reminders: " + quest);
            return;
        }
        
        // Cancel any existing reminders first
        cancelReminders(player);
        
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Guard: Only trigger if this quest is current and not completed
        if (data.getCurrentQuest() != quest || data.isQuestCompleted(quest)) {
            plugin.debug("[QuestReminder] Skipping - quest " + quest + " is not current or already completed");
            return;
        }
        
        // Initialize task set for this player
        reminderTasks.put(uuid, ConcurrentHashMap.newKeySet());
        
        // Get config values based on quest
        ReminderConfig config = getReminderConfig(quest);
        
        // Send immediate instruction
        sendImmediateInstruction(player, quest);
        
        // Schedule first reminder
        if (config.reminderDelay > 0) {
            scheduleFirstReminder(player, quest, config.reminderDelay);
        }
        
        // Schedule final reminder
        if (config.finalReminderDelay > 0) {
            scheduleFinalReminder(player, quest, config.finalReminderDelay);
        }
        
        // Schedule auto-complete
        if (config.autoCompleteDelay > 0) {
            scheduleAutoComplete(player, quest, config.autoCompleteDelay);
        }
        
        plugin.debug("[QuestReminder] Triggered reminders for quest " + quest + " (player: " + player.getName() + ")");
    }
    
    /**
     * Cancel all reminders for a player
     */
    public void cancelReminders(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Integer> tasks = reminderTasks.remove(uuid);
        
        if (tasks != null && !tasks.isEmpty()) {
            for (Integer taskId : tasks) {
                try {
                    plugin.getServer().getScheduler().cancelTask(taskId);
                } catch (Exception e) {
                    // Task may already be cancelled
                }
            }
            plugin.debug("[QuestReminder] Cancelled " + tasks.size() + " reminder tasks for " + player.getName());
        }
    }
    
    /**
     * Cancel all reminders (for plugin disable)
     */
    public void cancelAll() {
        for (UUID uuid : reminderTasks.keySet()) {
            Set<Integer> tasks = reminderTasks.remove(uuid);
            if (tasks != null) {
                for (Integer taskId : tasks) {
                    try {
                        plugin.getServer().getScheduler().cancelTask(taskId);
                    } catch (Exception e) {
                        // Task may already be cancelled
                    }
                }
            }
        }
        plugin.debug("[QuestReminder] Cancelled all reminder tasks");
    }
    
    /**
     * Check if player has active reminders
     */
    public boolean hasActiveReminders(Player player) {
        Set<Integer> tasks = reminderTasks.get(player.getUniqueId());
        return tasks != null && !tasks.isEmpty();
    }
    
    // ==================== INTERNAL SCHEDULING ====================
    
    private void sendImmediateInstruction(Player player, int quest) {
        String instructionKey = "quest.quest" + quest + ".instruction";
        String detailKey = "quest.quest" + quest + ".instruction-detail";
        
        player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get(instructionKey)));
        player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get(detailKey)));
    }
    
    private void scheduleFirstReminder(Player player, int quest, int delaySeconds) {
        UUID uuid = player.getUniqueId();
        String messageKey = "quest.quest" + quest + ".reminder-10s";
        
        int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            PlayerData data = plugin.getPlayerDataManager().getData(player);
            if (data.getCurrentQuest() != quest || data.isQuestCompleted(quest)) return;
            
            player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get(messageKey)));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
            
        }, delaySeconds * 20L).getTaskId();
        
        Set<Integer> tasks = reminderTasks.get(uuid);
        if (tasks != null) {
            tasks.add(taskId);
        }
    }
    
    private void scheduleFinalReminder(Player player, int quest, int delaySeconds) {
        UUID uuid = player.getUniqueId();
        String messageKey = "quest.quest" + quest + ".reminder-30s";
        
        int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            PlayerData data = plugin.getPlayerDataManager().getData(player);
            if (data.getCurrentQuest() != quest || data.isQuestCompleted(quest)) return;
            
            player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get(messageKey)));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.7f);
            
        }, delaySeconds * 20L).getTaskId();
        
        Set<Integer> tasks = reminderTasks.get(uuid);
        if (tasks != null) {
            tasks.add(taskId);
        }
    }
    
    private void scheduleAutoComplete(Player player, int quest, int delaySeconds) {
        UUID uuid = player.getUniqueId();
        String messageKey = "quest.quest" + quest + ".auto-complete";
        
        int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            PlayerData data = plugin.getPlayerDataManager().getData(player);
            if (data.getCurrentQuest() != quest || data.isQuestCompleted(quest)) return;
            
            player.sendMessage(MenuUtils.hex(plugin.getMessageManager().get(messageKey)));
            plugin.getQuestManager().completeQuest(player, quest);
            player.closeInventory();
            
            // Clean up tasks
            reminderTasks.remove(uuid);
            
        }, delaySeconds * 20L).getTaskId();
        
        Set<Integer> tasks = reminderTasks.get(uuid);
        if (tasks != null) {
            tasks.add(taskId);
        }
    }
    
    // ==================== CONFIGURATION ====================
    
    private ReminderConfig getReminderConfig(int quest) {
        if (quest == 5) {
            return new ReminderConfig(
                plugin.getConfigManager().getQuest5ReminderDelay(),
                plugin.getConfigManager().getQuest5FinalReminderDelay(),
                plugin.getConfigManager().getQuest5AutoCompleteDelay()
            );
        } else if (quest == 6) {
            return new ReminderConfig(
                plugin.getConfigManager().getQuest6ReminderDelay(),
                plugin.getConfigManager().getQuest6FinalReminderDelay(),
                plugin.getConfigManager().getQuest6AutoCompleteDelay()
            );
        }
        return new ReminderConfig(10, 30, 0);
    }
    
    private record ReminderConfig(int reminderDelay, int finalReminderDelay, int autoCompleteDelay) {}
}
