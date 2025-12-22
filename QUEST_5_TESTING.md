# Quest 5 Testing Guide

## Overview
Quest 5 uses the exact WDP-Quest menu structure with functional stone mining objective and proper command routing.

## Changes Made

### 1. Command Routing (FIXED)
- `/start` → Always opens normal WDP-Start quest overview menu (6 quests grid)
- `/quest` → Opens WDP-Quest style menu (ONLY when on Quest 5)

### 2. Quest 5 Menu Structure
- **Format**: Double chest (54 slots)
- **Layout**:
  - Slot 0: Quest icon (compass → emerald when complete)
  - Slots 1-8: 8 progress bar segments with Custom Model Data
  - Slot 20: Objective display
  - **Row 5 (slots 45-53): WDP-Quest navbar**
    - Slot 45: Page info (1/1)
    - Slot 46: Player head (coins, tokens)
    - Slot 48: Back button (← Back)
    - Slot 49: Complete button (only works when objective met)
    - Slot 53: Close button

### 3. Stone Mining Objective
- **Goal**: Mine 5 stone blocks
- **Tracked Types**:
  - Stone
  - Cobblestone
  - Deepslate
  - Cobbled Deepslate
  - Andesite
  - Diorite
  - Granite
  - Smooth Stone
  - Stone Bricks
  - Infested Stone

### 4. Progress Tracking
- Counter: `stone_mined` in PlayerData
- Real-time updates in menu
- 8 progress bar segments fill based on percentage (0-100%)
- Custom Model Data: 1000-1005 (green/normal quest)

### 5. Completion Flow
1. Player reaches Quest 5
2. Type `/start` → See normal 6-quest overview
3. Type `/quest` → See WDP-Quest style menu
4. Mine 5 stone blocks
5. Type `/quest` again to see updated progress
6. Click complete button (slot 49) when ready
7. Quest 5 completes

## Testing Checklist

### Basic Flow
- [ ] Start quests with `/start`
- [ ] Complete Quests 1-4
- [ ] Type `/start` on Quest 5 → See NORMAL quest grid (6 quests)
- [ ] Type `/quest` on Quest 5 → See WDP-Quest menu
- [ ] Close menu and mine stone

### WDP-Quest Menu Elements
- [ ] Navbar at bottom (slots 45-53) with black glass panes
- [ ] Page indicator shows "Page 1/1"
- [ ] Player head shows coins and tokens
- [ ] Back button (slot 48) returns to main menu
- [ ] Complete button (slot 49) is gray when incomplete
- [ ] Complete button (slot 49) is emerald when complete
- [ ] Close button (slot 53) closes menu

### Stone Mining
- [ ] Mine regular stone → Counter increments
- [ ] Mine cobblestone → Counter increments
- [ ] Mine deepslate → Counter increments
- [ ] Mine other stone types → Counter increments
- [ ] Check non-stone blocks ignored

### Progress Display
- [ ] Open `/quest` after mining 0 stone → 0% progress
- [ ] Mine 1 stone → 20% progress (1-2 segments filled)
- [ ] Mine 2-3 stone → 40-60% progress
- [ ] Mine 5 stone → 100% progress (all 8 segments filled)
- [ ] Quest icon changes to emerald with glow

### Completion
- [ ] Try clicking complete before mining 5 → Error message
- [ ] Mine 5 stone → Complete button turns emerald
- [ ] Click complete button → Quest completes
- [ ] Quest 6 becomes available

## Expected Messages

### Stone Mining
```
⛏ Stone Mined: 1/5
⛏ Stone Mined: 2/5
...
⛏ Stone Mined: 5/5

§l✓ Objective Complete!
Type /quest to complete Quest 5!
```

### Early Completion Attempt
```
✖ You must mine 5 stone first! (0/5)
```

## Known Features
- Progress bars use Custom Model Data for resource pack compatibility
- Fallback text bars (█ and ░) work without resource pack
- All stone types tracked automatically
- Menu updates require reopening `/quest`
- Navbar matches WDP-Quest plugin exactly

## Files Modified
- `QuestMenu.java`: Added WDP-Quest menu structure with navbar, removed bad simplified menu
- `QuestListener.java`: Command routing + stone mining tracker
- `PlayerData.java`: Counter methods for progress tracking
