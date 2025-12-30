package com.wdp.start.command;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for /quests
 */
public class QuestCommand implements CommandExecutor, TabCompleter {
    
    private final WDPStartPlugin plugin;
    
    public QuestCommand(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // No args - open menu
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.getMessageManager().sendRaw((Player) null, "commands.player-only");
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }
            
            if (!player.hasPermission("wdpstart.menu")) {
                plugin.getMessageManager().send(player, "commands.no-permission");
                return true;
            }
            
            plugin.getQuestMenu().openMainMenu(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help" -> handleHelp(sender);
            case "cancel" -> handleCancel(sender, args);
            case "start" -> handleStart(sender, args);
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            case "complete" -> handleComplete(sender, args);
            case "setquest" -> handleSetQuest(sender, args);
            case "debug" -> handleDebug(sender, args);
            default -> {
                if (sender instanceof Player player) {
                    plugin.getMessageManager().send(player, "commands.unknown");
                } else {
                    sender.sendMessage("§cUnknown subcommand.");
                }
            }
        }
        
        return true;
    }
    
    private void handleHelp(CommandSender sender) {
        if (sender instanceof Player player) {
            plugin.getMessageManager().sendList(player, "commands.help");
        } else {
            sender.sendMessage("§6WDP-Start Commands:");
            sender.sendMessage("§f/start §7- Open quests menu");
            sender.sendMessage("§f/start cancel §7- Cancel quest chain");
            sender.sendMessage("§f/start reload §7- Reload config (admin)");
            sender.sendMessage("§f/start reset <player> §7- Reset player (admin)");
        }
    }
    
    private void handleCancel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        
        if (!player.hasPermission("wdpstart.cancel")) {
            plugin.getMessageManager().send(player, "commands.no-permission");
            return;
        }
        
        boolean confirmed = args.length > 1 && args[1].equalsIgnoreCase("confirm");
        plugin.getQuestManager().cancelQuests(player, confirmed);
    }
    
    private void handleStart(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        
        if (!player.hasPermission("wdpstart.use")) {
            plugin.getMessageManager().send(player, "commands.no-permission");
            return;
        }
        
        // Check for force parameter
        boolean force = false;
        if (args.length > 1 && "force".equalsIgnoreCase(args[1])) {
            if (!player.isOp()) {
                plugin.getMessageManager().send(player, "commands.no-permission");
                return;
            }
            force = true;
        }
        
        plugin.getQuestManager().startQuests(player, force);
    }
    
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("wdpstart.admin.reload")) {
            if (sender instanceof Player player) {
                plugin.getMessageManager().send(player, "commands.no-permission");
            } else {
                sender.sendMessage("§cNo permission!");
            }
            return;
        }
        
        plugin.reload();
        
        if (sender instanceof Player player) {
            plugin.getMessageManager().send(player, "admin.reload");
        } else {
            sender.sendMessage("§aConfiguration reloaded!");
        }
    }
    
    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wdpstart.admin.reset")) {
            if (sender instanceof Player player) {
                plugin.getMessageManager().send(player, "commands.no-permission");
            } else {
                sender.sendMessage("§cNo permission!");
            }
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /quests reset <player>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            if (sender instanceof Player player) {
                plugin.getMessageManager().send(player, "admin.player-not-found", "player", args[1]);
            } else {
                sender.sendMessage("§cPlayer not found: " + args[1]);
            }
            return;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getData(target);
        data.reset();
        
        if (sender instanceof Player player) {
            plugin.getMessageManager().send(player, "admin.reset", "player", target.getName());
        } else {
            sender.sendMessage("§aReset quest progress for " + target.getName());
        }
    }
    
    private void handleComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wdpstart.admin.complete")) {
            if (sender instanceof Player player) {
                plugin.getMessageManager().send(player, "commands.no-permission");
            } else {
                sender.sendMessage("§cNo permission!");
            }
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /quests complete <player> <quest>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            if (sender instanceof Player player) {
                plugin.getMessageManager().send(player, "admin.player-not-found", "player", args[1]);
            } else {
                sender.sendMessage("§cPlayer not found: " + args[1]);
            }
            return;
        }
        
        int quest;
        try {
            quest = Integer.parseInt(args[2]);
            if (quest < 1 || quest > 6) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            if (sender instanceof Player player) {
                plugin.getMessageManager().send(player, "admin.invalid-quest", "quest", args[2]);
            } else {
                sender.sendMessage("§cInvalid quest number: " + args[2]);
            }
            return;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getData(target);
        
        // Ensure quest chain is started
        if (!data.isStarted()) {
            data.setStarted(true);
            data.setCurrentQuest(1);
        }
        
        // Complete up to and including the specified quest
        for (int i = 1; i <= quest; i++) {
            data.getQuestProgress(i).setCompleted(true);
        }
        
        // Set current quest to next one (or mark completed)
        if (quest >= 6) {
            data.setCompleted(true);
            data.setCurrentQuest(6);
        } else {
            data.setCurrentQuest(quest + 1);
            data.getQuestProgress(quest + 1).setStarted(true);
        }
        
        if (sender instanceof Player player) {
            plugin.getMessageManager().send(player, "admin.complete", 
                "quest", String.valueOf(quest), 
                "player", target.getName());
        } else {
            sender.sendMessage("§aForce completed quest " + quest + " for " + target.getName());
        }
    }
    
    private void handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wdpstart.admin.debug")) {
            if (sender instanceof Player player) {
                plugin.getMessageManager().send(player, "commands.no-permission");
            } else {
                sender.sendMessage("§cNo permission!");
            }
            return;
        }
        
        // If just "/quests debug" - toggle portal zone visualization
        if (args.length == 1 && sender instanceof Player player) {
            plugin.getPortalZoneManager().toggleDebug(player);
            return;
        }
        
        // "/quests debug <player>" - show player info
        Player target;
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§cUsage: /quests debug <player>");
            return;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getData(target);
        
        sender.sendMessage("§6§l=== WDP-Start Debug: " + target.getName() + " ===");
        sender.sendMessage("§7Started: §f" + data.isStarted());
        sender.sendMessage("§7Current Quest: §f" + data.getCurrentQuest());
        sender.sendMessage("§7Completed: §f" + data.isCompleted());
        sender.sendMessage("§7Coins Granted: §f" + data.getCoinsGranted());
        sender.sendMessage("§7Coins Spent: §f" + data.getCoinsSpent());
        sender.sendMessage("§7Refundable: §f" + data.getRefundableCoins());
        
        // Portal zone status
        sender.sendMessage("§7In Portal Zone: §f" + plugin.getPortalZoneManager().isPlayerInZone(target.getUniqueId()));
        sender.sendMessage("§7Debug Mode: §f" + plugin.getPortalZoneManager().hasDebugEnabled(target.getUniqueId()));
        
        for (int i = 1; i <= 6; i++) {
            PlayerData.QuestProgress progress = data.getQuestProgress(i);
            String status = progress.isCompleted() ? "§a✓" : (progress.isStarted() ? "§e⚡" : "§7○");
            sender.sendMessage("§7Quest " + i + ": " + status + " §7Step: §f" + progress.getStep());
        }
        
        // Show portal zone info
        sender.sendMessage("");
        sender.sendMessage("§7§l--- Portal Zone ---");
        
        if (plugin.getPortalZoneManager() != null) {
            boolean useWG = plugin.getConfigManager().isPortalZoneUseWorldGuard();
            if (useWG) {
                sender.sendMessage("§7Mode: §fWorldGuard Region");
                sender.sendMessage("§7Region: §f" + plugin.getConfigManager().getPortalZoneWorldGuardRegion());
            } else {
                sender.sendMessage("§7Mode: §fCoordinate Box");
                sender.sendMessage("§7Zone: §f(" + 
                    plugin.getPortalZoneManager().getMinX() + ", " + plugin.getPortalZoneManager().getMinY() + ", " + plugin.getPortalZoneManager().getMinZ() + 
                    ") to (" + 
                    plugin.getPortalZoneManager().getMaxX() + ", " + plugin.getPortalZoneManager().getMaxY() + ", " + plugin.getPortalZoneManager().getMaxZ() + ")");
            }
            sender.sendMessage("§7World: §f" + plugin.getPortalZoneManager().getZoneWorld());
        }
        
        sender.sendMessage("§eTip: Use §f/quests debug §ewithout args to toggle zone visualization");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "cancel", "start");
            
            if (sender.hasPermission("wdpstart.admin.reload")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("reload");
            }
            if (sender.hasPermission("wdpstart.admin.reset")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("reset");
            }
            if (sender.hasPermission("wdpstart.admin.complete")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("complete");
                subCommands.add("setquest");
            }
            if (sender.hasPermission("wdpstart.admin.debug")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("debug");
            }
            
            return subCommands.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            
            if (sub.equals("cancel")) {
                return Arrays.asList("confirm").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            if (sub.equals("start") && sender.isOp()) {
                return Arrays.asList("force").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            if (sub.equals("reset") || sub.equals("complete") || sub.equals("debug") || sub.equals("setquest")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3 && (args[0].equalsIgnoreCase("complete") || args[0].equalsIgnoreCase("setquest"))) {
            return Arrays.asList("1", "2", "3", "4", "5", "6").stream()
                .filter(s -> s.startsWith(args[2]))
                .collect(Collectors.toList());
        }
        
        return completions;
    }
    
    private void handleSetQuest(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wdpstart.admin.complete")) {
            if (sender instanceof Player player) {
                plugin.getMessageManager().send(player, "commands.no-permission");
            } else {
                sender.sendMessage("§cNo permission!");
            }
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /quests setquest <player> <quest>");
            sender.sendMessage("§7Sets the player to start at a specific quest (1-6)");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }
        
        int quest;
        try {
            quest = Integer.parseInt(args[2]);
            if (quest < 1 || quest > 6) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid quest number: " + args[2] + " (must be 1-6)");
            return;
        }
        
        plugin.getQuestManager().setQuest(target, quest);
        
        sender.sendMessage("§aSet " + target.getName() + " to quest " + quest + " (" + 
            plugin.getQuestManager().getQuestName(quest) + ")");
        target.sendMessage("§eAdmin set your quest to: §f" + plugin.getQuestManager().getQuestName(quest));
    }
}
