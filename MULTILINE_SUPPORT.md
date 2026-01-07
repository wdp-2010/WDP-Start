# Multiline Message Support

## Overview
WDP-Start now supports **multiline quest descriptions** in messages.yml. You can use either single-line or multi-line format for quest descriptions.

## Usage Examples

### Single-Line Format (Default)
```yaml
quests:
  descriptions:
    quest1: "Leave spawn and get teleported to the wild"
```

### Multi-Line Format (List)
```yaml
quests:
  descriptions:
    quest2:
      - "Reach Foraging level 1"
      - "&#AAAAAAChop trees to gain XP!"
```

### With Color Codes
```yaml
quests:
  descriptions:
    quest4:
      - "Convert SkillCoins to Tokens"
      - "&#FFD700Tokens unlock skill upgrades!"
      - "&#55FF55You need 100 coins per token"
```

## How It Works

1. **Single String**: If you provide a single string, it displays as-is
2. **List of Strings**: If you provide a YAML list, each line is displayed on a new line
3. **Color Support**: Both formats support hex colors (`&#RRGGBB`) and legacy color codes (`&a`, `&c`, etc.)

## Implementation Details

- The `MessageManager.getList()` method automatically handles both formats
- Single strings are converted to single-item lists internally
- Multiple lines are joined with `\n` for display in menus
- Each line is individually color-translated

## Where This Works

This multiline support works for:
- ✅ Quest descriptions (`quests.descriptions.questX`)
- ✅ Quest hints (`quests.hints.questX`)
- ✅ All other message lists in messages.yml

## Example: Full Quest Description with Multiple Lines

```yaml
quests:
  descriptions:
    quest3:
      - "&#FFD700Learn to use the shop!"
      - ""
      - "&#FFFFFF1. Type &#55FF55/shop"
      - "&#FFFFFF2. Browse categories"
      - "&#FFFFFF3. Buy any item"
      - ""
      - "&#AAAAAASpend your earned SkillCoins wisely!"
```

This will display as:
```
Learn to use the shop!

1. Type /shop
2. Browse categories
3. Buy any item

Spend your earned SkillCoins wisely!
```

## Notes

- Empty lines (just `""` or `''`) create visual spacing
- Lines are processed in order
- Automatic color translation applies to all lines
- No limit on number of lines (but keep it reasonable for UI display!)
