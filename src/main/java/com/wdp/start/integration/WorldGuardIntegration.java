package com.wdp.start.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.wdp.start.WDPStartPlugin;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Integration with WorldGuard for region-based portal zone detection
 */
public class WorldGuardIntegration {
    
    private final WDPStartPlugin plugin;
    private boolean enabled = false;
    
    public WorldGuardIntegration(WDPStartPlugin plugin) {
        this.plugin = plugin;
        
        try {
            // Check if WorldGuard is available
            Class.forName("com.sk89q.worldguard.WorldGuard");
            enabled = true;
            plugin.getLogger().info("WorldGuard integration enabled.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("WorldGuard not found! Region-based portal zones will not work.");
            enabled = false;
        }
    }
    
    /**
     * Check if WorldGuard integration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if a location is within a specific WorldGuard region
     */
    public boolean isInRegion(Location location, String regionName) {
        if (!enabled || location == null || location.getWorld() == null) {
            return false;
        }
        
        try {
            World world = location.getWorld();
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            
            if (regions == null) {
                plugin.debug("[WorldGuard] No region manager for world: " + world.getName());
                return false;
            }
            
            ProtectedRegion region = regions.getRegion(regionName);
            if (region == null) {
                plugin.debug("[WorldGuard] Region not found: " + regionName);
                return false;
            }
            
            BlockVector3 position = BlockVector3.at(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
            );
            
            return region.contains(position);
            
        } catch (Exception e) {
            plugin.debug("[WorldGuard] Error checking region: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the bounding box of a WorldGuard region
     * Returns null if region not found
     */
    public RegionBounds getRegionBounds(World world, String regionName) {
        if (!enabled || world == null) {
            return null;
        }
        
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            
            if (regions == null) {
                return null;
            }
            
            ProtectedRegion region = regions.getRegion(regionName);
            if (region == null) {
                return null;
            }
            
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            
            return new RegionBounds(
                min.getX(), min.getY(), min.getZ(),
                max.getX(), max.getY(), max.getZ()
            );
            
        } catch (Exception e) {
            plugin.debug("[WorldGuard] Error getting region bounds: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if a region exists
     */
    public boolean regionExists(World world, String regionName) {
        if (!enabled || world == null) {
            return false;
        }
        
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            
            if (regions == null) {
                return false;
            }
            
            return regions.getRegion(regionName) != null;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Simple class to hold region bounds
     */
    public static class RegionBounds {
        public final int minX, minY, minZ;
        public final int maxX, maxY, maxZ;
        
        public RegionBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
        
        @Override
        public String toString() {
            return String.format("(%d, %d, %d) to (%d, %d, %d)", 
                minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
