# WDP-Start Plugin Integration Guide

## Overview

This guide explains how other plugins should integrate with WDP-Start to provide simplified menus during quest progression.

---

## 🎯 Core Concept

**WDP-Start intercepts commands during certain quests and displays simplified menus.**

- Quest 4: `/shop` → Simplified shop with token purchase
- Quest 5: `/quests` → Simplified quest menu tutorial

**Default Behavior:** If WDP-Start is NOT installed or the API check fails, your plugin should ALWAYS open the normal menu.

---

## 📋 Integration Checklist for Shop Plugins

### Step 1: Add WDP-Start as Optional Dependency

**pom.xml:**
```xml
<dependency>
    <groupId>com.wdp</groupId>
    <artifactId>wdp-start</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

**plugin.yml:**
```yaml
softdepend: [WDP-Start]
```

### Step 2: Implement Command Check

**Before opening your shop menu:**

```java
import com.wdp.start.api.WDPStartAPI;

public void onShopCommand(Player player) {
    // Check if WDP-Start wants to show simplified view
    if (WDPStartAPI.isAvailable() && WDPStartAPI.shouldShowSimplifiedShop(player)) {
        // DO NOT OPEN YOUR MENU
        // WDP-Start will intercept the command and show simplified shop
        return;
    }
    
    // Default: Open normal shop menu
    openNormalShopMenu(player);
}
```

### Step 3: Notify Quest Completion

**When a token is purchased:**

```java
// After successful token purchase
if (WDPStartAPI.isAvailable()) {
    WDPStartAPI.notifyTokenPurchase(player, amountPurchased);
}
```

**When coins are spent:**

```java
// Track spending for refund calculation
if (WDPStartAPI.isAvailable()) {
    WDPStartAPI.trackCoinSpending(player, coinsSpent);
}
```

---

## 📋 Integration Checklist for Quest/Progress Plugins

### Step 1: Add WDP-Start Dependency (same as above)

### Step 2: Implement Command Check

**Before opening your quest/progress menu:**

```java
import com.wdp.start.api.WDPStartAPI;

public void onQuestCommand(Player player) {
    // Check if WDP-Start wants to show simplified view
    if (WDPStartAPI.isAvailable() && WDPStartAPI.shouldShowSimplifiedQuestMenu(player)) {
        // DO NOT OPEN YOUR MENU
        // WDP-Start will handle it
        return;
    }
    
    // Default: Open normal quest menu
    openNormalQuestMenu(player);
}
```

### Step 3: Notify Quest Completion

**When Statistics category is clicked (WDP-Progress):**

```java
// When player clicks Statistics button
if (WDPStartAPI.isAvailable()) {
    WDPStartAPI.notifyProgressStatsClick(player);
}
```

---

## 🔧 Complete Example: Shop Plugin Integration

```java
package com.example.shop;

import com.wdp.start.api.WDPStartAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    
    private final ShopPlugin plugin;
    
    public ShopCommand(ShopPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        
        // ===== WDP-Start Integration =====
        // Check if simplified shop should be shown
        if (WDPStartAPI.isAvailable() && WDPStartAPI.shouldShowSimplifiedShop(player)) {
            // Don't open - WDP-Start will handle the command
            return true;
        }
        // ===== End Integration =====
        
        // Default behavior: Open normal shop
        plugin.getShopMenu().open(player);
        return true;
    }
}
```

**Token Purchase Handler:**

```java
public void onTokenPurchase(Player player, int amount, int cost) {
    // Deduct coins
    economy.withdrawPlayer(player, cost);
    
    // Give tokens
    giveTokens(player, amount);
    
    // ===== WDP-Start Integration =====
    // Notify quest system about purchase
    if (WDPStartAPI.isAvailable()) {
        WDPStartAPI.notifyTokenPurchase(player, amount);
        WDPStartAPI.trackCoinSpending(player, cost);
    }
    // ===== End Integration =====
    
    player.sendMessage("§aYou purchased " + amount + " token(s)!");
}
```

---

## 🔧 Complete Example: Progress Plugin Integration

```java
package com.wdp.progress;

import com.wdp.start.api.WDPStartAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ProgressCommand implements CommandExecutor {
    
    private final ProgressPlugin plugin;
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        
        // Normal menu opens (no check needed for /progress)
        // WDP-Start doesn't intercept /progress, only /shop and /quests
        plugin.getProgressMenu().open(player);
        return true;
    }
}
```

**Statistics Click Handler:**

```java
public void onStatisticsClick(Player player, InventoryClickEvent event) {
    event.setCancelled(true);
    
    // Open statistics view
    openStatisticsView(player);
    
    // ===== WDP-Start Integration =====
    // Notify quest system about stats view
    if (WDPStartAPI.isAvailable()) {
        WDPStartAPI.notifyProgressStatsClick(player);
    }
    // ===== End Integration =====
}
```

---

## 📊 API Reference

### Check Methods

```java
// Check if API is available (plugin loaded and enabled)
boolean WDPStartAPI.isAvailable()

// Check if simplified shop should be shown (Quest 4)
boolean WDPStartAPI.shouldShowSimplifiedShop(Player player)

// Check if simplified quest menu should be shown (Quest 5)
boolean WDPStartAPI.shouldShowSimplifiedQuestMenu(Player player)
```

### Notification Methods

```java
// Notify token purchase (Quest 4 completion)
void WDPStartAPI.notifyTokenPurchase(Player player, int amount)

// Track coin spending (for refund calculation)
void WDPStartAPI.trackCoinSpending(Player player, int amount)

// Notify stats category click (Quest 3 completion)
void WDPStartAPI.notifyProgressStatsClick(Player player)
```

### Status Check Methods

```java
// Get current quest number (1-6, or 0 if not started)
int WDPStartAPI.getCurrentQuest(Player player)

// Check if player has started quests
boolean WDPStartAPI.hasStartedQuests(Player player)

// Check if player completed all quests
boolean WDPStartAPI.hasCompletedAllQuests(Player player)

// Check if specific quest is completed
boolean WDPStartAPI.isQuestCompleted(Player player, int quest)
```

---

## ⚠️ Important Notes

1. **Always check `isAvailable()` first** - The plugin might not be loaded
2. **Default to normal behavior** - If API returns false or is unavailable, show normal menus
3. **Don't throw exceptions** - Handle cases where WDP-Start is missing gracefully
4. **Use soft dependencies** - Don't require WDP-Start to function
5. **WDP-Start intercepts commands** - During Quest 4 and 5, WDP-Start uses `EventPriority.LOWEST` to intercept `/shop` and `/quests` commands

---

## 🧪 Testing

### Test Scenarios

1. **Without WDP-Start installed**
   - Shop command should open normal menu
   - No errors in console
   
2. **With WDP-Start, player NOT on Quest 4**
   - Shop command should open normal menu
   
3. **With WDP-Start, player ON Quest 4**
   - Shop command should NOT open your menu
   - WDP-Start simplified shop should appear
   
4. **Token purchase during Quest 4**
   - Token purchase should complete Quest 4
   - Player should see completion message

### Debug Logging

Enable debug mode in WDP-Start config:

```yaml
general:
  debug: true
```

This will log all API interactions to the console.

---

## 📞 Support

If you need help integrating with WDP-Start:

1. Check this guide first
2. Review the example code above
3. Enable debug logging to see API calls
4. Contact WDP Development Team

---

**WDP Development Team © 2025**
