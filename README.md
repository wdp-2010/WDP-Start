# WDP-Start - Get Started Quest System

**Professional New Player Onboarding System for WDP Server**

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)]()
[![Minecraft](https://img.shields.io/badge/minecraft-1.21+-green.svg)]()
[![Java](https://img.shields.io/badge/java-21+-orange.svg)]()

---

## 📋 Overview

WDP-Start is a professional quest system designed to guide new players through their first experience on the WDP server. It provides an intuitive, step-by-step onboarding experience with rewards and helpful guidance.

### Key Features

- ✅ **6 Guided Quests**: Step-by-step introduction to server features
- ✅ **Clean GUI**: ⛃-style navigation bar
- ✅ **Smart Rewards**: SkillCoins, items, and tokens
- ✅ **Auto Top-up**: Ensures players can complete token purchase
- ✅ **Refundable**: Cancel anytime, get unspent coins back
- ✅ **Integration Ready**: Works with AuraSkills, WDP-Progress, and more

---

## 🎯 Quest Overview

| # | Quest Name | Objective | Reward |
|---|------------|-----------|--------|
| 1 | **Leave & Teleport** | Leave spawn, get auto-teleported | 30 SC + 3 Apples |
| 2 | **Woodcutting Kickstart** | Reach Foraging level 2 | 40 SC |
| 3 | **Track Your Journey** | Open /progress, view Statistics | 50 SC |
| 4 | **Currency Converter** | Buy 1 SkillToken from shop | 60 SC |
| 5 | **Quest Menu Guide** | View simplified quest menu | 20 SC |
| 6 | **Good Luck!** | Complete & join Discord | 100 SC + 10 ST + Items |

**Total Rewards:** 300 SkillCoins + 10 SkillTokens + Starter Items

---

## 💻 Commands

### Player Commands

| Command | Description |
|---------|-------------|
| `/quests` | Open the quest menu |
| `/quests cancel` | Cancel quests (prompts confirm) |
| `/quests cancel confirm` | Confirm cancellation |
| `/quests help` | Show help |

### Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/quests reload` | `wdpstart.admin.reload` | Reload configuration |
| `/quests reset <player>` | `wdpstart.admin.reset` | Reset player progress |
| `/quests complete <player> <1-6>` | `wdpstart.admin.complete` | Force complete quest |
| `/quests debug [player]` | `wdpstart.admin.debug` | View debug info |

---

## 🔌 API Usage

### Integration for Shop Plugins (SkillCoins, Economy)

**CRITICAL:** Before opening your shop menu, check if WDP-Start wants to show the simplified version:

```java
import com.wdp.start.api.WDPStartAPI;

public void onShopCommand(Player player) {
    // Check if simplified view should be shown
    if (WDPStartAPI.isAvailable() && WDPStartAPI.shouldShowSimplifiedShop(player)) {
        // DO NOT open your menu - WDP-Start handles it
        return;
    }
    
    // Default: Open normal shop menu
    openNormalShopMenu(player);
}
```

### Integration for Quest/Progress Plugins

```java
public void onQuestCommand(Player player) {
    // Check if simplified quest view should be shown
    if (WDPStartAPI.isAvailable() && WDPStartAPI.shouldShowSimplifiedQuestMenu(player)) {
        // DO NOT open your menu - WDP-Start handles it
        return;
    }
    
    // Default: Open normal quest menu
    openNormalQuestMenu(player);
}
```

### Notify Quest Completion

```java
// Notify token purchase for Quest 4 completion
WDPStartAPI.notifyTokenPurchase(player, 1);

// Track coin spending for refund calculation
WDPStartAPI.trackCoinSpending(player, 50);

// Notify stats click for Quest 3
WDPStartAPI.notifyProgressStatsClick(player);
```

### Check Quest Progress

```java
// Check quest status
int currentQuest = WDPStartAPI.getCurrentQuest(player);
boolean started = WDPStartAPI.hasStartedQuests(player);
boolean completed = WDPStartAPI.isQuestCompleted(player, 3);
```

---

## ⚙️ Configuration

### config.yml

```yaml
general:
  welcome-message: true      # Show welcome on join
  auto-start: false          # Quests require manual start
  enable-sounds: true        # Sound effects
  enable-particles: true     # Particle effects

quest1:
  trigger-region: "spawn_exit"  # Region outside spawn
  trigger-world: "world"
  use-coordinates: false
  # Or use coordinate box:
  # use-coordinates: true
  # min-x: -100
  # max-x: 100
  # min-z: -100
  # max-z: 100

quest2:
  skill: "foraging"
  target-level: 2
  override-reward: true

quest4:
  tokens-required: 1
  auto-topup: true           # Give coins if player is short
  token-cost: 100

quest6:
  discord-link: "https://discord.gg/yourserver"

cancellation:
  enabled: true
  refund-unspent: true       # Refund granted coins not spent
```

---

## 📦 Installation

1. **Download** the latest JAR from releases
2. **Place** in your server's `plugins/` folder
3. **Restart** the server
4. **Configure** `plugins/WDP-Start/config.yml`
5. **Reload** with `/quests reload`

### Dependencies

- **Required:** Spigot/Paper 1.21+, Java 21+
- **Optional:** Vault (economy), AuraSkills, WDP-Progress

---

## 🔧 Building from Source

```bash
cd WDP-Start
mvn clean package
```

The JAR will be in `target/WDP-Start-1.0.0.jar`

---

## 📄 License

Proprietary - WDP Server © 2025

---

## 👥 Credits

- **Design & Development:** WDP Development Team
- **Inspired by:** SkillCoins, WDP-Progress, WDP-BaseDet
