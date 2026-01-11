# WDP-Start Permissions Guide

This document provides a comprehensive overview of all permission nodes available in WDP-Start, allowing server administrators to have fine-grained control over who can access various tutorial features.

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Player Permissions](#player-permissions)
3. [Admin Permissions](#admin-permissions)
4. [Bypass Permissions](#bypass-permissions)
5. [Notification Permissions](#notification-permissions)
6. [Permission Examples](#permission-examples)
7. [Best Practices](#best-practices)

---

## Overview

WDP-Start uses a hierarchical permission system. Parent permissions grant access to all child permissions below them. All permissions follow the format: `wdpstart.<category>.<action>`

**Default Behavior:**
- Regular players get base tutorial access
- Operators (OP) get full admin access
- Most player features are enabled by default

---

## Player Permissions

These permissions control what regular players can do with the tutorial system.

### Base Access
```yaml
wdpstart.use
```
- **Description:** Master permission for using the quest system
- **Default:** true
- **Includes:** wdpstart.menu, wdpstart.cancel
- **Usage:** Remove this to completely disable tutorial access for a group

### Individual Player Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `wdpstart.menu` | true | Open the quest menu with /start |
| `wdpstart.start` | true | Start the tutorial quest chain |
| `wdpstart.cancel` | true | Cancel the quest chain and receive refunds |

**Example:** To prevent players from canceling once started:
```yaml
- wdpstart.cancel
```

---

## Admin Permissions

Admin permissions allow staff members to manage and debug the tutorial system.

### Master Admin Permission
```yaml
wdpstart.admin
```
- **Description:** Grants all admin capabilities
- **Default:** op
- **Includes:** All admin.* and bypass.* permissions

### Individual Admin Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `wdpstart.admin.reload` | op | Reload configuration and messages |
| `wdpstart.admin.reset` | op | Reset a player's quest progress |
| `wdpstart.admin.complete` | op | Force-complete specific quests |
| `wdpstart.admin.setquest` | op | Set player to specific quest number |
| `wdpstart.admin.debug` | op | View debug info and particle zones |
| `wdpstart.admin.viewdata` | op | View detailed player quest data |
| `wdpstart.admin.forcestart` | op | Force tutorial for experienced players |

**Commands Associated:**
- `/start reload` → wdpstart.admin.reload
- `/start reset <player>` → wdpstart.admin.reset
- `/start complete <player> <quest>` → wdpstart.admin.complete
- `/start setquest <player> <quest>` → wdpstart.admin.setquest
- `/start debug <player>` → wdpstart.admin.debug

---

## Bypass Permissions

Bypass permissions allow skipping restrictions and requirements.

| Permission | Default | Description |
|------------|---------|-------------|
| `wdpstart.bypass.cooldown` | op | Skip quest cooldowns (if implemented) |
| `wdpstart.bypass.requirements` | op | Bypass quest requirements (level, progress) |

**Use Case:** Give to VIP players who want to test the tutorial without waiting.

---

## Notification Permissions

Control who receives notifications about tutorial events.

### Master Notification Permission
```yaml
wdpstart.notify
```
- **Description:** Receive all tutorial notifications
- **Default:** op
- **Includes:** All notify.* permissions

### Individual Notification Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `wdpstart.notify.start` | op | Notified when players start tutorial |
| `wdpstart.notify.complete` | op | Notified when players complete quests |
| `wdpstart.notify.cancel` | op | Notified when players cancel tutorial |

**Use Case:** Give to moderators monitoring new player onboarding.

---

## Permission Examples

### Example 1: Helper/Moderator Role
Grant helpers the ability to view player data and reset stuck players without full admin access:

```yaml
permissions:
  - wdpstart.admin.viewdata
  - wdpstart.admin.reset
  - wdpstart.notify
```

### Example 2: VIP Testing Role
Allow VIP players to test the tutorial multiple times:

```yaml
permissions:
  - wdpstart.use
  - wdpstart.bypass.cooldown
  - wdpstart.admin.reset  # Self-reset only
```

### Example 3: Limited Admin
Staff member who can reload configs but not modify player data:

```yaml
permissions:
  - wdpstart.admin.reload
  - wdpstart.admin.viewdata
  - wdpstart.admin.debug
```

### Example 4: Disable Tutorial for Group
Disable tutorial completely for a veteran player group:

```yaml
permissions:
  - -wdpstart.use
  - -wdpstart.menu
  - -wdpstart.start
```

### Example 5: Read-Only Monitor
Staff member who can view everything but not modify:

```yaml
permissions:
  - wdpstart.admin.viewdata
  - wdpstart.admin.debug
  - wdpstart.notify
```

---

## Best Practices

### 1. **Use Permission Groups**
Create dedicated permission groups for different roles:
- `tutorial_player` - Default player access
- `tutorial_helper` - Helper with view/reset access
- `tutorial_admin` - Full admin access

### 2. **Principle of Least Privilege**
Only grant permissions needed for the role. Avoid giving `wdpstart.admin` to anyone who doesn't need full access.

### 3. **Monitor Notifications**
Use notification permissions to track tutorial completion rates:
```yaml
# Give to analytics role
permissions:
  - wdpstart.notify.start
  - wdpstart.notify.complete
```

### 4. **Test Permission Changes**
Always test permission changes on a test server or with a test player before applying to production.

### 5. **Document Your Setup**
Keep a record of which groups have which permissions for easier troubleshooting.

### 6. **Use Negative Permissions**
To remove specific permissions from groups that inherit them:
```yaml
permissions:
  - wdpstart.*
  - -wdpstart.admin.reset  # Remove reset access
```

---

## Troubleshooting

### "You don't have permission to use this command"
**Solution:** Check if player has `wdpstart.use` or the specific command permission.

### Player can't start tutorial
**Solution:** Verify they have `wdpstart.start` permission.

### Admin commands not working
**Solution:** Ensure admin has `wdpstart.admin.<command>` or `wdpstart.admin` master permission.

### Player stuck in tutorial
**Solution:** Use `/start reset <player>` with `wdpstart.admin.reset` permission.

---

## Need Help?

- Check [README.md](README.md) for general plugin information
- Review configuration in `config.yml` and `messages.yml`
- Join our Discord: https://dsc.gg/wdp-server
- Report issues on GitHub

---

**Last Updated:** January 10, 2026
**Plugin Version:** 2.0.0+
**Document Version:** 1.0
