# WDP Server Features

> AI-developed plugins delivering intelligent automation, RPG depth, and community-defining events.

---

## Intelligent Base Detection & Protection

An advanced security framework that **automatically identifies and fortifies player bases** without manual claims. The system monitors player behavior in real-time, grouping actions into spatial clusters.

### How Detection Works

| Activity | Score Contribution |
|----------|-------------------|
| Bed placement | +50 points |
| Chest/Container | +25 points |
| Block placement | +5 points |
| Block breaking | +1 point |
| Strip mining | **-90% penalty** |

```
Activity clustering algorithm:
• Groups nearby player actions into clusters
• Max 5 clusters tracked per player
• New cluster creates when >200 blocks away from existing
• Score threshold triggers detection (default: 150)
```

### Smart Classification

The system **distinguishes between bases and mining operations** using pattern analysis:

- **Pure Base** → Full score retention
- **Mining Operation** → 90% score penalty (no false triggers!)
- **Hybrid Area** → 50% score penalty

### Protection System

| Trust Tier | When Owner Online | When Owner Offline |
|------------|-------------------|-------------------|
| Full Access | Break/Place/Containers | Break/Place/Containers |
| Limited | Cannot open containers | Cannot interact at all |

**Example:**
```yaml
/trust add BestFriend        # 8 permission types available
/trust remove ExFriend       # Revoke anytime
/trust list                  # See all trusted players
```

### Teleportation (v2.0+)

**Smart fallback routing** — tries locations in order:

1. `/base sethome` → Player's custom home point
2. Bed location → Safe spot near bed
3. Base center → Highest solid block at cluster center
4. Spiral search → Checks 5-block radius for safe landing

---

## RPG Skill Progression (AuraSkills)

Deep character development transforming Minecraft into a true RPG.

### Skill Categories

| Skill | Primary Benefit |
|-------|-----------------|
| Mining | Efficiency, fortune bonuses |
| Combat | Damage, crit chance, health |
| Farming | Growth speed, drop multipliers |
| Foraging | Tree felling, apple drops |
| Fishing | Treasure catches, luck |
| Enchanting | XP efficiency, book drops |

### Stat Modifiers (Permanent!)

```
Skill Level 10 → +10% damage
Skill Level 50 → +50% damage + 2 hearts
Skill Level 100 → +100% damage + 5 hearts + speed boost
```

### Ability System

**Active Abilities** (mana cost, strategic timing):
- Double jump
- Shield bash
- Ground smash
- Berserk mode

**Passive Abilities** (always active):
- Critical strikes
- Health regeneration
- Mining fortune
- Experience multiplier

### Equipment Evaluation

WDP-Progress tracks your best gear:

| Material | Progress Points | Enchantment Multiplier |
|----------|-----------------|----------------------|
| Netherite | 10 pts | ×2.5 (mending +150%) |
| Diamond | 8 pts | ×2.4 (fortune +140%) |
| Iron | 3 pts | ×1.5 |
| Stone | 1 pt | ×1.0 |

**Bonus:** Complete armor set = +15% bonus

---

## Progressive Quest System (250 Quests!)

Quest access is **tied to server-wide progress** — the entire community unlocks content together.

### Quest Tier Unlock Schedule

| Tier | Server Progress Required | Quests | Rewards |
|------|-------------------------|--------|---------|
| Beginner | 0-20% | 50 quests | 50-200 coins |
| Early | 20-40% | 50 quests | 150-500 coins |
| Intermediate | 40-60% | 50 quests | 300-800 coins |
| Advanced | 60-80% | 50 quests | 500-1500 coins |
| Expert | 80-100% | 50 quests | 750-2500 coins |

### Objective Types

- ⛏️ **Mining** — Break specific blocks
- ⚔️ **Combat** — Kill designated mobs
- 🔨 **Crafting** — Create items
- 🏗️ **Building** — Place block types
- 🔮 **Enchanting** — Enchant items
- 🏪 **Trading** — Villager trades
- 🎣 **Fishing** — Catch rare fish
- ❤️ **Breeding** — Breed animals
- 📊 **Level Up** — Gain XP levels

### Quest Rewards

```yaml
"Hard" quest bonus: +500 coins per completion
Daily quest refresh: 4:00 AM server time
Cooldown system: Prevents exploit farming
Multi-objective quests: 2-3 tasks simultaneously
```

---

## Rogue Wanderer Events

**Legendary community boss** that roams the world requiring coordinated player effort.

### Event Mechanics

```
Rogue Wanderer spawns → Random location
Server receives hint → /hint reveals coordinates
Players hunt together → Defeat = rewards
Broadcast at 15:00 → Daily update + post-defeat
```

### Exclusive Rewards

| Reward | Description |
|--------|-------------|
| 🟦 **Beam Stone** | Prestige item + functional tool |
| 🎁 Loot drops | Rare materials |
| 📢 Server-wide recognition | "Player X defeated the Rogue Wanderer!" |

---

## Secure Death Recovery

Never lose items again. Graves persist **indefinitely** with owner-only access.

### How It Works

```
Player dies → Grave creates at exact location
All items stored → Protected container
Only owner access → Theft-proof
No despawn timer → Recover anytime
```

---

## Multi-Dimensional Progress Tracking (1-100 Score)

WDP-Progress calculates a holistic player advancement score.

### Category Weights

| Category | Weight | What It Measures |
|----------|--------|------------------|
| Advancements | 25% | Story progression (nether, dragon, elytra) |
| Equipment | 20% | Best gear in inventory + ender chest |
| Experience | 15% | XP levels with diminishing returns |
| Economy | 15% | Coin balance (logarithmic scale) |
| Statistics | 15% | Kills, blocks mined, distance traveled |
| Achievements | 10% | Custom server milestones |

### Progress Tiers

| Score | Rank | Milestone |
|-------|------|-----------|
| 1-20 | Beginner | Just started |
| 21-40 | Novice | Iron age |
| 41-60 | Intermediate | Diamond tools, nether established |
| 61-80 | Advanced | End dimension, elytra |
| 81-99 | Expert | Netherite, wither defeated |
| 100 | Master | Complete mastery |

### Example Point Values

```yaml
Kill Ender Dragon:      +20 points
Enter Nether:           +10 points
Obtain Elytra:          +15 points
Full Diamond Armor:     +32 points (8×4)
Full Netherite Armor:   +40 points (10×4)
$100 balance:           ~20% of economy category
$1,000,000 balance:     100% of economy category
```

---

## New Player Onboarding (WDP-Start)

Structured 6-step tutorial with rewards for new players.

### Tutorial Flow

1. 🎬 **Welcome** — Introduction to server
2. 💰 **Economy** — Earn first SkillCoins
3. 🛒 **Shop** — Browse marketplace
4. ⚔️ **Skills** — Unlock AuraSkills
5. 🎯 **Quests** — Complete first quest
6. 🏆 **Rewards** — Bonus tokens + completion celebration

### Features

```yaml
Auto-start on first join:      Enabled
Starting rewards:              100 coins + 1 token
Simplified GUIs:               Yes
Refund on cancel:              Yes
Admin reset:                   /start reset <player>
```

---

## AI-Powered Help System (WDP-Help)

OpenRouter-powered assistant with server-specific context.

### Relevance Scoring (0-10)

| Score | Category | Example |
|-------|----------|---------|
| 9-10 | Server-specific | "How do I set base home?" |
| 7-8 | Gameplay-related | "What's the best mining skill?" |
| 4-6 | General Minecraft | "How do I make obsidian?" |
| 1-3 | Loosely related | "What version is this?" |
| 0 | Off-topic | "What's for lunch?" |

### Context System

**Always Included:**
- Server info & rules
- Command documentation
- Skills overview
- Quest system

**Fetchable on Demand:**
- Rogue Wanderer mechanics
- Base detection details
- Economy systems

### Example Interaction

```
Player: /help How do I teleport home?
AI: Use /home after setting a home with /sethome!
     [Response saved to history]

Player: /help
→ Shows recent questions with short descriptions
```

---

## SkillCoins Economy

Primary currency (⛃) integrated with AuraSkills.

### Balance Tiers

| Tier | Price Range | Examples |
|------|-------------|----------|
| Abundant | 0.5-3 coins | Stone, dirt, cobble |
| Common | 3-10 coins | Iron ingots, wood |
| Uncommon | 10-30 coins | Crafted items, coal |
| Rare | 30-100 coins | Diamonds, enchanted books |
| Legendary | 500-2000 coins | Netherite, boss drops |
| Token Items | 1-10 tokens | Skill levels, premium |

### Transaction Commands

```bash
/pay PlayerName 500        # Transfer coins
/skillcoins balance        # Check balance
/sc bal                    # Alias
/shop                      # Open marketplace
```

### Economy Philosophy

```
Buy price = Fair value
Sell price = 20-30% of buy
Never cheaper to buy than gather
AI rebalancer maintains consistency
```

---

## Teleportation Network

| Command | Purpose |
|---------|---------|
| `/spawn` | Instant hub access |
| `/sethome` | Set personal waypoint |
| `/home` | Teleport to waypoint |
| `/rtp` / `/wild` | Random wilderness |
| `/tpa PlayerName` | Request teleport |
| `/back` | Return to previous location |

### Requirements

```yaml
/home system:      Discord account link required
/tpa cooldown:     60 seconds
/rtp cooldown:     300 seconds
/back limit:       Last 3 positions
```

---

## Discord Integration

Link accounts for enhanced features.

### Linking Process

```
1. In-game: /discord link
2. Receive: 6-digit code
3. Discord: DM bot with code
4. Done:    Accounts linked
```

### Benefits

- 📢 In-game chat → Discord channel
- 🎮 Discord roles → Minecraft ranks
- 🔐 Enhanced /home security
- 💬 Cross-platform communication

---

## Team System

Form alliances and manage groups.

### Team Commands

```bash
/team create MySquad          # Create team
/team invite PlayerName       # Add members
/team kick PlayerName         # Remove member
/team chat                    # Private channel
/team info                    # View team status
/team leave                   # Leave current team
```

### Team Features

- Private chat channels
- Shared team progress
- Easy member management
- Team-based quests (future)

---

## Creative Sandbox

Dedicated plots world for building without limits.

### Plot Commands

```bash
/plot auto                    # Get nearest free plot
/plot claim                  # Claim current plot
/plot name "My Build"        # Name your plot
/plot trust Friend           # Give build access
```

### World Features

- Full creative mode
- No resource gathering needed
- Protected from griefing
- 100×100 plot size
- Infinite plots available

---

## Technical Foundation

| Specification | Requirement |
|---------------|-------------|
| Server Platform | Paper/Spigot 1.21+ |
| Java Version | Java 21+ |
| Development | AI-Assisted (Copilot + Claude) |
| Plugin Version | 2.0.0 (unified) |
| Database | SQLite (default) or MySQL |

### Deployment

```bash
# Deploy to DEV server
./deploy.sh

# Deploy to MAIN server
./deploy.sh main
```

### API Integrations

| System | Purpose |
|--------|---------|
| WDP-Progress | Quest unlock tracking |
| AuraSkills | Skill data + SkillCoins |
| Vault | Economy transactions |
| PlaceholderAPI | Dynamic placeholders |

---

> All plugins developed with AI assistance under human oversight. Review code before production use.
