package com.wdp.start.quest;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Manages quest logic, progression, and rewards
 */
public class QuestManager {
    
    private final WDPStartPlugin plugin;
    
    // Quest definitions
    public static final int TOTAL_QUESTS = 6;
    
    public QuestManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start the quest chain for a player
     */
    public void startQuests(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (data.isCompleted()) {
            plugin.getMessageManager().send(player, "quest.already-completed");
            return;
        }
        
        // If not started at all, start the first quest
        if (!data.isStarted()) {
            // Check if player is already experienced (Foraging level 1 or higher)
            String targetSkill = plugin.getConfigManager().getQuest2Skill();
            int targetLevel = plugin.getConfigManager().getQuest2TargetLevel();
            if (plugin.getAuraSkillsIntegration() != null) {
                int currentLevel = plugin.getAuraSkillsIntegration().getSkillLevel(player, targetSkill);
                if (currentLevel >= targetLevel) {
                    plugin.getMessageManager().send(player, "quest.already-experienced");
                    return;
                }
            }
            
            data.setStarted(true);
            data.setCurrentQuest(1);
            data.getQuestProgress(1).setStarted(true);
            
            // Save data IMMEDIATELY to database
            plugin.getPlayerDataManager().saveData(data);
            plugin.getPlayerDataManager().forceSave(player.getUniqueId());
            
            plugin.getMessageManager().send(player, "quest.started");
            
            // Console debug
            plugin.getLogger().info("[DEBUG] [QuestManager] " + player.getName() + " STARTED quest chain. Saved to database.");
            
            // Start particle path guide for Quest 1
            if (plugin.getPathGuideManager() != null) {
                plugin.debug("Starting particle path guide for " + player.getName());
                plugin.getPathGuideManager().startPath(player);
            }
            
            // Play sound
            if (plugin.getConfigManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            
            // Open the quest menu
            plugin.getQuestMenu().openMainMenu(player);
            return;
        }
        
        // If already started, check if we can start the next quest
        int currentQuest = data.getCurrentQuest();
        if (currentQuest < TOTAL_QUESTS) {
            PlayerData.QuestProgress nextProgress = data.getQuestProgress(currentQuest + 1);
            if (nextProgress != null && !nextProgress.isStarted()) {
                // Start the next quest
                data.setCurrentQuest(currentQuest + 1);
                nextProgress.setStarted(true);
                
                // Save data IMMEDIATELY to database
                plugin.getPlayerDataManager().saveData(data);
                plugin.getPlayerDataManager().forceSave(player.getUniqueId());
                
                plugin.getMessageManager().send(player, "quest.next-started", "quest", String.valueOf(currentQuest + 1));
                
                // Console debug
                plugin.getLogger().info("[DEBUG] [QuestManager] " + player.getName() + " STARTED next quest: " + (currentQuest + 1));
                
                // Play sound
                if (plugin.getConfigManager().isSoundsEnabled()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                
                // Open the quest menu
                plugin.getQuestMenu().openMainMenu(player);
                return;
            }
        }
        
        // Already on the last quest or next quest already started
        plugin.getMessageManager().send(player, "quest.already-started");
    }
    
    /**
     * Cancel the quest chain for a player
     */
    public void cancelQuests(Player player, boolean confirmed) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (!data.isStarted() || data.isCompleted()) {
            plugin.getMessageManager().send(player, "cancel.cannot-cancel");
            return;
        }
        
        if (!confirmed) {
            plugin.getMessageManager().send(player, "cancel.confirm");
            return;
        }
        
        // Calculate refund
        int refund = 0;
        if (plugin.getConfigManager().isRefundUnspent()) {
            refund = data.getRefundableCoins();
            
            if (refund > 0 && plugin.getVaultIntegration() != null) {
                // Take back the coins
                plugin.getVaultIntegration().withdraw(player, refund);
                plugin.getMessageManager().send(player, "cancel.refunded", "amount", String.valueOf(refund));
            } else {
                plugin.getMessageManager().sendRaw(player, "cancel.no-refund");
            }
        }
        
        // Reset progress
        data.reset();
        
        // Save data
        plugin.getPlayerDataManager().saveData(data);
        
        plugin.getMessageManager().send(player, "cancel.cancelled");
        
        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }
    
    /**
     * Complete a quest step
     */
    public void completeStep(Player player, int quest, int step, String stepKey) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (!data.isStarted() || data.getCurrentQuest() != quest) {
            return;
        }
        
        PlayerData.QuestProgress progress = data.getQuestProgress(quest);
        
        // Check if already completed this step
        if (progress.hasData(stepKey) && (boolean) progress.getData(stepKey)) {
            return;
        }
        
        // Mark step complete
        progress.setData(stepKey, true);
        progress.advanceStep();
        
        // Save data IMMEDIATELY to database
        plugin.getPlayerDataManager().saveData(data);
        plugin.getPlayerDataManager().forceSave(player.getUniqueId());
        
        // Console debug
        plugin.getLogger().info("[DEBUG] [QuestManager] " + player.getName() + 
            " completed STEP " + step + " of Quest " + quest + " (" + stepKey + "). Saved to database.");
        
        // Send step completion message
        String messagePath = "quest.quest" + quest + ".step" + step;
        plugin.getMessageManager().sendRaw(player, messagePath);
        
        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }
        
        plugin.debug("Player " + player.getName() + " completed step " + step + " of quest " + quest);
    }
    
    /**
     * Complete a quest entirely
     */
    public void completeQuest(Player player, int quest) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (!data.isStarted() || data.getCurrentQuest() != quest) {
            return;
        }
        
        // Mark quest completed
        PlayerData.QuestProgress progress = data.getQuestProgress(quest);
        progress.setCompleted(true);
        
        // Give rewards
        int reward = giveQuestRewards(player, quest);
        
        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        
        // Stop particle path guide when Quest 1 is completed
        if (quest == 1 && plugin.getPathGuideManager() != null) {
            plugin.debug("Stopping particle path guide for " + player.getName() + " - Quest 1 complete");
            plugin.getPathGuideManager().stopPath(player);
        }
        
        // Advance to next quest or complete
        if (quest < TOTAL_QUESTS) {
            // Auto-start the next quest so the player sees the next objective (e.g., Foraging) as started
            data.setCurrentQuest(quest + 1);
            data.getQuestProgress(quest + 1).setStarted(true);

            // Send consistent completion message (skip for Quest 2 which has its own flow)
            if (quest != 2) {
                sendQuestCompleteMessage(player, quest, reward);
            }

            // Special handling: if quest 1 just completed, check and auto-complete quest 2 if player already meets requirements
            if (quest == 1) {
                checkAndCompleteQuest2IfReady(player, data);
            }
        } else {
            // All quests complete!
            data.setCompleted(true);
            onAllQuestsComplete(player);
        }
        
        // Save data IMMEDIATELY to database
        plugin.getPlayerDataManager().saveData(data);
        plugin.getPlayerDataManager().forceSave(player.getUniqueId());
        
        // Console debug
        plugin.getLogger().info("[DEBUG] [QuestManager] " + player.getName() + 
            " COMPLETED Quest " + quest + ". Progress saved to database.");
        
        plugin.debug("Player " + player.getName() + " completed quest " + quest);
    }
    
    /**
     * Give rewards for completing a quest
     * @return the amount of skillcoins given
     */
    private int giveQuestRewards(Player player, int quest) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        int coins = switch (quest) {
            case 1 -> plugin.getConfigManager().getQuest1SkillCoins();
            case 2 -> plugin.getConfigManager().getQuest2SkillCoins();
            case 3 -> plugin.getConfigManager().getQuest3SkillCoins();
            case 4 -> plugin.getConfigManager().getQuest4SkillCoins();
            case 5 -> plugin.getConfigManager().getQuest5SkillCoins();
            case 6 -> 0; // Final rewards handled separately
            default -> 0;
        };
        
        if (coins > 0 && plugin.getVaultIntegration() != null) {
            plugin.getVaultIntegration().deposit(player, coins);
            data.addCoinsGranted(coins);
        }
        
        // Give items for specific quests
        List<String> items = switch (quest) {
            case 1 -> plugin.getConfigManager().getQuest1Items();
            default -> List.of();
        };
        
        for (String itemStr : items) {
            giveItem(player, itemStr);
        }
        
        return coins;
    }
    
    /**
     * Handle all quests complete
     */
    private void onAllQuestsComplete(Player player) {
        // Send completion messages
        String discord = plugin.getConfigManager().getDiscordLink();
        plugin.getMessageManager().sendList(player, "quest.all-complete", "discord", discord);
        
        // Give final rewards
        int coins = plugin.getConfigManager().getQuest6SkillCoins();
        int tokens = plugin.getConfigManager().getQuest6SkillTokens();
        
        if (plugin.getVaultIntegration() != null && coins > 0) {
            plugin.getVaultIntegration().deposit(player, coins);
        }
        
        // Give SkillTokens via AuraSkills if available
        if (plugin.getAuraSkillsIntegration() != null && tokens > 0) {
            plugin.getAuraSkillsIntegration().giveSkillTokens(player, tokens);
        }
        
        // Give items
        for (String itemStr : plugin.getConfigManager().getQuest6Items()) {
            giveItem(player, itemStr);
        }
        
        // Play fanfare
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            // Delayed second sound for effect
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
            }, 10L);
        }
        
        // Particles
        if (plugin.getConfigManager().isParticlesEnabled()) {
            // Spawn firework particles around player
            player.getWorld().spawnParticle(
                org.bukkit.Particle.FIREWORK,
                player.getLocation().add(0, 1, 0),
                50, 0.5, 0.5, 0.5, 0.1
            );
        }
    }
    
    /**
     * Give an item to a player from string format (MATERIAL:AMOUNT)
     */
    private void giveItem(Player player, String itemStr) {
        try {
            String[] parts = itemStr.split(":");
            Material mat = Material.valueOf(parts[0].toUpperCase());
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            
            ItemStack item = new ItemStack(mat, amount);
            
            // Try to add to inventory, drop if full
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(item);
            } else {
                player.getWorld().dropItem(player.getLocation(), item);
            }
            
            plugin.getMessageManager().sendRaw(player, "rewards.item", 
                "amount", String.valueOf(amount),
                "item", formatMaterialName(mat));
                
        } catch (Exception e) {
            plugin.logError("Failed to give item: " + itemStr, e);
        }
    }
    
    /**
     * Format material name for display
     */
    private String formatMaterialName(Material mat) {
        String name = mat.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
    /**
     * Check if player can access a quest
     */
    public boolean canAccessQuest(Player player, int quest) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        if (!data.isStarted()) {
            return false;
        }
        
        return data.getCurrentQuest() >= quest;
    }
    
    /**
     * Get quest name
     */
    public String getQuestName(int quest) {
        return switch (quest) {
            case 1 -> "Leave Spawn";
            case 2 -> "Foraging Kickstart";
            case 3 -> "Shop Tutorial";
            case 4 -> "Token Exchange";
            case 5 -> "Quest Menu";
            case 6 -> "Good Luck!";
            default -> "Unknown";
        };
    }
    
    /**
     * Get quest icon
     */
    public Material getQuestIcon(int quest) {
        return switch (quest) {
            case 1 -> Material.GRASS_BLOCK;
            case 2 -> Material.IRON_AXE;
            case 3 -> Material.EMERALD;
            case 4 -> Material.GOLD_INGOT;
            case 5 -> Material.COMPASS;
            case 6 -> Material.MUSIC_DISC_CAT;
            default -> Material.PAPER;
        };
    }
    
    /**
     * Get quest description
     */
    public String getQuestDescription(int quest) {
        return switch (quest) {
            case 1 -> "Leave spawn and get teleported to the wild";
            case 2 -> "Reach Foraging level 1";
            case 3 -> "Open /shop and buy an item";
            case 4 -> "Convert SkillCoins to Tokens";
            case 5 -> "Open /quests and complete the task";
            case 6 -> "Join Discord for help!";
            default -> "Unknown";
        };
    }
    
    /**
     * Get total steps for a quest
     */
    public int getQuestSteps(int quest) {
        return switch (quest) {
            case 1 -> 2; // Enter region + get teleported
            case 2 -> 1; // Reach level 2
            case 3 -> 1; // Complete simplified task
            case 4 -> 1; // Convert tokens
            case 5 -> 1; // Complete simple objective
            case 6 -> 1; // Click complete
            default -> 1;
        };
    }
    
    /**
     * Get reward coins for a quest
     */
    public int getQuestRewardCoins(int quest) {
        return switch (quest) {
            case 1 -> plugin.getConfigManager().getQuest1SkillCoins();
            case 2 -> plugin.getConfigManager().getQuest2SkillCoins();
            case 3 -> plugin.getConfigManager().getQuest3SkillCoins();
            case 4 -> plugin.getConfigManager().getQuest4SkillCoins();
            case 5 -> plugin.getConfigManager().getQuest5SkillCoins();
            case 6 -> plugin.getConfigManager().getQuest6SkillCoins();
            default -> 0;
        };
    }
    
    /**
     * Auto top-up coins if player doesn't have enough for quest 4
     */
    public void autoTopUpIfNeeded(Player player) {
        if (!plugin.getConfigManager().isQuest4AutoTopup()) {
            return;
        }
        
        if (plugin.getVaultIntegration() == null) {
            return;
        }
        
        int tokenCost = plugin.getConfigManager().getQuest4TokenCost();
        double balance = plugin.getVaultIntegration().getBalance(player);
        
        if (balance < tokenCost) {
            int needed = (int) Math.ceil(tokenCost - balance);
            plugin.getVaultIntegration().deposit(player, needed);
            
            PlayerData data = plugin.getPlayerDataManager().getData(player);
            data.addCoinsGranted(needed);
            
            plugin.getMessageManager().send(player, "quest.quest4.topup", "amount", String.valueOf(needed));
        }
    }
    
    /**
     * Set player to a specific quest (admin command)
     * This allows jumping to any quest for testing
     */
    public void setQuest(Player player, int quest) {
        if (quest < 1 || quest > TOTAL_QUESTS) {
            return;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Start if not started
        if (!data.isStarted()) {
            data.setStarted(true);
        }
        
        // Mark all previous quests as completed
        for (int i = 1; i < quest; i++) {
            PlayerData.QuestProgress progress = data.getQuestProgress(i);
            progress.setCompleted(true);
            progress.setStep(getQuestSteps(i));
        }
        
        // Reset current and future quests
        for (int i = quest; i <= TOTAL_QUESTS; i++) {
            PlayerData.QuestProgress progress = data.getQuestProgress(i);
            progress.setCompleted(false);
            progress.setStep(0);
            progress.clearData();
        }
        
        // Set current quest
        data.setCurrentQuest(quest);
        data.setCompleted(false);
        data.getQuestProgress(quest).setStarted(true);
        
        // Save
        plugin.getPlayerDataManager().saveData(data);
        
        plugin.debug("Admin set " + player.getName() + " to quest " + quest);
    }
    
    /**
     * Check if a player is currently doing Quest 2
     * Used by AuraSkills integration to suppress messages
     */
    public boolean isPlayerDoingQuest2(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        return data.isStarted() && 
               data.getCurrentQuest() == 2 && 
               !data.isQuestCompleted(2);
    }
    
    /**
     * Check if player is already at target level for Quest 2
     * If so, complete it immediately
     */
    private void checkAndCompleteQuest2IfReady(Player player, PlayerData data) {
        if (data.getCurrentQuest() != 2 || data.isQuestCompleted(2)) {
            return;
        }
        
        String targetSkill = plugin.getConfigManager().getQuest2Skill();
        int targetLevel = plugin.getConfigManager().getQuest2TargetLevel();
        
        // Check current level via AuraSkills
        if (plugin.getAuraSkillsIntegration() != null) {
            int currentLevel = plugin.getAuraSkillsIntegration().getSkillLevel(player, targetSkill);
            
            if (currentLevel >= targetLevel) {
                plugin.debug("Player " + player.getName() + " already at " + targetSkill + " level " + currentLevel + " - auto-completing Quest 2");
                
                // Mark progress
                data.getQuestProgress(2).setData("level_progress", 100.0);
                
                // Complete Quest 2 with delay
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    completeQuest(player, 2);
                    sendAuraSkillsStyleLevelUp(player, targetSkill, currentLevel);
                }, 20L); // 1 second delay
            }
        }
    }
    
    /**
     * Called when player levels up a skill - handles Quest 2
     * Overrides the standard AuraSkills level up message
     */
    public void onSkillLevelUp(Player player, String skill, int newLevel) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Only process Quest 2
        if (!data.isStarted() || data.getCurrentQuest() != 2 || data.isQuestCompleted(2)) {
            return;
        }
        
        String targetSkill = plugin.getConfigManager().getQuest2Skill();
        int targetLevel = plugin.getConfigManager().getQuest2TargetLevel();
        
        if (!skill.equalsIgnoreCase(targetSkill)) {
            return;
        }
        
        // Store progress percentage
        double progress = (double) newLevel / targetLevel * 100;
        data.getQuestProgress(2).setData("level_progress", Math.min(progress, 100));
        
        if (newLevel >= targetLevel) {
            // Temporarily suppress AuraSkills' own messages to avoid race conditions
            data.setSuppressLevelUpUntil(System.currentTimeMillis() + 2000);
            // Clear suppression after a short delay to avoid lingering state
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                data.clearSuppressLevelUp();
                plugin.getPlayerDataManager().saveData(data);
            }, 40L); // 40 ticks = 2 seconds

            // Complete Quest 2
            completeQuest(player, 2);
            
            // Send AuraSkills-style level up message with next objective
            sendAuraSkillsStyleLevelUp(player, skill, newLevel);
        }
        
        // Save progress
        plugin.getPlayerDataManager().saveData(data);
    }
    
    /**
     * Send AuraSkills-style level up message that shows completion and next objective
     * This overrides the standard SkillCoins/AuraSkills level up message
     */
    private void sendAuraSkillsStyleLevelUp(Player player, String skill, int level) {
        int reward = plugin.getConfigManager().getQuest2SkillCoins();
        
        // Clear screen with empty lines
        player.sendMessage("");
        player.sendMessage("");
        
        // AuraSkills-style header
        player.sendMessage(WDPStartPlugin.hex("&#3CB371▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
        
        // Skill level up announcement
        String skillName = skill.substring(0, 1).toUpperCase() + skill.substring(1).toLowerCase();
        player.sendMessage(WDPStartPlugin.hex("  &#FFFFFF&l" + skillName.toUpperCase() + " &#55FF55&lLEVEL UP"));
        player.sendMessage(WDPStartPlugin.hex("  &#AAAAAA" + skillName + " &#FFFFFF" + (level - 1) + " &#AAAAAA→ &#55FF55" + level));
        player.sendMessage("");
        
        // Quest completion
        player.sendMessage(WDPStartPlugin.hex("  &#FFD700✦ &#55FF55&lQUEST COMPLETE: &#FFFFFFForaging Kickstart"));
        player.sendMessage(WDPStartPlugin.hex("  &#AAAAAAYou earned &#FFD700" + reward + " ⛃&#AAAAAA!"));
        player.sendMessage("");
        
        // Next objective - consistent AuraSkills style
        player.sendMessage(WDPStartPlugin.hex("  &#55FFFF&l➤ NEXT OBJECTIVE"));
        player.sendMessage(WDPStartPlugin.hex("  &#FFFFFFOpen &#FFFF55/shop &#FFFFFFand buy an item"));
        player.sendMessage(WDPStartPlugin.hex("  &#AAAAAASpend Skillcoins in the shop!"));
        player.sendMessage("");
        
        // Footer
        player.sendMessage(WDPStartPlugin.hex("&#3CB371▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
        
        // Play AuraSkills-style level up sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            // Delayed celebration sound
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
            }, 5L);
        }
    }
    
    /**
     * Send consistent next objective message (AuraSkills style)
     */
    public void sendNextObjectiveMessage(Player player, int nextQuest) {
        String objective = switch (nextQuest) {
            case 2 -> "Chop trees to reach Foraging level 1";
            case 3 -> "Open /shop and buy an item";
            case 4 -> "Open /shop and exchange tokens";
            case 5 -> "Open /quests to see your progress";
            case 6 -> "Click to complete and join Discord!";
            default -> "Continue your adventure!";
        };
        
        player.sendMessage("");
        player.sendMessage(WDPStartPlugin.hex("&#3CB371▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
        player.sendMessage(WDPStartPlugin.hex("  &#55FFFF&l➤ NEXT OBJECTIVE"));
        player.sendMessage(WDPStartPlugin.hex("  &#FFFFFF" + objective));
        player.sendMessage("");
        player.sendMessage(WDPStartPlugin.hex("&#3CB371▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }
    
    /**
     * Send consistent quest complete message with next objective (AuraSkills style)
     */
    public void sendQuestCompleteMessage(Player player, int quest, int reward) {
        String questName = getQuestName(quest);
        int nextQuest = quest + 1;
        
        String nextObjective = switch (nextQuest) {
            case 2 -> "Chop trees to reach Foraging level 1";
            case 3 -> "Open &#FFFF55/shop &#FFFFFFand buy an item";
            case 4 -> "Open &#FFFF55/shop &#FFFFFFand exchange tokens";
            case 5 -> "Open &#FFFF55/quests &#FFFFFFto see your progress";
            case 6 -> "Click to complete and join Discord!";
            default -> "Continue your adventure!";
        };
        
        player.sendMessage("");
        player.sendMessage(WDPStartPlugin.hex("&#3CB371▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
        player.sendMessage(WDPStartPlugin.hex("  &#FFD700✦ &#55FF55&lQUEST COMPLETE"));
        player.sendMessage(WDPStartPlugin.hex("  &#FFFFFF" + questName));
        if (reward > 0) {
            player.sendMessage(WDPStartPlugin.hex("  &#AAAAAAYou earned &#FFD700" + reward + " ⛃&#AAAAAA!"));
        }
        player.sendMessage("");
        
        if (nextQuest <= TOTAL_QUESTS) {
            player.sendMessage(WDPStartPlugin.hex("  &#55FFFF&l➤ NEXT OBJECTIVE"));
            player.sendMessage(WDPStartPlugin.hex("  &#FFFFFF" + nextObjective));
            player.sendMessage("");
        }
        
        player.sendMessage(WDPStartPlugin.hex("&#3CB371▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
        
        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        }
    }

    /**
     * Auto top-up coins for Quest 3 (shop) if player doesn't have enough
     */
    public void autoTopUpForShop(Player player) {
        if (!plugin.getConfigManager().isQuest3EnsureBalance()) {
            return;
        }
        
        if (plugin.getVaultIntegration() == null) {
            return;
        }
        
        int minRequired = plugin.getConfigManager().getQuest3EnsureBalanceAmount();
        double balance = plugin.getVaultIntegration().getBalance(player);
        
        if (balance < minRequired) {
            int needed = (int) Math.ceil(minRequired - balance);
            plugin.getVaultIntegration().deposit(player, needed);
            
            PlayerData data = plugin.getPlayerDataManager().getData(player);
            data.addCoinsGranted(needed);
            
            plugin.getMessageManager().send(player, "quest.quest3.topup", "amount", String.valueOf(needed));
        }
    }
}
