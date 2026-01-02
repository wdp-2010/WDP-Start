package com.wdp.start.path;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.integration.WorldGuardIntegration;
import com.wdp.start.player.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages the portal zone detection and visualization
 * Handles debug mode highlighting and player entry detection
 */
public class PortalZoneManager {
    
    private final WDPStartPlugin plugin;
    private final Set<UUID> debugModeEnabled = new HashSet<>();
    private final Map<UUID, BukkitTask> highlightTasks = new HashMap<>();
    private final Set<UUID> playersInZone = new HashSet<>();
    
    // Zone configuration
    private boolean useWorldGuard;
    private String worldGuardRegion;
    private int minX, maxX, minY, maxY, minZ, maxZ;
    private String zoneWorld;
    
    public PortalZoneManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
        loadZoneBounds();
    }
    
    /**
     * Load portal zone bounds from config
     */
    public void loadZoneBounds() {
        this.useWorldGuard = plugin.getConfigManager().isPortalZoneUseWorldGuard();
        this.worldGuardRegion = plugin.getConfigManager().getPortalZoneWorldGuardRegion();
        this.zoneWorld = plugin.getConfigManager().getPortalZoneWorld();
        
        if (useWorldGuard) {
            plugin.debug("[PortalZone] Using WorldGuard region: " + worldGuardRegion + " in " + zoneWorld);
            
            // Try to get bounds from WorldGuard for debug visualization
            WorldGuardIntegration wg = plugin.getWorldGuardIntegration();
            if (wg != null && wg.isEnabled()) {
                World world = Bukkit.getWorld(zoneWorld);
                if (world != null) {
                    WorldGuardIntegration.RegionBounds bounds = wg.getRegionBounds(world, worldGuardRegion);
                    if (bounds != null) {
                        this.minX = bounds.minX;
                        this.maxX = bounds.maxX;
                        this.minY = bounds.minY;
                        this.maxY = bounds.maxY;
                        this.minZ = bounds.minZ;
                        this.maxZ = bounds.maxZ;
                        plugin.getLogger().info("[PortalZone] WorldGuard region bounds: " + bounds);
                    } else {
                        plugin.getLogger().warning("[PortalZone] WorldGuard region '" + worldGuardRegion + "' not found!");
                    }
                }
            } else {
                plugin.getLogger().warning("[PortalZone] WorldGuard integration not available!");
            }
        } else {
            this.minX = plugin.getConfigManager().getPortalZoneMinX();
            this.maxX = plugin.getConfigManager().getPortalZoneMaxX();
            this.minY = plugin.getConfigManager().getPortalZoneMinY();
            this.maxY = plugin.getConfigManager().getPortalZoneMaxY();
            this.minZ = plugin.getConfigManager().getPortalZoneMinZ();
            this.maxZ = plugin.getConfigManager().getPortalZoneMaxZ();
            
            plugin.debug("[PortalZone] Using coordinate box: " + 
                String.format("(%d, %d, %d) to (%d, %d, %d) in %s",
                    minX, minY, minZ, maxX, maxY, maxZ, zoneWorld));
        }
    }
    
    /**
     * Toggle debug mode for a player
     */
    public boolean toggleDebug(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (debugModeEnabled.contains(uuid)) {
            disableDebug(player);
            return false;
        } else {
            enableDebug(player);
            return true;
        }
    }
    
    /**
     * Enable debug mode with portal zone highlighting
     */
    public void enableDebug(Player player) {
        UUID uuid = player.getUniqueId();
        debugModeEnabled.add(uuid);
        
        // Start highlighting task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                disableDebug(player);
                return;
            }
            
            showZoneHighlight(player);
        }, 0L, 10L); // Every 0.5 seconds
        
        highlightTasks.put(uuid, task);
        
        // Send info message
        player.sendMessage("");
        player.sendMessage(WDPStartPlugin.hex("&#55FF55&l✓ Debug Mode Enabled"));
        player.sendMessage(WDPStartPlugin.hex("&#AAAAAAPortal zone is now highlighted with particles"));
        
        if (useWorldGuard) {
            player.sendMessage(WDPStartPlugin.hex("&#AAAAAAMode: &#FFFFFF WorldGuard Region"));
            player.sendMessage(WDPStartPlugin.hex("&#AAAAAARegion: &#FFFFFF" + worldGuardRegion));
            player.sendMessage(WDPStartPlugin.hex("&#AAAAAAWorld: &#FFFFFF" + zoneWorld));
        } else {
            player.sendMessage(WDPStartPlugin.hex("&#AAAAAAMode: &#FFFFFF Coordinate Box"));
            player.sendMessage(WDPStartPlugin.hex("&#AAAAAAZone: &#FFFFFF" + 
                String.format("(%d, %d, %d) to (%d, %d, %d)", minX, minY, minZ, maxX, maxY, maxZ)));
            player.sendMessage(WDPStartPlugin.hex("&#AAAAAAWorld: &#FFFFFF" + zoneWorld));
        }
        
        player.sendMessage(WDPStartPlugin.hex("&#FFFF55Use /quests debug again to disable"));
        player.sendMessage("");
        
        plugin.debug("Enabled portal zone highlight for " + player.getName());
    }
    
    /**
     * Disable debug mode
     */
    public void disableDebug(Player player) {
        UUID uuid = player.getUniqueId();
        debugModeEnabled.remove(uuid);
        
        BukkitTask task = highlightTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        
        player.sendMessage(WDPStartPlugin.hex("&#FF5555&l✗ Debug Mode Disabled"));
        plugin.debug("Disabled portal zone highlight for " + player.getName());
    }
    
    /**
     * Show the portal zone with particle highlighting
     */
    private void showZoneHighlight(Player player) {
        World world = Bukkit.getWorld(zoneWorld);
        if (world == null || !player.getWorld().equals(world)) {
            return;
        }
        
        // Draw the zone edges with particles
        Particle particle = Particle.FLAME;
        Particle cornerParticle = Particle.SOUL_FIRE_FLAME;
        
        double step = 1.0; // Particle spacing
        
        // Draw vertical edges (corners)
        for (int y = minY; y <= maxY; y += 2) {
            // All 4 corner pillars
            spawnParticle(player, cornerParticle, minX, y, minZ);
            spawnParticle(player, cornerParticle, maxX, y, minZ);
            spawnParticle(player, cornerParticle, minX, y, maxZ);
            spawnParticle(player, cornerParticle, maxX, y, maxZ);
        }
        
        // Draw horizontal edges at bottom and top
        for (int yLevel : new int[]{minY, maxY}) {
            // X edges
            for (double x = minX; x <= maxX; x += step) {
                spawnParticle(player, particle, x, yLevel, minZ);
                spawnParticle(player, particle, x, yLevel, maxZ);
            }
            
            // Z edges
            for (double z = minZ; z <= maxZ; z += step) {
                spawnParticle(player, particle, minX, yLevel, z);
                spawnParticle(player, particle, maxX, yLevel, z);
            }
        }
        
        // Draw middle highlight to show the floor/area
        for (double x = minX + 0.5; x < maxX; x += 2) {
            for (double z = minZ + 0.5; z < maxZ; z += 2) {
                player.spawnParticle(Particle.HAPPY_VILLAGER, 
                    new Location(world, x, minY + 0.5, z), 1, 0, 0, 0, 0);
            }
        }
    }
    
    /**
     * Spawn a particle for a player
     */
    private void spawnParticle(Player player, Particle particle, double x, double y, double z) {
        World world = Bukkit.getWorld(zoneWorld);
        if (world == null) return;
        
        player.spawnParticle(particle, new Location(world, x + 0.5, y + 0.5, z + 0.5), 
            1, 0.05, 0.05, 0.05, 0);
    }
    
    /**
     * Check if a player is in the portal zone
     */
    public boolean isInPortalZone(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        
        if (!loc.getWorld().getName().equalsIgnoreCase(zoneWorld)) {
            return false;
        }
        
        // Use WorldGuard if enabled
        if (useWorldGuard) {
            WorldGuardIntegration wg = plugin.getWorldGuardIntegration();
            if (wg != null && wg.isEnabled()) {
                return wg.isInRegion(loc, worldGuardRegion);
            } else {
                plugin.debug("[PortalZone] WorldGuard enabled but integration not available");
                return false;
            }
        }
        
        // Use coordinate box
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
    
    /**
     * Handle player movement - check for zone entry
     */
    public void onPlayerMove(Player player, Location to) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        
        // Only track for Quest 1
        if (!data.isStarted() || data.getCurrentQuest() != 1) {
            return;
        }
        
        boolean wasInZone = playersInZone.contains(uuid);
        boolean isNowInZone = isInPortalZone(to);
        
        if (!wasInZone && isNowInZone) {
            // Player just entered the zone
            playersInZone.add(uuid);
            onPlayerEnterZone(player);
        } else if (wasInZone && !isNowInZone) {
            // Player left the zone without teleporting
            playersInZone.remove(uuid);
            plugin.debug("[PortalZone] " + player.getName() + " left the portal zone without teleporting");
        }
    }
    
    /**
     * Called when a player enters the portal zone
     */
    private void onPlayerEnterZone(Player player) {
        // Debug message
        plugin.debug("[PortalZone] " + player.getName() + 
            " ENTERED the portal zone at " + formatLocation(player.getLocation()));
        plugin.debug("[PortalZone] Waiting for teleport event...");
        
        // Send message to player
        player.sendMessage("");
        player.sendMessage(WDPStartPlugin.hex("&#55FF55&l✦ Portal Zone Entered! &#55FF55&l✦"));
        player.sendMessage(WDPStartPlugin.hex("&#AAAAAAStep into the portal to continue..."));
        player.sendMessage("");
        
        // Play sound
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.0f);
        }
        
        // Mark step 1 complete (entered zone)
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        PlayerData.QuestProgress progress = data.getQuestProgress(1);
        
        if (!progress.hasData("entered_zone")) {
            plugin.getQuestManager().completeStep(player, 1, 1, "entered_zone");
            plugin.debug("[PortalZone] Marked Quest 1 Step 1 complete for " + player.getName());
        }
    }
    
    /**
     * Called when player is teleported (from QuestListener)
     */
    public void onPlayerTeleport(Player player, Location from, Location to) {
        UUID uuid = player.getUniqueId();
        
        // Check if they were in the zone
        if (playersInZone.contains(uuid)) {
            // Debug message
            plugin.debug("[PortalZone] " + player.getName() + 
                " was TELEPORTED from portal zone!");
            plugin.debug("[PortalZone] From: " + formatLocation(from) + 
                " To: " + formatLocation(to));
            
            playersInZone.remove(uuid);
            
            // This will be handled by QuestListener for Quest 1 completion
        }
    }
    
    /**
     * Format location for debug output
     */
    private String formatLocation(Location loc) {
        if (loc == null) return "null";
        return String.format("(%.1f, %.1f, %.1f) in %s", 
            loc.getX(), loc.getY(), loc.getZ(), 
            loc.getWorld() != null ? loc.getWorld().getName() : "null");
    }
    
    /**
     * Clean up player data on logout
     */
    public void cleanupPlayer(UUID uuid) {
        debugModeEnabled.remove(uuid);
        playersInZone.remove(uuid);
        
        BukkitTask task = highlightTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Check if player has debug mode enabled
     */
    public boolean hasDebugEnabled(UUID uuid) {
        return debugModeEnabled.contains(uuid);
    }
    
    /**
     * Check if player is in zone
     */
    public boolean isPlayerInZone(UUID uuid) {
        return playersInZone.contains(uuid);
    }
    
    /**
     * Shutdown - cancel all tasks
     */
    public void shutdown() {
        for (BukkitTask task : highlightTasks.values()) {
            task.cancel();
        }
        highlightTasks.clear();
        debugModeEnabled.clear();
        playersInZone.clear();
    }
    
    // Getters for zone bounds
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public String getZoneWorld() { return zoneWorld; }
}
