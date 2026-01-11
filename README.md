# WDP-Start

**AI Authorship Notice:** **The full plugin (WDP-Start) was written by an AI due to a time shortage.** Please review code and configs before production use.

---

## 🚀 What is WDP-Start?

WDP-Start is a focused New Player Onboarding plugin for Paper/Spigot servers. It provides a short, guided quest chain that introduces new players to server features, rewards them, and optionally integrates with SkillCoins / AuraSkills.

## ✨ Key Features

- Clear 6-step onboarding quest chain
- Clean GUI and simplified menus for new players
- Rewards: SkillCoins and SkillTokens (where configured)
- Integration points for Shop & Progress plugins
- Configurable auto-topup and refund logic

## ⚙️ Requirements

- Minecraft: Paper/Spigot 1.21+
- Java: 21+
- Optional: Vault, AuraSkills, WDP-Progress

## 📋 Commands

### Player Commands
- `/start` (aliases: `quests`, `quest`, `getstarted`, `wdpstart`) — Open the tutorial menu
- `/start cancel` — Cancel current tutorial (with refund confirmation)

### Admin Commands
- `/start reload` — Reload plugin configuration and messages
- `/start reset <player>` — Reset a player's tutorial progress
- `/start complete <player> <quest>` — Force-complete a specific quest
- `/start setquest <player> <quest>` — Set player to specific quest number
- `/start debug <player>` — View debug information and visualize zones

## 🔐 Permissions

WDP-Start uses a comprehensive permission system for fine-grained control. For complete documentation, see [PERMISSIONS.md](PERMISSIONS.md).

### Quick Reference

**Player Permissions:**
- `wdpstart.use` - Base access to tutorial system (default: true)
- `wdpstart.menu` - Open quest menu (default: true)
- `wdpstart.cancel` - Cancel quest chain (default: true)

**Admin Permissions:**
- `wdpstart.admin` - Full admin access (default: op)
- `wdpstart.admin.reload` - Reload configuration (default: op)
- `wdpstart.admin.reset` - Reset player progress (default: op)
- `wdpstart.admin.complete` - Force-complete quests (default: op)

**Full documentation:** See [PERMISSIONS.md](PERMISSIONS.md) for all permissions, examples, and best practices.

## 🧩 Integration / API

WDP-Start exposes a small API for other plugins (see `com.wdp.start.api.WDPStartAPI`). Use the API to:
- Query quest progress
- Notify about token purchases (Quest 4)
- Respect simplified menus (check `shouldShowSimplifiedShop` / `shouldShowSimplifiedQuestMenu`)

Code examples and usage are included in the original README in the `API` section.

## 🛠 Configuration

Default configuration is `plugins/WDP-Start/config.yml` (auto-created on first run). Important options:
- `general.auto-start` — Start quests automatically on first join
- `questX.*` — Per-quest triggers and requirements
- `cancellation.refund-unspent` — Refund logic for unspent coins

Always review `config.yml` after installing to adapt messages, regions, and costs.

## 📦 Installation

1. Place the built JAR in `plugins/`.
2. Start/restart the server.
3. Adjust `plugins/WDP-Start/config.yml` as needed.
4. Use `/start reload` to apply changes.

## 🧪 Building from source

Maven build:

```bash
cd WDP-Start
mvn clean package
```

Resulting artifact: `target/*.jar`

## 📣 License & Attribution

This repository copy is provided under the license stated in the project; see `LICENSE` or header files for details.

---

If you'd like, I can now apply this same layout to the other READMEs and add a short review of each file's accuracy.✅
