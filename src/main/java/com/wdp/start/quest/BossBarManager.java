package com.wdp.start.quest;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages boss bars for quest progress display
 */
public class BossBarManager {

    private final WDPStartPlugin plugin;
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();

    public BossBarManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Show or update boss bar for a player's current quest
     */
    public void updateBossBar(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);

        // Hide boss bar if player hasn't started quests or has completed all
        if (!data.isStarted() || data.isCompleted()) {
            hideBossBar(player);
            return;
        }

        int currentQuest = data.getCurrentQuest();
        if (currentQuest < 1 || currentQuest > 6) {
            hideBossBar(player);
            return;
        }

        // Get quest progress
        PlayerData.QuestProgress progress = data.getQuestProgress(currentQuest);
        if (!progress.isStarted()) {
            hideBossBar(player);
            return;
        }

        // Calculate progress and message
        double progressPercent;
        if (currentQuest == 2 || currentQuest == 5) {
            // Quest-specific progress calculations
            progressPercent = calculateProgress(currentQuest, progress);
        } else {
            // Overall progress: completed quests count / 6 (plus a small indicator if the current quest is started)
            int completed = plugin.getPlayerDataManager().getData(player).getCompletedQuestCount();
            double inProgressFraction = (progress.isStarted() && !progress.isCompleted()) ? 0.5 : 0.0;
            progressPercent = Math.min(100.0, ((completed + inProgressFraction) / 6.0) * 100.0);
        }
        String message = getBossBarMessage(currentQuest, progress);

        // Create or update boss bar
        BossBar bossBar = activeBossBars.get(player.getUniqueId());
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
            bossBar.addPlayer(player);
            activeBossBars.put(player.getUniqueId(), bossBar);
        }

        // Set color: Quest 5 completion should be green to draw attention
        // For Quest 5, treat "objective complete" (stone_mined >= target) as completed state to highlight it
        if (currentQuest == 5) {
            int stoneMined = progress.getCounter("stone_mined", 0);
            if (stoneMined >= 5) {
                bossBar.setColor(BarColor.GREEN);
            } else {
                bossBar.setColor(BarColor.BLUE);
            }
        } else {
            bossBar.setColor(BarColor.BLUE);
        }

        bossBar.setTitle(message);
        bossBar.setProgress(progressPercent / 100.0);
        bossBar.setVisible(true);
    }

    /**
     * Hide boss bar for a player
     */
    public void hideBossBar(Player player) {
        BossBar bossBar = activeBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
    }

    /**
     * Hide boss bar for a player by UUID (for logout handling)
     */
    public void hideBossBar(UUID playerId) {
        BossBar bossBar = activeBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
    }

    /**
     * Calculate progress percentage for a quest
     */
    private double calculateProgress(int quest, PlayerData.QuestProgress progress) {
        if (progress.isCompleted()) {
            return 100.0;
        }

        switch (quest) {
            case 2: // Foraging level
                // Check if level_progress is stored
                if (progress.hasData("level_progress")) {
                    return (double) progress.getData("level_progress");
                }
                // Otherwise, assume 50% if started
                return 50.0;

            case 5: // Mine stone
                int stoneMined = progress.getCounter("stone_mined", 0);
                return Math.min(100.0, (stoneMined * 100.0) / 5.0);

            default:
                // For other quests, use simple progress: 0% not started, 50% started, 100% completed
                return progress.isStarted() ? 50.0 : 0.0;
        }
    }

    /**
     * Get boss bar message for a quest
     */
    private String getBossBarMessage(int quest, PlayerData.QuestProgress progress) {
        String baseMessage = plugin.getMessageManager().get("bossbar.quest" + quest);

        if (quest == 5) {
            // Special handling for quest 5: Mine 5 Stone Blocks - Progress: {current}/5
            int current = progress.getCounter("stone_mined", 0);
            if (current >= 5) {
                // Objective reached: instruct player to open /quests and click the emerald until they click it
                return plugin.getMessageManager().get("bossbar.quest5-completed");
            }
            return baseMessage.replace("{current}", String.valueOf(current));
        } else {
            // For other quests: Progress: {progress}%
            int progressPercent = (int) Math.round(calculateProgress(quest, progress));
            return baseMessage.replace("{progress}", String.valueOf(progressPercent));
        }
    }

    /**
     * Clean up all boss bars (for plugin disable)
     */
    public void cleanup() {
        for (BossBar bossBar : activeBossBars.values()) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        activeBossBars.clear();
    }
}