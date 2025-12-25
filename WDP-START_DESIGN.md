# WDP-Start - Get Started Quest System

**Version:** 1.0.0  
**Date:** December 20, 2025  
**Professional Quest Plugin for New Player Onboarding**

---

## 📋 Overview

WDP-Start is a professional quest system designed to guide new players through their first experience on the WDP server. It provides a clean, intuitive GUI with 6 carefully selected quests that introduce core gameplay mechanics and custom plugins.

### Key Features

- ✅ **Clean SkillCoins-style Navbar**: Professional navigation at bottom (slots 45-53)
- ✅ **6 Interactive Quests**: Sequential introduction to server features
- ✅ **Progress Tracking**: Visual indicators for quest completion
- ✅ **Reward System**: SkillCoins, SkillTokens, and items as rewards
- ✅ **Auto-tracking**: Quests automatically track player actions
- ✅ **Beautiful GUI**: Hex color codes and modern design
- ✅ **Cancellation & Refund**: Players can cancel and get unspent coins back
- ✅ **API for Integration**: Other plugins can trigger quest completion

---

## 🎯 Quest List (6 Quests for New Player Onboarding)

> **Important:** Quests do **not** auto-start. Players must opt-in via \`/quests\`. 
> Cancelling returns any granted SkillCoins that were not spent.
> A welcome banner highlights \`/quests\` when new players join.

---

### Quest 1: Leave & Teleport (Spawn Exit + Auto RTP)
**Icon:** 🚶 Grass Block  
**Category:** Exploration  
**Reward:** 100 SkillCoins + 5 Apples

**Objective:** Leave the protected spawn area and get auto-teleported to the wilderness.

**Steps:**
1. ✓ Cross the spawn boundary into the configured detection area
2. ✓ Get auto-teleported by the external RTP plugin (we listen for its teleport event)

**Detection:** 
- Listen for region-enter event (spawn outskirts area)
- Then listen for the subsequent teleport event fired by the external RTP plugin
- Order enforced: region enter must occur before the teleport
- We do **NOT** issue \`/rtp\` ourselves - another plugin handles the actual teleport

**Description:**
> "Welcome to WDP! Step beyond spawn and the server will auto-teleport you to a safe location in the wild."

---

### Quest 2: Woodcutting Kickstart (AuraSkills)
**Icon:** 🪓 Iron Axe  
**Category:** Custom Plugin (AuraSkills)  
**Reward:** 100 SkillCoins (overwrites default AuraSkills reward)

**Objective:** Level your Foraging (Woodcutting) skill to level 2.

**Steps:**
1. ✓ Gain Foraging XP by chopping trees
2. ✓ Reach Foraging level 2

**Notes:**
- Overrides the standard AuraSkills level-up reward
- Force-grants exactly 100 SkillCoins on completion
- Sends motivational prompt: "Use these coins to buy something from /shop!"

**Description:**
> "Time to gather resources! Chop some trees to level up your Foraging skill. You'll earn coins to spend in the shop!"

---

### Quest 3: Track Your Journey (WDP-Progress)
**Icon:** 📊 Experience Bottle  
**Category:** Custom Plugin (WDP-Progress)  
**Reward:** 150 SkillCoins

**Objective:** Open the progress menu and view your statistics.

**Steps:**
1. ✓ Use \`/progress\` command to open the progress menu
2. ✓ Click on the Statistics category to view your stats

**Detection:**
- Listen for \`/progress\` command execution
- Use API call \`WDPStartAPI.notifyProgressStatsClick(player)\` when player clicks Statistics

**Description:**
> "Knowledge is power! Check your progress with /progress to see how you're advancing."

---

### Quest 4: Currency Converter (SkillCoins Shop)
**Icon:** 💱 Gold Nugget  
**Category:** Custom Plugin (SkillCoins)  
**Reward:** 200 SkillCoins (+ auto top-up if player is short)

**Objective:** Purchase 1 SkillToken from the shop.

**Steps:**
1. ✓ Open the shop via \`/shop\` command
2. ✓ Purchase 1 SkillToken

**Menu Simplification (only during this quest):**
- Show only 2-3 relevant categories when player opens shop
- Token UI reduced to simple: \`[Amount Display] [+ Buy]\` buttons
- Keep the SkillCoins navbar intact (slots 45-53)
- Use \`WDPStartAPI.shouldShowSimplifiedShop(player)\` to check

**Auto Top-up:**
- If player doesn't have enough coins, automatically grant the difference
- Example: Token costs 100 coins, player has 50 → grant 50 more

**Detection:**
- Listen for \`/shop\` command
- Use API call \`WDPStartAPI.notifyTokenPurchase(player, amount)\` when purchase is made

**Description:**
> "Learn the economy! Open /shop and purchase a SkillToken. You can use tokens to buy skill upgrades!"

---

### Quest 5: Quest Menu Guide (Simplified View)
**Icon:** 📜 Book  
**Category:** Tutorial  
**Reward:** 50 SkillCoins

**Objective:** Open \`/quests\` and see the simplified quest menu.

**Steps:**
1. ✓ Open the quest menu with \`/quests\`
2. ✓ Click the "Got it!" button

**Menu Override:**
- During this quest, the menu shows only ONE quest card (Quest 5 itself)
- SkillCoins-style navbar is preserved (slots 45-53)
- Simple "Got it!" button to acknowledge and complete

**Detection:**
- Automatically opens simplified view when player types \`/quests\`
- Clicking the complete button finishes the quest

**Description:**
> "Let's make sure you know how quests work! Open /quests to see this simplified tutorial view."

---

### Quest 6: Good Luck! (Discord & Farewell)
**Icon:** 🎧 Music Disc (Cat)  
**Category:** Community  
**Reward:** 500 ⛃ + 25 🎟 + Starter Items

**Objective:** Complete the onboarding and join Discord for help.

**Steps:**
1. ✓ Click to acknowledge and complete

**Final Rewards:**
- 500 SkillCoins
- 25 SkillTokens
- 3× Diamonds
- 16× Iron Ingots
- 32× Bread

**Description:**
> "Congratulations! You've completed all starter quests. Join our Discord if you ever need help. Good luck on your adventure!"

---

## 🎨 GUI Design

### Main Quest Menu (Normal View)

**Title:** \`✦ Get Started Quests ✦\`  
**Size:** 54 slots (6 rows)

### Quest Item Slots:
- Quest 1: Slot 11
- Quest 2: Slot 13
- Quest 3: Slot 15
- Quest 4: Slot 20
- Quest 5: Slot 22
- Quest 6: Slot 24

### Info Row Slots:
- Cancel: Slot 29
- Progress Overview: Slot 31
- Help: Slot 33

### Navbar (Slots 45-53):
| Slot | Item | Name | Function |
|------|------|------|----------|
| 45 | Nether Star | \`⌂ Home\` | Refresh menu |
| 46-48 | Black Glass | \` \` | Decoration |
| 49 | Paper | \`WDP-Start\` | Plugin info |
| 50-52 | Black Glass | \` \` | Decoration |
| 53 | Barrier | \`✗ Close\` | Close menu |

---

## 🎨 Quest Item States

### Not Started (Locked)
- **Color:** Gray (§7)
- **Glow:** No
- **Lore:** Shows "🔒 Locked - Complete previous quests first"

### In Progress (Current)
- **Color:** Yellow (§e)
- **Glow:** Yes (enchant glow)
- **Lore:** Shows progress bar and steps

### Completed
- **Color:** Green (§a)
- **Glow:** Yes
- **Lore:** Shows "✓ Completed!"

---

## 💬 Commands & Permissions

### Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| \`/quests\` | Open the quest menu | \`wdpstart.menu\` |
| \`/quests cancel\` | Request cancellation | \`wdpstart.cancel\` |
| \`/quests cancel confirm\` | Confirm cancellation | \`wdpstart.cancel\` |
| \`/quests help\` | Show command help | \`wdpstart.use\` |

**Aliases:** \`/quest\`, \`/getstarted\`, \`/wdpstart\`

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| \`/quests reload\` | Reload configuration | \`wdpstart.admin.reload\` |
| \`/quests reset <player>\` | Reset player's quests | \`wdpstart.admin.reset\` |
| \`/quests complete <player> <1-6>\` | Force complete a quest | \`wdpstart.admin.complete\` |
| \`/quests debug [player]\` | View debug info | \`wdpstart.admin.debug\` |

---

## 🔌 API Endpoints

### For Other Plugins to Call

\`\`\`java
import com.wdp.start.api.WDPStartAPI;

// Quest 3: Notify stats category click
WDPStartAPI.notifyProgressStatsClick(player);

// Quest 4: Notify token purchase
WDPStartAPI.notifyTokenPurchase(player, amount);

// Quest 4: Track coin spending (for refund calculation)
WDPStartAPI.trackCoinSpending(player, amount);

// Check if simplified shop should be shown
boolean simplified = WDPStartAPI.shouldShowSimplifiedShop(player);

// Check if simplified quest menu should be shown
boolean tutorialMode = WDPStartAPI.shouldShowSimplifiedQuestMenu(player);

// Get player quest status
int currentQuest = WDPStartAPI.getCurrentQuest(player);
boolean started = WDPStartAPI.hasStartedQuests(player);
boolean completed = WDPStartAPI.hasCompletedAllQuests(player);
\`\`\`

---

## 📊 Reward Summary

| Quest | SkillCoins | SkillTokens | Items |
|-------|------------|-------------|-------|
| 1. Leave & Teleport | 100 | - | 5× Apple |
| 2. Woodcutting | 100 | - | - |
| 3. Progress | 150 | - | - |
| 4. Token | 200 | - | - |
| 5. Menu Guide | 50 | - | - |
| 6. Good Luck | 500 | 25 | 3× Diamond, 16× Iron, 32× Bread |
| **Total** | **1,100** | **25** | **Starter Kit** |

---

## �� License

Proprietary - WDP Server © 2025 WDP Development Team
